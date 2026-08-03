# 爱拼豆

一个以画布为中心的拼豆图纸编辑器，支持图片转图纸、颜色整理、精修绘制、智能拼豆板、高级裁剪和图纸导出。应用采用单文件 HTML/CSS/JavaScript 实现界面与编辑逻辑，并通过 Android WebView 打包为离线 App。

## 当前版本

- 版本：3.4.0（versionCode 40）
- 主页面：`index-pro.html`
- Android 工程：`android-app/`
- 安装包：请从 GitHub Releases 下载

## 本次更新

- 顶部安全区改为读取设备真实状态栏高度，主界面、智能拼豆板和高级裁剪统一自适应。
- 智能拼豆板布局编辑精简为“调整位置”和“调整位置和大小”。
- 智能拼豆板预览与主画布使用一致的滚动、拖动、滚轮缩放和双击适应逻辑，不再被隐藏边界裁切。
- 智能拼豆板和高级裁剪仅让左侧控件避开横屏状态栏，右侧预览保持完整画布空间。
- 智能拼豆板继续采用滑动解锁，锁定后保留分板与颜色相关功能。

## 项目结构

```text
index-pro.html                         主界面与全部编辑逻辑
android-app/app/src/main/java/         Android WebView 容器
android-app/app/src/main/res/          图标与主题资源
tools/                                 页面检查脚本
scripts/sync-github.ps1                后续同步与发布辅助脚本
```

## 后续同步

修改完成后运行：

```powershell
.\scripts\sync-github.ps1 -Message "描述本次修改"
```

如果还要发布安装包，并且电脑已安装并登录 GitHub CLI：

```powershell
.\scripts\sync-github.ps1 -Message "发布 3.4.1" -Version "3.4.1" -ApkPath ".\AiPindou-v3.4.1.apk"
```

签名密钥、构建工具、缓存和 APK 不提交到源码仓库；APK 单独放在 Release 中。
