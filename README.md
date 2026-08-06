# 野水簿 Android Beta

个人钓点与出钓记录管理应用的 Android Beta 源码。

## Beta 已实现

- Android 原生运行时定位权限与 GPS/网络定位，网页地图可手动选点。
- 钓点以真实 WGS84 坐标保存；导航前转换为 GCJ-02，并唤起高德地图。
- 一键从长江水文网页读取实时水位；武汉区域自动参考“汉口”，也可手动选择其他水文站。
- 钓点、鱼获、历史记录、月/年统计与推荐均使用本机实际录入数据，不附带示例记录。
- GitHub Actions 自动构建可安装的 Debug Beta APK；`beta-v*` 标签会发布 GitHub Prerelease。

## 目录

- `angler-atlas-android/`：Android Java/WebView 工程。
- `angler-atlas-demo/`：打包进 APK 的本地 Web UI，同时可在浏览器中预览。

## 构建

本项目使用 Android Gradle Plugin 8.7.3、Gradle 8.9、Java 17、compileSdk 35。

```bash
gradle -p angler-atlas-android :app:assembleDebug
```

APK 生成在 `angler-atlas-android/app/build/outputs/apk/debug/app-debug.apk`。

## 当前边界

- 照片选择在当前会话可预览；正式版需要迁移到 Room/文件存储以持久化原图。
- 冬季照片地形为“当前水位减照片对应水位”的参考估算，不等同声呐测深。
- 在线水位依赖长江水文网页结构与网络可用性，失败时不会填充虚构缓存值。
- 天气、OCR、停车点到钓点的精确步行路线仍待正式 API/服务接入。
