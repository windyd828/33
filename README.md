# 耳机线控屏蔽 — GitHub 自动编译版

把本工程上传到 GitHub，就可以通过 Actions 编译安卓 APK。无需在本地安装 Android Studio、Java 或 SDK，也无需配置 Token、Secrets 或签名文件。

## 按这五步操作

1. 在 GitHub 新建一个仓库，例如 `HeadsetKeyBlocker`。公开或私有均可；需要仓库允许使用 GitHub Actions，构建消耗所属账户的 Actions 用量。
2. **先解压源码包**。进入解压目录，使用仓库的 **Add file → Upload files**，把里面的文件和文件夹上传到仓库根目录，再点 **Commit changes**。不要只上传 ZIP，也不要把整个项目再套进一层目录。
3. 确认仓库根目录能直接看到 `app/`、`ci/`、`build.gradle`、`settings.gradle`，并且存在 **`.github/workflows/build-apk.yml`**。
4. 点击仓库顶部 **Actions → Build Android APK**。推送代码后会自动开始；也可以点击 **Run workflow → Run workflow** 手动运行。手动按钮要求工作流文件已经提交到仓库默认分支（通常是 `main`）。
5. 等本次运行变成绿色成功状态，进入运行详情，在 **Artifacts** 中下载 **`HeadsetKeyBlocker-debug.apk`**，或使用构建摘要中的下载链接。需要登录 GitHub；产物设置为保留 14 天。

使用的是支持直接上传单个文件的 `actions/upload-artifact@v7`，因此提供 APK 文件下载；如浏览器或下载方式给你的是压缩包，解压取出 APK 后安装。

## Actions 页面没有工作流？

常见原因是 `.github` 文件夹没有被上传，或项目多套了一层目录。可以在 GitHub 网页直接补齐：

1. 在仓库选择 **Add file → Create new file**。
2. 文件名填写：`.github/workflows/build-apk.yml`。
3. 打开源码包中的 **`github-build.yml`**（它是工作流的可见备份），把全文复制到 GitHub 编辑框。
4. 提交到默认分支，然后回到 Actions。

根目录的 `github-build.yml` 只是便于复制；GitHub 实际只执行 `.github/workflows/build-apk.yml`。

## 工作流做什么

| 步骤 | 内容 |
| --- | --- |
| Set up Java 17 | 安装 Temurin JDK 17 |
| Set up Gradle 8.9 | 通过 Gradle 官方 action 安装固定版本，并使用 basic 缓存 |
| Install Android SDK packages | 使用 GitHub Ubuntu runner 的 SDK 工具，安装 SDK 35 和 Build Tools 34.0.0 |
| Test key filtering logic | 运行 27 项核心逻辑断言 |
| Build installable debug APK | 使用 Android Gradle Plugin 8.7.3 编译并生成 debug 签名 APK |
| Upload APK | 把 APK 保存为本次运行的可下载产物 |

工作流只需要读取仓库的权限。它不会建立 Release 或向仓库推送提交。无需 Gradle Wrapper，因为工作流显式安装并使用 Gradle 8.9。

## 手机上使用

1. 安装 APK，打开“耳机线控屏蔽”。
2. 按页面提示启用无障碍服务。
3. 拔掉耳机，点击“保护手机音量键”，各按一次手机机身音量＋和音量－。
4. 插上转接头和有线耳机，逐个学习要屏蔽的中键、音量键或其他控制键。
5. 打开屏蔽开关，测试播放音乐和锁屏后的效果。

完整说明和免 Root 方案的兼容性边界见 [APP_USAGE.md](APP_USAGE.md)。同一输入事件无法区分手机与耳机时，应用会拒绝添加屏蔽规则。

## 构建或安装遇到问题

| 现象 | 处理方法 |
| --- | --- |
| 没有 Run workflow 按钮 | 先把工作流提交到默认分支，并确认仓库 Actions 已启用。 |
| 无法找到 ci/prepare-sdk.py 或 build.gradle | 源码目录层级错了；这些文件需要处于工作流所用的仓库根目录。 |
| Actions 显示红色 | 点进失败任务，展开红色步骤，复制具体错误日志。失败状态不会上传 APK。 |
| 下载按钮不可点或文件过期 | 登录 GitHub；过期后重新运行工作流。 |
| 新 APK 无法覆盖旧版 | 默认使用临时 debug 签名，不同运行的签名可能不同。卸载旧版后重新安装，会清除已学习规则。长期使用可另行配置固定签名。 |
| 安装后仍有线控动作 | 编译成功不代表所有手机/转接头都支持拦截；查看 APP_USAGE.md 的事件日志排查说明。 |

**验证状态：** 已检查工作流 YAML、脚本语法、文件路径和源码包结构，并在本地通过 27 项核心测试。尚未在你的 GitHub 仓库实际触发完整构建，也没有真机测试；是否生成 APK 以 Actions 运行结果为准。

## 官方参考

- [GitHub 手动运行工作流](https://docs.github.com/en/actions/how-tos/manage-workflow-runs/manually-run-a-workflow)
- [Gradle setup-gradle](https://github.com/gradle/actions/blob/main/docs/setup-gradle.md)
- [GitHub 上传构建产物](https://github.com/actions/upload-artifact)
- [Ubuntu 24.04 runner 环境](https://github.com/actions/runner-images/blob/main/images/ubuntu/Ubuntu2404-Readme.md)
