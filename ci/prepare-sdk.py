"""Install the exact Android packages required by this project on a hosted runner."""
import os
from pathlib import Path
import shutil
import subprocess

sdk_root = os.environ.get('ANDROID_HOME') or os.environ.get('ANDROID_SDK_ROOT')
if not sdk_root:
    raise SystemExit('Android SDK was not found. Use the GitHub-hosted ubuntu-24.04 runner.')

manager = shutil.which('sdkmanager')
if manager is None:
    candidates = [Path(sdk_root) / 'cmdline-tools/latest/bin/sdkmanager']
    candidates.extend(sorted((Path(sdk_root) / 'cmdline-tools').glob('*/bin/sdkmanager')))
    manager = next((str(p) for p in candidates if p.is_file()), None)
if manager is None:
    raise SystemExit('sdkmanager is missing from this runner. Check the Install Android SDK packages step.')

subprocess.run(
    [manager, f'--sdk_root={sdk_root}', '--licenses'],
    input='y\n' * 100, text=True, check=True
)
subprocess.run(
    [manager, f'--sdk_root={sdk_root}', 'platform-tools', 'platforms;android-35', 'build-tools;34.0.0'],
    check=True
)
