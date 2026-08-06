# 野水簿 Android Beta 0.4.0-beta.4

最低 Android 8.0（API 26），包名 `com.rivertrace.angleratlas`。

## 已接通

- Android 原生定位权限、GPS/网络定位回调。
- Leaflet + OpenStreetMap 在线底图与地图真实坐标选点。
- 钓点、记录、统计和推荐的本机持久化；首次安装无示例数据。
- WGS84 转 GCJ-02 后唤起高德导航，并可搜索钓点附近停车场。
- 一键访问长江水文网页、解析实时站点水位；武汉定位自动参考汉口站，其他站可手选。
- 多鱼种逐条录入、提交预览、历史 CSV 导入和月/年统计。

## 构建

仓库根目录的 GitHub Actions 会使用 Java 17、Gradle 8.9 和 Android SDK 35 构建可安装 Debug APK。也可在配置好同版本工具的本机执行：

```bash
gradle -p angler-atlas-android :app:assembleDebug
```

## 数据与安全边界

`CjhHydrologyClient.java` 读取 `http://www.cjh.com.cn/sqindex.html` 中的实时水情数组。网页并非稳定 JSON API，失败时 Beta 会明确显示获取失败，不填充虚构水位。

当前“地形”仅按用户上传的枯水期照片对应水位与当前水位计算水位差，是参考估算，不是测深数据，不可用于涉水、航行或防汛决策。正式版应使用 Room/文件存储持久化照片，并接入天气、OCR、步行路径和可靠的水文后端适配器。
