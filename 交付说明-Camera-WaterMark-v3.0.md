# Camera-WaterMark v3.0 交付说明

> 离线照片水印工具：读取 EXIF 参数，添加文字 / 图片 / 模糊卡片水印，Windows + Android 双端。
> 作者：**shiraijikuu**　|　仓库：https://github.com/shiraijikuu/camera-watermark　|　协议：MIT

---

## 一、版本与关键标识

| 项 | 值 |
|---|---|
| 应用版本 | v3.0（Android versionName `3.0` / versionCode `3`；package.json `3.0.0`） |
| 应用名称 | Camera-WaterMark |
| Android 包名 (applicationId) | `com.shiraijikuu.cwm`（**发布后不可改**） |
| Android 签名 | 正式 release，密钥 `cwm-release.jks`，alias `cwm`，RSA2048，有效期至 2054 |
| Windows appId | `com.shiraijiku.cwm`（electron-builder 标识，与安卓互不影响） |
| 技术内核 | 单文件 `index.html`（HTML5 Canvas + 原生 JS，无框架），三端同源 |
| 桌面壳 | Electron 31（x64） |
| 安卓壳 | Capacitor 8（WebView，minSdk/targetSdk 见工程） |

---

## 二、交付物清单（均在 `E:\codex\`）

### 1. 可直接分发的成品
| 文件 | 大小 | 用途 | 给谁 |
|---|---|---|---|
| `Camera-WaterMark-3.0-release.apk` | 12.27MB | **Android 正式签名版**，可安装/上架 | 安卓用户（**发这个**） |
| `Camera-WaterMark-3.0-debug.apk` | ≈13.2MB | Android 调试版，仅用于 Android Studio / logcat 排错 | 自己调试，可不分发 |
| `Camera-WaterMark-3.0.0-Setup.exe` | 83.43MB | Windows 安装版（可选目录、桌面快捷方式） | Windows 用户 |
| `Camera-WaterMark-3.0.0-portable.exe` | 83.13MB | Windows 便携版（免安装、双击即用） | Windows 用户 |

> exe 体积 80+MB 是内置 Chromium 运行时所致，属正常；程序本体仅约 9MB。

### 2. 源码工程（转交 Codex / 上传 GitHub）
| 目录 | 内容 |
|---|---|
| `camera-watermark-android\` | Android 完整工程（含 `android\` 原生壳、`www\index.html` 内核、`package.json`、构建文档） |
| `camera-watermark-windows\` | Windows 工程（`index.html` + `main.js` + `package.json` + `presets\` 预设） |
| `HANDOFF_FOR_CODEX.md` | **详细开发交接（21 节）**：每次改动的根因、方案、验证、命令，Codex 应先读它 |

### 3. 签名密钥（最高优先级保管）
- 目录 `CWM签名密钥备份\`：`cwm-release.jks`（私钥）+ `keystore.properties(含密码)`
- 密码：`Cwm2026release`，alias：`cwm`
- **务必多处离线备份，不要提交到公开仓库（已在 .gitignore 忽略）**：
  - 私钥丢失 → 以后无法用同一签名给 App 发更新（只能换包名，等于新应用）；
  - 私钥泄露 → 他人可冒充你的应用发更新。

---

## 三、架构（一句话）

同一份 `index.html` 内核，分别套壳：

```
                    ┌─ Electron 31 ─→ Windows exe（nsis 安装版 / portable 便携版）
index.html 单文件 ──┤
（Canvas 渲染/EXIF/  └─ Capacitor 8 ─→ Android APK（WebView 运行）
 水印/插件功能内建）
```

- 手机版**不装插件**，原 Python 时代各插件（图片水印、模糊卡片、主题等）功能已全部并入本体；
- 内核改动只需改 `index.html`，再同步到各工程的对应位置（见第五节）。

---

## 四、最终用户使用

**Android**：把 `Camera-WaterMark-3.0-release.apk` 传到手机安装（首次需允许"安装未知来源应用"）。导出时图片直存系统相册 `Pictures/Camera-WaterMark`。

**Windows**：
- 安装版双击 `Setup.exe` 按向导安装；
- 便携版双击 `portable.exe` 直接运行。
- 首次可能弹 SmartScreen「未知发布者」（未购买代码签名证书）→「更多信息 → 仍要运行」。

---

## 五、开发者：如何重新构建

### 环境（本机已就绪）
- JDK21：`E:\jdk21\jdk-21.0.12.1+1`（**必须用它**，JDK17/JBR 会被 Capacitor/Gradle 卡）
- Android SDK：`E:\sdk`（platform android-35、build-tools 36.0.0）
- Node.js + 工程 `node_modules`（首次需 `npm install`）

### 0. 改完内核后的同步（关键，四处必须一致）
改 `index.html` 后，复制到：
1. `www\index.html`
2. `android\app\src\main\assets\public\index.html`（或直接 `npx cap sync android` 自动同步）
3. Windows 工程的 `index.html`
4. Android 交付工程的两处 `index.html`
- 用 SHA256 前 16 位核对四份完全一致。

### 1. Android
PowerShell，在工程的 `android\` 目录：
```powershell
$env:JAVA_HOME="E:\jdk21\jdk-21.0.12.1+1"
$env:ANDROID_HOME="E:\sdk"; $env:ANDROID_SDK_ROOT="E:\sdk"
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
.\gradlew.bat assembleRelease          # 正式包 → app/build/outputs/apk/release/app-release.apk
# 调试包：assembleDebug；改包名/namespace 后必须先 .\gradlew.bat clean
```
- 有 `keystore.properties` + `cwm-release.jks` 时自动用正式签名；缺失则回退 debug 签名（他人 clone 也能构建）。
- 验证包名/签名：
  `E:\sdk\build-tools\36.0.0\aapt.exe dump badging app-release.apk`（看 package name）
  `apksigner.bat verify --verbose app-release.apk`（看 v1/v2 是否 true）

### 2. Windows
在工程根：
```powershell
npm run build:win          # = electron-builder --win --x64，同时出 nsis + portable，输出到 release\
npm run build:win-portable # 仅便携版
```

---

## 六、发布与"检查更新"机制（重要）

- App 内「检查更新」逻辑：读取 `https://cdn.jsdelivr.net/gh/shiraijikuu/camera-watermark@main/update.json` 比对版本，并跳转 GitHub Releases 下载页；
- **这是"检测新版本 + 引导下载"，不是应用内静默热更新（OTA）**：用户仍需下载新 APK/新 exe 手动覆盖安装；
- 发布流程：
  1. GitHub 仓库打 tag（如 `v3.0`）、建 Release、上传 APK/exe；
  2. 更新 `update.json`（版本号 + download_url）推到 main；
  3. jsDelivr 的 `@main` 边缘缓存约 12 小时，不及时可访问 `https://purge.jsdelivr.net/gh/shiraijikuu/camera-watermark@main/update.json` 强制刷新，或让 update_url 指向固定 tag；
- Android 升级必须用**同一签名密钥**，否则无法覆盖安装。

---

## 七、v3.0 功能一览

- 文字水印：模板变量（品牌/型号/焦距/光圈/快门/ISO/日期等）、自定义字体（ttf/otf/ttc）、字号/行距/颜色/描边/透明度、多行与空格对齐；
- 图片水印：1–5 个独立槽位、内置品牌 logo 预设（含相机/手机/DJI/影石等素材）、支持上传 PNG/GIF（取首帧）、百分比 offset、图片缓存；
- 模糊卡片：同图"模糊背景 + 清晰主体"，上/下/左/右/左右分离五种布局，圆角/阴影/描边/压暗、多种画面比例导出；
- 参数标 badge：mm/F/S/ISO 标签，**带框时等大对齐、不带框随文字**，整组支持左/中/右对齐（横排与竖排均已支持）；
- 品牌标预设：品牌 logo 图 + 参数文字组合，五种布局通用；
- 主题：8 套配色 + 跟随系统，菜单/面板深色化、轻动效（可关）；
- 三栏式 UI（左列表 / 中预览 / 右参数），滚轮 + 滑块缩放、拖空白平移、拖拽移动水印；
- 中英双语；导出 JPG/PNG/WEBP、可选比例、全分辨率；Android 直存相册并带成功/失败提示；
- 「关于」内含作者 shiraijikuu、可点击 GitHub 链接（系统浏览器打开）、MIT 协议。

---

## 八、测试结论与待确认项

**已验证（自动化/无头）**
- 内核四份哈希一致；Android release/debug 均 `BUILD SUCCESSFUL`，release 签名 v1+v2 通过；
- 包名 `com.shiraijikuu.cwm`、启动 Activity、dex 新包类无旧残留；
- Windows 安装包/便携包构建成功，解包内容正确，打包后 exe 启动不崩溃；
- 参数框等大/三档对齐、模糊卡片、导出 Toast 等经 Electron 无头断言，零控制台错误。

**需你在真机/实机确认**
1. Android 实体机点导出：是否弹"正在保存…→✓ 已保存到系统相册"，并在图库看到 `Camera-WaterMark` 相册（无头环境无法写 MediaStore）；
2. Android 9 及以下首次导出的存储权限（目标机为 Android 13 不受影响）；
3. 真机手势：双指缩放、水印拖拽、顶部缩放条（手机版已移除缩放条，用双指）；
4. Windows 安装/便携版在干净系统的首次启动与 SmartScreen 流程。

**已知非问题**
- 未做 Windows 代码签名 → SmartScreen 未知发布者提示，属正常；
- jsDelivr `@main` 更新缓存最长约 12 小时；
- debug 与 release 签名不同，不能互相覆盖，测试切换需先卸载。

---

## 九、给 Codex 的入口
1. 先读 `camera-watermark-android\HANDOFF_FOR_CODEX.md`（第 1–21 节，含每次改动根因与命令）与 `BUILD_ANDROID.md`；
2. 唯一内核是 `index.html`，任何 UI/渲染/水印逻辑只改它，再按第五节同步；
3. 原生安卓代码仅两个类：`android\app\src\main\java\com\shiraijikuu\cwm\` 下 `MainActivity.java`、`GallerySaverPlugin.java`（相册直存）；
4. 不要把 `*.jks`、`keystore.properties`、`local.properties`、`build/` 提交到公开仓库（已配 .gitignore）。

---

*交付生成日期：2026-09-01　|　对应内核哈希前缀见工程内 index.html 实际 SHA256。*
