# 耳机线控屏蔽（免 Root 源码版）

这是 Android 原生 Java 工程，旨在屏蔽带转接头的有线耳机上报给 Android 的指定按键，并保留正常音频播放。要求 Android 8.0（API 26）或以上；兼容性需要在你的手机和转接头上确认。

**本包是源码，不包含 APK。本次未完成 Android 工程编译或真机测试。**

## 做了什么

- 通过用户手动启用的无障碍服务过滤 KeyEvent，不要求 Root、ADB 或 Shizuku。
- 学习并保存“输入设备描述符＋Android 按键码＋扫描码”，不会仅凭音量键码把所有设备一起屏蔽。
- 支持系统上报的耳机中键、播放/暂停、上/下一曲、音量、静音、助手类按键；每个事件要分别学习。
- 先记录手机机身音量＋/－作为始终放行的事件。耳机音量事件与之完全相同时，拒绝添加该规则。
- 屏蔽仅在检测到有线/USB 音频输出时生效；蓝牙音频不作为启用条件。
- 同一次按键的 DOWN、长按重复和 UP 保持一致，即使中途关闭开关或拔出耳机。
- 不占用媒体播放会话，不循环强改音量，不录音，不联网，不读取屏幕，不记录普通打字内容。
- 最近 12 条控制按键记录仅存在于当前进程内存。学习规则保存在应用私有设置中，禁用系统备份。

## 在 Windows 上生成 APK

本工程固定使用 AGP 8.7.3、Gradle 8.9、compile/target SDK 35，建议 JDK 17 或 21。不是商店上架配置承诺。

1. 安装 Android Studio，并在 SDK Manager 安装 **Android SDK Platform 35** 和 **Android SDK Build-Tools 34.0.0**。首次安装需要你在官方界面接受 SDK 许可。
2. 解压本包。可以用 Android Studio 自带的 JDK（版本须与 Gradle 8.9 兼容），或设置 `JAVA_HOME` 指向 JDK 17/21。
3. 在项目根目录打开 PowerShell：

```powershell
powershell -ExecutionPolicy Bypass -File .\build-apk.ps1
```

这里的 ExecutionPolicy 仅对该 PowerShell 进程生效。脚本只下载官方 Gradle 8.9、验证其官方 SHA256，然后调用构建；不会修改手机或永久修改系统执行策略。

如 Android SDK 位于自定义目录：

```powershell
powershell -ExecutionPolicy Bypass -File .\build-apk.ps1 -SdkPath "D:\Android\Sdk"
```

首次构建需要下载 Gradle 和 Google/Maven 的构建依赖。生成文件位于：

```text
app/build/outputs/apk/debug/app-debug.apk
```

它是用于自用测试的 debug 签名 APK。复制到手机安装即可。持续升级时应保留同一台构建电脑的签名密钥；签名不同会影响覆盖安装。

如果已经安装 Gradle 8.9，可直接在工程根目录执行：

```text
gradle :app:assembleDebug
```

Linux/macOS 同样可使用上述 Gradle 命令，并配置 `ANDROID_HOME` 和 JDK。工程没有附带 Gradle Wrapper 二进制；Android Studio 如要求选择 Gradle 分发，请先运行构建脚本，再选择工程下 `.tools/gradle-8.9` 的本地安装，或选择自己安装的 Gradle 8.9。

## 手机上怎么用

1. 打开应用，点击“开启无障碍服务”，在系统设置里启用“耳机线控屏蔽”。不会自动打开权限。
2. **先拔掉耳机**，回到应用点击“保护手机音量键”，在 20 秒内各按一次手机机身的音量＋和音量－。这一步会正常改变手机音量。
3. 插好转接头和有线耳机。应用应显示检测到有线/USB 音频。
4. 点击“学习一个耳机按键”，按一次耳机中键；成功后再次点击学习，分别记录耳机＋、－或其他控制键。学习期间请不要按手机机身按键。
5. 开启“开启屏蔽”开关，播放音乐验证。切换到音乐应用后，无障碍服务仍由系统管理，不依赖本应用页面保持打开。
6. 如果长按会产生额外的助手按键码，需要再次学习该按键。没有上报给服务的助手或通话指令无法用此程序可靠屏蔽。

进入后台会取消尚未完成的学习，已保存规则仍有效。学习成功的当次按键会直接被拦截。正常使用时开关默认关闭，需你明确打开。

Android 13 及以上部分侧载安装会限制启用无障碍：如系统明确提示受限设置，可在“设置 → 应用 → 本应用 → 更多”中查找“允许受限设置”，再返回无障碍。入口随系统不同；如果系统不提供该入口，不要假设此源码能解除限制。

## 必须理解的边界

无 Root 应用不能保证屏蔽所有手机、转接头、通话或锁屏场景。

| 现象 | 解释与下一步 |
| --- | --- |
| 日志出现“拦截”，音乐没有被暂停 | 该事件链路有效，继续验证其他按键与锁屏。 |
| 日志出现“放行” | 开关关闭、规则未学到、事件标识不一致、处于保护模式，或未检测到有线音频。 |
| 线控有反应但完全没有按键日志 | 可能被厂商系统、其他无障碍服务或转接头处理；服务也可能被系统停止。核查本应用服务状态及按键过滤服务冲突。 |
| 音量变化但系统没有对应事件 | 转接头可能在自身电路/固件里调节，Android 按键过滤无法阻止。 |
| 提示与手机音量键相同 | 系统暴露的设备、按键码、扫描码完全一样，应用无法可靠区分来源；该规则不会添加。 |
| 未检测到有线音频 | 部分厂商设备类型上报不同。为了避免误屏蔽，本版不猜测；需要按具体机型调整。 |
| 锁屏、来电或助手仍有动作 | 部分控制链路可能提前被系统处理；本程序不能承诺挂断/接听和唤醒助手都失效。 |
| 重新插拔后失效 | 常规设备 ID 变化不影响本程序，但如果设备描述符或扫描码也改变，需要重新学习。 |

若仍需更底层的全局过滤，需要结合**手机型号、Android/系统版本、Root 状态、转接头类型和真实输入事件**另做适配；修改系统通用键位映射可能同时破坏手机按键，本项目没有这样做。

## 恢复

关闭本应用的屏蔽开关即可恢复新按键。若按键正被按住，请先松开再测试；当前这次按键会按原来的拦截决策完成。也可在系统无障碍设置里关闭本服务，或卸载应用。不要同时启用多个映射/过滤相同按键的服务。

## 验证

本次可在 Java 17 环境运行纯逻辑测试：

```text
java -m jdk.compiler/com.sun.tools.javac.Main -encoding UTF-8 -d build/core-tests app/src/main/java/com/example/headsetblocker/KeyRules.java tests/KeyRulesTest.java
java -cp build/core-tests com.example.headsetblocker.KeyRulesTest
```

验证覆盖：保护优先、未知来源放行、手机与耳机分离、拔出后新事件放行、长按重复、切换开关时 DOWN/UP 一致、同时按不同设备、丢失 UP 后的新按键。

**这些测试不等于 APK 编译或真机兼容性验证。** 安装后还需要测试：音乐播放、中键连按、＋/－长按、手机音量键、锁屏、耳机插拔、接听/挂断、重启后服务状态和停用恢复。通话测试请使用普通测试呼叫，不要拨打紧急号码。

## 主要文件

- `BlockerService.java`：实际接收和过滤按键，识别有线音频输出。
- `KeyRules.java`：匹配规则与整次按键的一致性处理。
- `RuleStore.java`：保存规则与手机按键保护。
- `MainActivity.java`：中文界面、学习操作、日志和恢复。
- `tests/KeyRulesTest.java`：独立 Java 核心测试。

## 官方依据

- [AccessibilityService.onKeyEvent](https://developer.android.com/reference/android/accessibilityservice/AccessibilityService#onKeyEvent(android.view.KeyEvent))：过滤按键及保持 DOWN/UP 事件流一致。
- [FLAG_REQUEST_FILTER_KEY_EVENTS](https://developer.android.com/reference/android/accessibilityservice/AccessibilityServiceInfo#FLAG_REQUEST_FILTER_KEY_EVENTS)：过滤事件所需服务声明。
- [InputDevice.getDescriptor](https://developer.android.com/reference/android/view/InputDevice#getDescriptor())：使用稳定描述符而不是可能变化的运行时 ID。
- [AGP 8.7 兼容表](https://developer.android.com/build/releases/agp-8-7-0-release-notes)：Gradle 8.9、JDK 17、最高 SDK 35。
- [Android 受限设置说明](https://support.google.com/android/answer/12623953)：手动授权说明。

该工程不含系统键位修改、Root 命令或自动授予无障碍权限的逻辑。
