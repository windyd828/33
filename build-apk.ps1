param([string]$SdkPath = "")
$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

if (-not $SdkPath) { $SdkPath = $env:ANDROID_SDK_ROOT }
if (-not $SdkPath) { $SdkPath = $env:ANDROID_HOME }
if (-not $SdkPath) { $SdkPath = Join-Path $env:LOCALAPPDATA "Android\Sdk" }
if (-not (Test-Path (Join-Path $SdkPath "platforms\android-35\android.jar"))) {
    throw "Install Android SDK Platform 35 and Build-Tools 34.0.0 in Android Studio SDK Manager first, then run with -SdkPath if needed."
}
$env:ANDROID_HOME = $SdkPath
if (-not $env:JAVA_HOME) {
    $studioJdk = Join-Path $env:ProgramFiles "Android\Android Studio\jbr"
    if (Test-Path (Join-Path $studioJdk "bin\java.exe")) { $env:JAVA_HOME = $studioJdk }
}
if ($env:JAVA_HOME) { $env:Path = (Join-Path $env:JAVA_HOME "bin") + ";" + $env:Path }
if (-not (Get-Command java -ErrorAction SilentlyContinue)) { throw "Install JDK 17 or JDK 21 first." }

$toolsDir = Join-Path $PSScriptRoot ".tools"
$gradleBat = Join-Path $toolsDir "gradle-8.9\bin\gradle.bat"
if (-not (Test-Path $gradleBat)) {
    New-Item -ItemType Directory -Force $toolsDir | Out-Null
    $zip = Join-Path $toolsDir "gradle-8.9-bin.zip"
    $url = "https://services.gradle.org/distributions/gradle-8.9-bin.zip"
    Write-Host "Downloading Gradle 8.9 from its official distribution service..."
    Invoke-WebRequest -Uri $url -OutFile $zip -UseBasicParsing
    $expected = ([string](Invoke-WebRequest -Uri ($url + ".sha256") -UseBasicParsing).Content).Trim()
    if ($expected -notmatch '^[a-fA-F0-9]{64}$') { throw "Invalid Gradle checksum response." }
    $actual = (Get-FileHash $zip -Algorithm SHA256).Hash
    if ($actual -ne $expected) { Remove-Item $zip; throw "Gradle SHA256 verification failed." }
    Expand-Archive -Path $zip -DestinationPath $toolsDir -Force
    Remove-Item $zip
}
& $gradleBat --no-daemon :app:assembleDebug
if ($LASTEXITCODE -ne 0) { throw "Android build failed. Read the error above." }
Write-Host "APK ready: $PSScriptRoot\app\build\outputs\apk\debug\app-debug.apk"
