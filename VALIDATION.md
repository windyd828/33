# 验证记录

日期：2026-10-11。

- 已使用 OpenJDK 17 编译 KeyRules.java 与 KeyRulesTest.java，运行通过 27 个核心断言。
- 已使用 Java 编译器的解析阶段检查 6 个 Java 源文件，未发现语法错误；未进行 Android API 类型检查。
- 已解析 3 个 Android XML 文件，均为有效 XML。
- 已核对 Manifest 中的 Activity 和 Service 均有对应源码。
- 本应用未声明网络、录音、存储或电话权限；无障碍服务由系统的 BIND_ACCESSIBILITY_SERVICE 绑定权限保护，需要用户手动启用。
- 当前环境缺少 Android SDK、Gradle 和真机，未生成 APK，未验证完整 Android 构建、系统服务接入、厂商兼容性及 Windows 构建脚本实际运行。

代码核查时修正了重复进行手机音量键保护时的状态：每次开始保护都要求重新按完两个机身音量键。

最终应以你的手机、转接头和耳机上的测试结果为准。

## GitHub 版追加检查

- 已解析 Actions YAML，核对 push 和 workflow_dispatch 触发器、读取权限、固定 Gradle 版本和 APK 产物路径。
- 已检查所有工作流内 Bash 片段以及 SDK 脚本、内嵌 Python 语法。
- 已实际运行 ci/test-core.sh，27 项核心断言通过。
- 已核对工作流可见备份与 .github/workflows/build-apk.yml 内容完全一致。
- 源码包直接以仓库根目录组织，包含工作流、ci 脚本和全部安卓源码，不包含临时构建输出。
- 没有连接或操作用户的 GitHub 仓库，尚未完成远程构建或 APK 真机验证。
