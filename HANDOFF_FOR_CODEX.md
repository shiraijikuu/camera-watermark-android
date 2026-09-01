# Camera-WaterMark 交接说明（给 Codex）

> 交接时间：2026-08-31 ｜ 交接人：Doubao ｜ 目标：接手并继续发布 / 迭代本项目
> 原始 Python/Tkinter 项目在 `E:\codex\camera-watermark`（**只读参考，不要再改它**）。本项目是用 HTML/Canvas + Electron / Capacitor 重写的新版，工作工程在 `C:\Users\白井时空\Downloads\cwm-prototype`，成品已输出到 `E:\codex\camera-watermark-windows` 和 `E:\codex\camera-watermark-android`。

---

## 1. 这是什么

一款**离线照片水印工具**：读取照片 EXIF，叠加「文字水印 / 图片 logo 水印 / 模糊卡片水印」，支持批量、全分辨率导出，中英双语、多主题、缩放拖拽、手机自适应。

- **内核**：单个 `index.html`（HTML + CSS + Canvas 2D，原生 JS，无前端框架；EXIF 解析库 exifr 已**离线内嵌**在 `<head>`，不联网）。
- **Windows 壳**：Electron 31（`main.js`），打成单文件 portable exe。
- **Android 壳**：Capacitor（`android/` 工程），靠 GitHub Actions 在云端出 APK（本机无 JDK/SDK，不能本地出包）。
- 手机版**不做插件机制**，原 Python 版各插件功能已直接并入本体内核。

---

## 2. 命名与版本口径（重要，别再写错）

- 应用对外名称统一为 **Camera-WaterMark**（不是 CWM、不是"完整版/Web/Canvas/技术验证"，成品里**禁止出现任何技术栈或测试字样**）。
- `appId`：`com.shiraijikuu.cwm`（保持不变即可）。
- 当前版本：**3.0**。各处口径：

| 位置 | 值 | 说明 |
|---|---|---|
| `index.html` → `APP_VERSION` | `'3.0'` | 界面显示 + 检查更新比对用，允许两段 |
| 底部「检查更新」按钮初始文案 | `v3.0 · 检查更新` | 会被 APP_VERSION 覆盖，保持一致 |
| `package.json` version | `3.0.0` | npm/semver 必须三段，影响 exe 文件名 |
| `package.json` name / productName | `camera-watermark` / `Camera-WaterMark` | productName 决定 exe 名与任务栏名 |
| `capacitor.config.json` appName | `Camera-WaterMark` | 安卓桌面名 |
| `android/.../values/strings.xml` app_name、title_activity_main | `Camera-WaterMark` | 同上 |
| `android/app/build.gradle` | versionCode `3` / versionName `"3.0"` | 每次发版 versionCode +1 |

> 版本比对函数 `cmp()` 用 `(part||0)` 补零、固定比 3 段，所以 `'3.0'` 等价 `[3,0,0]`，两段/三段混用安全。**升版本时上表 7 处要一起改。**

---

## 3. 成品在哪（最终交付）

### `E:\codex\camera-watermark-windows\`
- `Camera-WaterMark-3.0.0-portable.exe`（80.35 MB，单文件绿色版，双击即用，已内置 cwm 图标、无默认英文菜单）
- `index.html` / `main.js` / `package.json` / `update.example.json` / `cwm.ico`（对应源码副本）
- `presets\`（36 个内置水印预设：26 png + 10 gif，**gif 只取首帧**显示，与旧版一致）

### `E:\codex\camera-watermark-android\`
- `android\`：完整 Capacitor/Gradle 工程（minSdk23、compile/targetSdk35、Gradle 8.11.1、AGP 8.7.2、需 JDK17）
- `www\`：网页内核（index.html + 36 presets，已和源码同步，共 37 文件）
- `.github\workflows\android-apk.yml`：推到 GitHub 后云端自动打 debug APK
- `capacitor.config.json` / `package.json` / `cwm.ico` / `BUILD_ANDROID.md`

> 两个目录都已用最新版**整体覆盖**，旧的 `CWM-1.1.0-portable.exe` 已删除。

---

## 4. 本次已完成清单（已逐项验证）

1. **导入照片后文字水印不显示** → 根因：原代码运行时从 jsDelivr 动态加载 exifr，Electron 离线失败 → meta 空 → 文本为空不绘制。已把 exifr full UMD（约 75KB）**内嵌进 `<head>`**（标记 `/* INLINE_EXIFR */`），`loadExifr()` 直接 `Promise.resolve()`，彻底离线。冒烟实测：示例图左下角正常显示 `SONY ILCE-7CM2 / 35mm F1.8 1/250s ISO 400`。
2. **检查更新** → 新增 `APP_VERSION`、`UPDATE_URL`、`checkUpdate(silent)`（三段语义版本比对 + `showModal` 通用弹窗），底部状态栏按钮。当前是"检测到新版→弹窗→跳转下载页"；**内核自动覆盖式热更新尚未实现**（见第 7 节）。
3. **汉化** → i18n 中英两套（`I18N` 对象），清理了状态栏/关于弹窗的残留英文与开发向文案。
4. **原版图标** → 从 `E:\codex\camera-watermark\assets\cwm.ico` 取图：`main.js` BrowserWindow 设 `icon`，`package.json` 的 `build.files` **必须包含 cwm.ico**（否则打进 asar 后路径丢失会回退 Electron 默认图标），exe 元数据 ProductName=Camera-WaterMark、FileVersion=3.0.0 已验证。
5. **放大后无法拖动** → 根因：pointer 事件绑在 `#preview`，但 CSS `transform:scale()` 不改 hit-test 区域，放大后点到的是父容器空白。已把 pointerdown/move/up/cancel/dblclick 改绑到 `.canvas-stage`，用 `getBoundingClientRect()`（视觉 rect）算归一化坐标并做 0..1 边界判断，`setPointerCapture` 也改到 stage；触屏天然兼容。
6. **手机版** → 删掉"返回桌面"按钮；手机用底部 tabbar、隐藏桌面状态栏；功能全部并入本体。
7. **改名 + 版本 3.0** → 第 2 节 7 处全部统一；"关于"弹窗重写为正常用户向简介，删掉"Tauri/Electron 套壳、浏览器画布、矢量字样、平台能力诚实披露"等开发向内容。
8. 其它既定偏好：文字底色块默认关、不用矢量图替代真实预设图、gif 只取首帧、图片水印单独成栏并保留真实预览。

冒烟截图确认：标题栏=Camera-WaterMark、自定义图标、无 File/Edit/View 菜单、三栏布局、文字水印与列表参数正常。

### 4.9 发布前「真实照片」实测修复（第二轮，已双验证）

第一轮冒烟只看了内置示例（示例用假 meta 必然出字），漏掉真实导入，属无效验证。本轮严格按「先复现变红 → 定位根因 → 修 → 在原始真实场景回归变绿」重做，5 项修复**全部只改 `index.html`**：

1. **真实照片无文字水印（真根因）**：`exifr.parse(file,{translate:true,...})` 在当前内嵌 exifr 上必抛 `Cannot set property translate of #<…> which has only a getter`，被 catch 吞成 meta={}，于是模板渲染成空白、画布无字。**去掉 `translate:true` 即好**（exifr 默认就会 translate）。实测红米照解析出 Xiaomi / 23049RAD8C / F1.8 / 1/100s / ISO2000，左下角水印正常。
2. **放大后拖不动（真根因，与第一轮"事件绑错元素"的判断不同）**：pointerdown 里写的是 `const[tx,ty,tw,th]=ph._textRect`（**数组**解构），但 `drawTextWatermark` 返回的是**对象** `{x,y,w,h}`，于是抛 `object is not iterable`；该异常发生在给 `drag` 赋值之前，drag 恒为 null，pointermove 里 `if(!drag)return` 直接退出，任何情况都拖不动。改为对象解构 `const{x:tx,y:ty,w:tw,h:th}=ph._textRect`。
3. **手机触摸拖不动（两处）**：① `setPointerCapture` 在没有 active pointer 时抛错会中断整个 pointerdown，已包 `try/catch`；② 给 `.canvas-stage` 与 `#preview` 补 `touch-action:none`，否则触摸被浏览器当成页面滚动/缩放手势吃掉。桌面 mouse + 手机 touch 两种合成指针序列均验证 panX/panY 确实变化。
4. **删除「载入示例」按钮与全部示例图**：移除 `btnSample`、`loadSamples()`、`makeSamplePhoto()`、`sampleMeta()`、i18n 的 `sample` 键、以及初始化里的自动加载；启动后照片列表为空，等待用户导入（全仓已无这些符号的残留引用）。
5. **顶栏 logo**：改为「C 环包围小写 wm」——C 弧开口朝右、`wm` 居中在环心（text x=11），替代原来偏大、偏右的大写 WM。

验证方式（可复现）：用项目自带 Electron 写无头 harness，导入真实照片 `E:\2\IMG_20260829_191118.jpg`，桌面 + `body.phone` 双形态断言全绿、零未捕获错误；再用 `webContents.capturePage()` 出可视截图，确认水印真画到画布、模板预设下拉非空（6 项，之前"看着空"是旧版）；最后解包 `app.asar` 内 `index.html` 与源码**字节一致**（166561 B、哈希相同），证明修复确实进了安装包。

### 4.10 应用图标统一（Windows + Android，用户指定用原版 `assets/cwm.ico`，禁止自绘）

- **原版图标来源**：`E:\codex\camera-watermark\assets\cwm.ico`（7 尺寸 16~256，256 层是 PNG 压缩）。工程根 `cwm.ico` 必须与它一致。
- **Windows**：`build.win.icon=cwm.ico`（rcedit 写 exe/任务栏/标题栏）；应用内顶栏不再用内联 SVG，改 `<img src="cwm-logo.png">`。`cwm-logo.png` 是从 ico **无损抽取的 256 PNG 原始字节**（见下，**不要用 GDI `Icon.ToBitmap()` 渲染 PNG 压缩 ico，会花屏成噪点**）；`package.json build.files` 同时含 `cwm.ico`、`cwm-logo.png`。窗口刚创建第一帧可能闪默认图标，属正常，多等 1~2 秒即加载为 cwm。
- **Android 自适应图标**（`android/app/src/main/res/`）：
  - `values/ic_launcher_background.xml` 背景色由 #FFFFFF 改为 **#021B6C**（取 logo 深蓝）。
  - 每个 `mipmap-<density>/ic_launcher_foreground.png`：抠除深蓝底、只留青环+wm、居中到 108dp 自适应画布安全区，尺寸 mdpi108 / hdpi162 / xhdpi216 / xxhdpi324 / xxxhdpi432。
  - `ic_launcher.png` 与 `ic_launcher_round.png`（Android 7 及以下兜底）= 完整 logo 传统位图，尺寸 48/72/96/144/192。
  - 重新生成：工程根 `gen_android_icons.py`（Pillow，依赖同目录 `cwm-logo.png`，内含抠背景阈值 lo16/hi48 羽化）。
  - 本机不编译 APK，push 后 GitHub Actions 出的 APK 即新图标。
- **同步坑**：PowerShell `Copy-Item -Recurse` 往已存在的 `res` 合并会错误嵌套成 `res/res`；同步 `res/` 到交付目录用 `robocopy 源 目标 /E`。

### 4.11 本地命令行构建 Android debug APK（已在本机实测 BUILD SUCCESSFUL）

环境（本机实际值，复现用）：
- **JDK 必须用 21**。Android Studio 自带 JBR 是 **JDK 25**，Gradle 8.11.1 直接报 `Unsupported class file major version 69`；换 JDK17 又在 capacitor 模块报「无效的源发行版: 21」（新版 Capacitor `sourceCompatibility VERSION_21`）。已放**免安装 JDK21** 于 `E:\jdk21\jdk-21.0.12.1+1`（解压即用，不写系统、不影响 Studio）。
- Android SDK 在 `E:\sdk`；`android/local.properties` 写 `sdk.dir=E:/sdk`（**该文件含本地路径，不提交**，CI 用自己的 SDK）。首次构建 Gradle 已自动补装 `build-tools;34.0.0` 与 `platforms;android-35`（licenses 已接受）。
- 工程路径含中文「白井时空」：已在 `android/gradle.properties` 加 `android.overridePathCheck=true` 才能过 AGP 路径检查。**注意** aapt/aapt2 等原生工具仍打不开中文路径的 APK（报 Illegal byte sequence），所以验证 badging 时先把 apk 复制到纯英文路径。
- `android/app/build.gradle` 顶部加了 `configurations.all { resolutionStrategy.eachDependency {...} }`，把所有 `org.jetbrains.kotlin:kotlin-stdlib*` 强制对齐 `1.8.22`，解决 `checkDebugDuplicateClasses` 的 kotlin-stdlib 1.8.22 与旧 jdk7/jdk8 1.6.21 重复类冲突。

构建命令（PowerShell）：
```
$env:JAVA_HOME="E:\jdk21\jdk-21.0.12.1+1"
$env:ANDROID_HOME="E:\sdk"; $env:ANDROID_SDK_ROOT="E:\sdk"
cd android; .\gradlew.bat assembleDebug
```
产物：`android/app/build/outputs/apk/debug/app-debug.apk`（约 13MB）；交付副本 `E:\codex\Camera-WaterMark-3.0-debug.apk`。
验证（复制到英文路径后）`E:\sdk\build-tools\36.0.0\aapt.exe dump badging`：包名 `com.shiraijikuu.cwm`、versionName 3.0、minSdk23/targetSdk35、label `Camera-WaterMark`、启动 `MainActivity`，各密度图标均走 `mipmap-anydpi-v26/ic_launcher.xml` 自适应图标，权限仅 INTERNET。
真机自动手机布局：`index.html` 初始化处新增 IIFE，满足「Capacitor 原生 / 移动 UA / 宽度≤820px」任一即自动 `body.classList.add('phone')`，APK 打开不再是桌面三栏。
装到手机：数据线连红米并开「USB 调试」→ `E:\sdk\platform-tools\adb.exe install -r app-debug.apk`；或把 apk 传到手机点击安装（允许未知来源）。**release 包需另配 keystore 签名**，当前只产出 debug。

---

## 5. Windows 重新打包流程（含必踩的坑）

```powershell
cd C:\Users\白井时空\Downloads\cwm-prototype
$env:PATH = "C:\Program Files\nodejs;$env:PATH"
$env:CSC_IDENTITY_AUTO_DISCOVERY = "false"     # 不签名，避免找证书
# 改完 index.html 后先同步两处内核，再打包
Copy-Item index.html www\index.html -Force
Copy-Item index.html android\app\src\main\assets\public\index.html -Force
npx electron-builder --win portable --x64
# 产物：release\Camera-WaterMark 3.0.0.exe；release\win-unpacked\ 是免安装目录
```

### 坑：winCodeSign 符号链接报错（非管理员 / 未开 Windows 开发者模式必现）
现象：`Cannot create symbolic link ... winCodeSign\...\darwin\10.12\lib\libcrypto.dylib`，7za 返回非零，打包失败。这些是 **macOS 代码签名工具，Windows 打包根本用不到**，但 electron-builder 会先下载解压整个 `winCodeSign-2.6.0.7z`。

已在本机解决（缓存已修好，正常情况下直接能打）。若换机器/清了缓存复现，手动执行一次即可：

```powershell
$cache = "$env:LOCALAPPDATA\electron-builder\Cache\winCodeSign"
$7z    = "node_modules\7zip-bin\win\x64\7za.exe"
# app-builder 下载的包是数字临时名（约 5.37MB 的那个 .7z），或自行下载 winCodeSign-2.6.0.7z
# 关键：-x! 排除 darwin/linux，只解 Windows 需要的，绕开符号链接权限
& $7z x "$cache\<数字>.7z" "-o$cache\winCodeSign-2.6.0" -y -bd "-x!darwin" "-x!linux"
# 解完确认 rcedit-x64.exe 存在即可：Test-Path "$cache\winCodeSign-2.6.0\rcedit-x64.exe"
```
正式缓存目录名必须是 **winCodeSign-2.6.0**（electron-builder 24.13.3 对应版本）；存在即命中、跳过下载解压。nsis 缓存（nsis-3.0.4.1 / nsis-resources-3.4.1）本机已正常。

其它注意：
- 不要加 `signAndEditExecutable:false`（会阻止 rcedit 写 exe 图标）。
- PowerShell 脚本文件若含中文路径，用 `$PSScriptRoot` 定位，别在脚本里写死中文用户名（UTF-8 无 BOM 会被当 GBK 读成乱码导致找不到文件）。

---

## 6. Android 出 APK 流程（本机出不了，走 CI）

本机没有 JDK / ANDROID_HOME / SDK，**不要尝试本地 gradle 构建**。三条路任选（详见 `BUILD_ANDROID.md`）：

1. **GitHub Actions（推荐）**：把 `camera-watermark-android` 内容推到 GitHub 仓库，`.github/workflows/android-apk.yml` 会在 push main（或 Actions 页手动 Run workflow）时：setup-jdk temurin17 → setup-android SDK35 → `./gradlew assembleDebug` → 上传 `CWM-debug-apk` artifact，到 Actions 运行页下载即可。
2. 本地装 Android Studio + JDK17 后 `npx cap open android` 用 IDE 出包。
3. 改完网页内核记得 `npx cap copy android`（或手动覆盖 `android/app/src/main/assets/public/`，本工程已手动同步好）。

出 release 签名包需要自建 keystore 并在 gradle 配置 signingConfigs，当前工程只配了 debug。

---

## 7. 热更新怎么发布（沿用你现有的 GitHub + jsDelivr 体系）

采用**壳核分离**，GitHub 仓库仍用 `github.com/shiraijikuu/camera-watermark`：

- **核（界面/功能，index.html + presets）**：提交到仓库 `main` 分支（建议放 `app/` 目录），经 jsDelivr：`https://cdn.jsdelivr.net/gh/shiraijikuu/camera-watermark@main/app/index.html`。改内核不用重发 exe，秒级生效（jsDelivr 边缘缓存卡住时用 `https://purge.jsdelivr.net/gh/shiraijikuu/camera-watermark@main/app/index.html` 强刷；`@main` 偶发顽固缓存，可改用 tag 固定地址）。
- **壳（Electron exe / Android APK）**：走 **GitHub Releases**，手动发版、用户下载覆盖安装。
- **更新清单**：仓库 `main` 根放一个 `update.json`（模板见 `update.example.json`），字段：
  - `version`：最新版本号，和客户端 `APP_VERSION` 用同一套 `cmp()` 比对（写 `"3.0"` 即可）
  - `download_url`：壳的 Releases 下载页
  - `core_url`：内核 index.html 的 jsDelivr 地址
  - `notes.zh / notes.en`：更新日志
  - 客户端 `UPDATE_URL` 指向这个 update.json 的 jsDelivr 地址。
- **插件商店体系不变**：各插件仍是独立仓库 + `plugins.json` 清单 + checksum 更新检测；但注意手机版不加载插件，插件只服务于桌面壳。

### 尚未实现、需要 Codex 补的"真·自动热更"
当前 `checkUpdate()` 只做到**检测 + 弹窗 + 跳转下载页**，不会自动下载覆盖。要做到用户点一下就更新：
- Electron：加 `preload.js`（contextBridge 暴露安全 IPC），主进程用 `net/https` 下载新 index.html 到 `userData`，启动时优先加载本地新内核、失败回退包内 `index.html`；因为 `contextIsolation:true、sandbox:true、nodeIntegration:false`，**必须走 IPC，不能在渲染进程直接 require**。
- Android：用 Capacitor 的 Filesystem + 本地 server 或热更新插件实现，思路相同。

---

## 8. 关键文件 / 改动清单（本次动过的文件）

源码工程 `cwm-prototype`：
- `index.html` —— 全部业务内核（六 bug 修复、改名、APP_VERSION=3.0、关于弹窗重写、内嵌 exifr）
- `main.js` —— Electron 主进程：`title:'Camera-WaterMark'`、`icon:cwm.ico`、`Menu.setApplicationMenu(null)` 去默认菜单、外链用系统浏览器打开
- `package.json` —— name/productName/version=3.0.0、build.files 含 cwm.ico、win.icon=cwm.ico、target portable+nsis x64
- `capacitor.config.json` —— appName=Camera-WaterMark，appId 不变
- `android/app/src/main/res/values/strings.xml` —— app_name / title_activity_main
- `android/app/build.gradle` —— versionCode 3 / versionName "3.0"
- `update.example.json` —— 更新清单模板（版本 3.0，中英 notes）
- `.github/workflows/android-apk.yml`、`BUILD_ANDROID.md` —— Android CI 与构建说明
- `www/`、`android/app/src/main/assets/public/` —— 内核分发副本（37 文件，已与 index.html 哈希一致）
- `cwm.ico`、`presets/`（36 个）

> 原 Python 项目 `E:\codex\camera-watermark` 本次**一字未改**。

---

## 9. 已知限制 / 建议后续

1. 只支持 JPG/PNG/WEBP/BMP；RAW（arw/nef/dng…）浏览器/Electron 无法解码，需要 wasm 解码库（如 libraw wasm）才能支持。
2. 自定义字体支持 ttf/otf/ttc；部分 ttc 集合字体可能加载失败。
3. 导出走 Canvas，**不会把原始 EXIF 回写进导出文件**（旧 Python 版用 Pillow 可以）。若要保留拍摄参数，需要在 Electron 主进程引入 piexif/exiftool-rs 之类在保存时注回——这是相对原 Python 版的一个功能回退点，建议优先补。
4. 真·内核自动热更新（第 7 节末）未做。
5. Android 目前只出 debug APK，未配签名 release。
6. 界面右侧参数面板在窄窗口下文字可能被裁切，后续可加最小宽度/折叠。

---

## 10. 给 Codex 的工作约定（用户偏好）

- 每次改完**明确列出改了哪些文件**；严格 SemVer，升版本要同步第 2 节全部位置。
- 成品里**不得留**测试字样、技术栈名（Electron/Tauri/Canvas/Web）、"完整版/验证版/prototype/full"等内部措辞；面向用户只说 Camera-WaterMark。
- 只清理 AI 自己产生的临时文件，**不要动** `Downloads` 其它内容和 `_user_*.png` 等用户附件。
- 不凭记忆猜实现，改前先读磁盘现状；改完用"不同于生成路径"的方式回读验证（启动 exe 截图 / 哈希比对 / JSON 解析）。
- 手机版与桌面版改同一套内核后，都要回归一遍（拖拽、缩放、水印渲染、语言切换）。


---

## 11. 2026-08-31 真机全屏适配 / 双指缩放 / 触摸拖拽 / 滑块虚化

### 11.1 改动文件（仅内核 index.html，三端同源，已四处同步，SHA256 前缀 E0105ABC）
- 工程根 `cwm-prototype/index.html`，并同步到 `www/index.html`、`android/app/src/main/assets/public/index.html`，以及交付目录 camera-watermark-windows / camera-watermark-android 内同名文件。
- 已用 JDK21 重新 `assembleDebug`，覆盖 `E:\codex\Camera-WaterMark-3.0-debug.apk`（约 13.1MB），解包确认新逻辑均在包内。

### 11.2 真机全屏适配（根因与做法）
- 旧 CSS 里 `body.phone #app{width:390px;height:844px;border:9px solid;border-radius:38px;box-shadow;animation}` 是“电脑里的手机外壳模拟器”，固定尺寸，真机 WebView 铺不满、有外框黑边。
- 现区分两种形态：桌面手动点“手机形态”按钮只加 `.phone`（保留外壳预览）；真机/移动 UA/窄屏自动检测时同时加 `.phone.device`。
- `body.phone.device` 下：#app 用 100vw × 100dvh 铺满，去 padding/border/radius/shadow/animation；grid 行高用 `calc(50px + env(safe-area-inset-top))` 与 `calc(58px + env(safe-area-inset-bottom))` 避让刘海/手势条；topbar/tabbar/sheet 用 env(safe-area-inset-*) 内边距。
- viewport meta 改为 `width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no, viewport-fit=cover`（禁整页浏览器缩放，交给应用内手势；覆盖安全区）。

### 11.3 手势（Pointer Events 多点触控，桌面鼠标同样走这套）
- 维护 `pointers` Map：两指落下进入 pinch，按“上一帧距离/中点/zoom”做增量缩放（15%–320%），以两指中点为锚并叠加中点平移；两指变一指时把剩余手指重置为 pan 基准，避免跳变。
- 单指：命中文字水印→拖水印改 offset%；否则拖空白平移。扩大了触摸热区：`.device` 下文字水印判定矩形外扩约 18 屏幕像素（换算回原图像素），解决“手指点不准、拖不动水印”。
- 桌面滚轮以光标为锚缩放、双击复位均保留。

### 11.4 其它
- 底部操作提示按形态切换文案：桌面“滚轮缩放…”，真机用新增 i18n key `dropHintTouch`（中：单指拖空白平移·双指捏合缩放·拖文字水印改位置；英同义）。applyI18n 在 `.device` 下优先取 `xxxTouch`。
- 拖动任意 `input[type=range]`（大小/旋转/不透明度等滑块）期间给 body 加 `.sliding`，#preview 加 `filter:blur(7px) brightness(.92)`（仅 transition filter，不影响 transform 跟手）；pointerup/cancel/blur 移除。
- 验证（Electron 无头，393×851，真实红米样片）：#app 与视口同尺寸且 border/radius=0；双指距离 40→90 时 zoom 100→225；单指拖水印 offset 0,0→3,-4；滑块按下 sliding=true、松手 false；无 JS 报错。凭证 `E:\codex\phone-5-真机全屏.png`。

### 11.5 更新机制现状（回应“手机和电脑更新是否同一种方式”）
- **检测更新同源**：两端跑同一套 `checkUpdate()`，都请求同一个 `UPDATE_URL`（jsDelivr 的 update.json），比对版本号。
- **安装更新都还是“整包手动覆盖”，不是静默热更**：
  - Windows：检测到新版 → 打开 Releases，用户下载新 exe/portable 覆盖。
  - Android：检测到新版 → 下载新 APK，用户在手机上确认安装覆盖（同 applicationId 覆盖安装，数据/配置保留情况取决于 WebView 存储，建议后续用 Capacitor Preferences 落盘）。
- 网页内核（HTML/JS）理论上可做 OTA 热更（Capacitor 远程拉取 www 资源覆盖本地），**当前未实现**；Android 也无法用网页方式直接替换已安装 APK。要做真热更需另加：版本清单 + 内核包下载校验 + 本地资源加载切换（Windows 同理）。这与第 9 节第 4 点是同一件事。


---

## 12. 2026-08-31（续）面板透明 / 手机去缩放条 / 图片水印可拖 / 修复参数错位

### 12.1 改动文件
- 仍只改内核 `index.html`，同步四处（工程根、www、android assets、两个交付目录），SHA256 前缀 B59511BC；已重打 `E:\codex\Camera-WaterMark-3.0-debug.apk`（约 13.2MB），解包核验下列逻辑均在包内。

### 12.2 本轮变更
1. **滑块反馈纠正**：上一版误做成“拖滑块时模糊预览图”，用户实际要的是“拖滑块时让参数面板变透明、透出被面板挡住的预览”。现改为 `body.sliding .sheet,body.sliding #rightPane{opacity:.4}`（仅手机底部 sheet 会真正遮挡预览；松手恢复，transition 只动 opacity）。
2. **手机移除缩放条**：`body.phone` 下隐藏 `.zoombar` 与 #zoomFit/#zoom100/#zoom200（双指捏合已覆盖缩放，也解决顶部工具栏被裁切）。桌面不受影响。
3. **图片水印可在预览上拖动改位置**：
   - drawImageMarks 开始时清空 `ph._imgRects=[]`；drawOneMark 绘制后 push `{i,x,y,w,h}`（原图坐标、含旋转包围盒）。
   - 新增 hitImageMark（倒序命中、触摸外扩 18px）；手势 drag 增加 `mode:'img'`，按抓取点增量反算 `slot.x/slot.y`（百分比，clamp 0–100）；拖动第 1 个且处于“与文字对齐”模式时自动切 imgAlign='无' 转自由定位；松手 buildSlots() 同步槽位面板数值。
4. **修复既有 bug（重要）**：`drawOneMark` 形参原为 `(c,s,i,W,H,textRect,isFirst)`，而调用传 8 个实参 `(…,textRect,ph,i===0)`——形参缺 ph，导致 isFirst 错误接到照片对象（恒真，多水印对齐全部被当“第 1 个”），且函数体内一旦引用 ph 就 ReferenceError、整个图片水印绘制中断。已改为 `function drawOneMark(c,s,i,W,H,textRect,ph,isFirst)`，isFirst 现在正确等于 (i===0)。

### 12.3 验证（Electron 无头 393×851，真实样片，全部 pass）
- hideZoom：手机下 zoombar/zoomFit/zoom100/zoom200 computed display 均 none。
- dragImg：图片水印 slot0 位置 50,50 → 60,59（可拖）。
- panelTransparent：拖滑块时 #rightPane opacity=0.4，松手=1。
- 回归 pinch 100→200、文字水印拖动均正常；无 JS 报错。凭证 `E:\codex\phone-6-隐藏缩放条与图片水印.png`。


---

## 13. 2026-08-31（再续）手机导出按钮 / 模糊参数左中右对齐

### 13.1 改动文件
- 仍只改内核 `index.html`，同步四处，SHA256 前缀 0A18ABE4；重打 `E:\codex\Camera-WaterMark-3.0-debug.apk`（约 13.1MB），解包核验 btnExportM / badgeAlign / 对齐分支均在包内。

### 13.2 修复：手机没有导出键
- 根因：真正的“导出当前/批量导出”按钮在 `footer.statusbar`，而 `body.phone .statusbar{display:none}` 把整个底栏隐藏了；手机底部“导出”tab 打开的 pexport 页只有格式/质量/后缀/比例，没有执行按钮。
- 做法：pexport 页内新增 `.phone-only-export` 按钮组（btnExportM/btnBatchM），CSS 默认 display:none、仅 `body.phone` 下 display:flex；点击转发到既有 btnExport/btnBatch 逻辑（不重复实现导出）。桌面仍用窗口底部按钮，不受影响。

### 13.3 新增：模糊卡片参数对齐（左/居中/右）
- state 新增 `badgeAlign:'居中'`；模糊卡片组“字段模板”下新增下拉 `#badgeAlign`（左对齐/居中/右对齐，中英 i18n key b_align），buildControls 用 fillSelect 绑定。
- drawBadges 的**竖块分支**（左参数/右参数/左右分离的右块都走这里）按对齐算每行起点 xRow：居中=块中心-行宽/2（原行为）；左对齐=`left+padx`（各行标签框左缘齐平）；右对齐=`right-padx-行宽`（各行数值右端齐平）。横宽条（上/下参数）为单行排列，不受影响。
- 左右分离布局右块同样生效（共用 drawBadges 竖块分支）。

### 13.4 验证（Electron 无头 393×851，零 console 错误）
- 手机下 .phone-only-export computed display=flex、两按钮存在且 onclick 为函数；桌面 display=none。
- badgeAlign 选项恰为 左对齐/居中/右对齐；左参数布局三档 render 均无异常。
- 点“导出”tab sheet 正常打开并显示两个导出按钮。凭证：phone-7-手机导出按钮.png、phone-8-参数左对齐.png、phone-9-参数右对齐.png。


---

## 14. 2026-09-01 参数两列对齐细化 / 带框标签等大

### 14.1 改动文件
- 仅内核 `index.html`，同步四处，SHA256 前缀 CF282250；重打 `E:\codex\Camera-WaterMark-3.0-debug.apk`（约 13.2MB），解包核验两列网格与带框等宽逻辑均在包内。

### 14.2 变更（drawBadges 竖块分支，作用于左参数/右参数/左右分离右块）
1. **标签、数值拆成两个等宽列**：先算 maxTagW（最宽标签）、maxValW（最宽数值），gridW=两列+列间距；整块按 badgeAlign 定 xBase。标签列宽=maxTagW、数值列统一从 vCol0=xBase+maxTagW+ig 起。三档：左对齐两列各自左缘齐平；右对齐各自右缘齐平；居中各自在列内居中。修正了旧版“整行一起对齐、数值因标签宽度不同而错位”。
2. **带框标签统一大小**：framed（参数标签框开启）时，mm/F/S/ISO 标签框宽度统一为列宽 maxTagW（=最宽标签），文字在框内居中，于是所有标签框等大、左右缘齐平。
3. **不带框（frameless，字段模板含“无框”）维持原样**：标签宽度随文字、按方向对齐，未改其行为。
4. 横宽条（上/下参数，单行连排）未改。

### 14.3 验证（Electron 无头 393×851，零 console 错误）
- fillText 探针：左/中/右三档下三个数值列 x 完全相同（374.73 / 515.99 / 657.25）、textAlign 分别 left/center/right。
- 带框 'mm / F / S / ISO'：mm/F/S/ISO 四标签 center x 全为 238.39（框等宽）；无框模板渲染不报错。凭证 phone-10-带框标签等大两列对齐.png。


---

## 15. 2026-09-01 修复手机导出（直接进系统相册）+ 移除批量导出

### 15.1 根因
旧 download() 用 `<a download=blobURL>.click()`，Android WebView 默认不处理 blob 下载，导致手机点导出无反应。

### 15.2 方案：自定义原生 Capacitor 插件直写 MediaStore（无需选位置/不弹分享）
新增/修改的**原生文件（Android 工程，务必随包提交）**：
- `android/app/src/main/java/com/shiraijiku/cwm/GallerySaverPlugin.java`（新增）：@CapacitorPlugin(name="GallerySaver")，方法 saveImage({base64,name,mime})。Android 10+(API29) 用 MediaStore.Images 写入 `Pictures/Camera-WaterMark`（IS_PENDING 流程，**免存储权限、直接进图库**）；Android 9- 写公共 Pictures 目录 + MediaScanner 扫描（需 WRITE_EXTERNAL_STORAGE）。IO 在子线程。
- `MainActivity.java`：onCreate 里 super.onCreate 之前 registerPlugin(GallerySaverPlugin.class)。
- `AndroidManifest.xml`：加 `<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" android:maxSdkVersion="28"/>`（API29+ 不需要任何存储权限）。

### 15.3 内核 index.html（SHA256 前缀 E4EB2ABC，四处同步）
- 新增 isNativeApp()；download() 分两路：原生 App → canvas.toDataURL 取 base64 → window.Capacitor.Plugins.GallerySaver.saveImage 直存相册；浏览器/Windows(Electron) → 维持 a.download。
- exportOne 加“正在导出…/已保存到系统相册/导出失败”状态与 try/catch。
- **移除批量导出全部**：删除桌面 btnBatch、手机 btnBatchM 两个按钮及其绑定（无 btnBatch 残留）；手机导出页只留单个“导出当前”。照片页“全选/全不选”保留未动。
- 曾临时安装 @capacitor/filesystem、@capacitor/share，评估后改用自定义直存方案，已 npm uninstall，package.json 依赖回到 core/android/cli。

### 15.4 验证
- gradle assembleDebug 成功；解包 classes4.dex 含 GallerySaver 类；aapt dump permissions：INTERNET + WRITE_EXTERNAL_STORAGE(maxSdkVersion=28)。
- Electron 无头回归：DOM 桌面/手机均“有单个导出、无批量”；走桌面分支 exportOne 状态=已导出；零 console 错误。
- **真机“直接进图库”需在实体 Android 上点导出确认（无头环境无法验证 MediaStore 落盘）**：预期图片出现在 图库/相册 的 Camera-WaterMark 相册，Android 10+ 不弹任何权限或位置选择。Windows 端仍走浏览器下载目录。

### 15.5 待办 / 注意
- Android 9 及以下首次导出若返回 NEED_PERMISSION，需要补运行时权限申请（当前用户机 Android 13 不受影响）。
- 重名文件 MediaStore 会自动加序号，不会覆盖。


---

## 16. 2026-09-01 移除模糊卡片黑底（删底纹功能）+ 导出结果 Toast

### 16.1 黑色背景根因与处理
- 黑底来自“底纹透明度” state.backdrop：drawBlurStyle 里在水印盒 wm 上叠一层 rgba(0,0,0,backdrop/100)。代码默认虽为 0，但旧存档(localStorage)可能记住非 0 值导致复现。
- 按用户要求**彻底删除底纹功能**（不是只改默认值），共 4 处，全包 backdrop 出现次数=0：
  1. state 默认字段 backdrop:0 删除；
  2. drawBlurStyle 内底纹 fillRect 整段删除（保留整图“背景压暗 darken”，那是模糊层次、不是黑底）；
  3. 设置容器 c_backdrop 删除；
  4. buildControls 的 sliderTo('c_backdrop',...) 删除。
- 注意：c_darken(背景压暗) 保留，别误删。

### 16.2 导出结果弹出提示（Toast）
- 新增 showToast(msg)：动态 #appToast，屏幕中上居中、深色圆角、淡入、1.7s 自动淡出，不依赖主题、不阻塞操作。
- exportOne：成功弹“已保存到系统相册/已导出”；失败弹“导出失败：<原因>”，并把原生 reject 码 NEED_PERMISSION 映射为中文；状态栏 setStatus 同步保留。

### 16.3 验证（Electron 无头，零 console 错误）
- document 内 state.backdrop 绘制代码不存在(false)。
- 模拟原生成功：#appToast 存在、文本“已保存到系统相册”、opacity=1；模拟 GallerySaver reject(Error SAVE_FAILED)：toast=“导出失败：SAVE_FAILED”（原因透传）。
- 模糊卡片渲染：水印块后无黑色矩形，模糊背景正常透出，mm/F/S/ISO 等大框与两列对齐正常（凭证 phone-11）。
- 内核 SHA256 前缀 CB1ECAA，四处同步；assembleDebug 成功，APK 解包无 backdrop、含 showToast。

### 16.4 改动文件
- 仅 index.html（无原生改动）；新 APK E:\codex\Camera-WaterMark-3.0-debug.apk。


---

## 17. 2026-09-01 修复真机导出无反应/无成功提示（关键：缺 JS 插件代理）

### 17.1 真正根因（比第15节更深）
单文件 index.html **没有 import @capacitor/core**，而 Capacitor 的原生插件代理对象必须由 JS 端 registerPlugin() 创建并挂到 window.Capacitor.Plugins；native-bridge.js 只注入底层通道 cap.toNative，**不会自动为原生插件生成代理**。
结果真机上 window.Capacitor.Plugins 为 undefined → 旧 download() 里 Plugins.GallerySaver 取不到 → 回退到 <a download>（WebView 无效）→ 原生 GallerySaver 从未被调用（图片其实没进相册），成功提示也因此异常。第15节只写了原生侧，漏了 JS 代理这一环。

### 17.2 修复（仅 index.html，SHA256 前缀 83E79009，四处同步）
- 新增 nativePlugin(name)：用 window.Capacitor.toNative(plugin,method,opts,{resolve,reject}) 自建 Promise 代理（等价 registerPlugin 核心，无需打包器/import core）；getGallerySaver() 优先取 Capacitor.Plugins.GallerySaver，缺失则回退 nativePlugin('GallerySaver')。
- exportOne 重写：点击立即 showToast('正在保存…')；原生分支 toDataURL→toNative 调 saveImage，Promise.race 加 20s 超时(TIMEOUT)；成功 toast“✓ 已保存到系统相册”，失败 toast“导出失败：<原因>”（NEED_PERMISSION/TIMEOUT 中文映射）；桌面分支改名 browserDownload，toast“✓ 已导出”。
- showToast 不再依赖 requestAnimationFrame（直接 opacity=1 显示，避免某些 WebView 不触发），时长 2.2s，top 44%。

### 17.3 验证（Electron 无头，零错误）
模拟真机“只有 toNative、没有 Capacitor.Plugins”的环境：捕获到 toNative 调用 plugin=GallerySaver/method=saveImage/带 base64/文件名 r_wm.jpg，toast=✓ 已保存到系统相册；原生 reject({message:MEDIA_FAIL})→toast“导出失败：MEDIA_FAIL”；桌面→✓ 已导出。APK 解包含 nativePlugin/正在保存/TIMEOUT；assembleDebug 成功。

### 17.4 仍需真机确认
无头无法执行 Android MediaStore，请在实体机点导出：应先弹“正在保存…”，随后“✓ 已保存到系统相册”，并在图库 Camera-WaterMark 相册看到图片；若弹“导出失败：xxx”把 xxx 反馈。原生侧 GallerySaverPlugin.java/MainActivity/Manifest 第15节已就位、dex 含类，无需再改。


---

## 18. v3.0 正式签名 release + 关于页署名 + 外链（SHA256 内核前缀 1F7AA7D8）

### 18.1 关于页加入作者 / GitHub / 协议（index.html）
- 抽出 openAbout()/aboutHTML()，桌面顶栏“关于”(btnAbout) 与手机导出页新增的“关于”按钮(btnAboutM) 共用；手机顶栏/底栏默认隐藏，故入口放在导出页 .phone-only-export 内。
- 关于内容：Camera-WaterMark v3.0 简介 + 分隔线 + 作者 **shiraijikuu** + 可点击 GitHub 仓库 https://github.com/shiraijikuu/camera-watermark + 开源协议 MIT License（中英双语随 L() 切换）。
- openExternal(url)：桌面 window.open(url,'_blank')（main.js 的 setWindowOpenHandler 已转交系统浏览器）；原生优先 Capacitor.Plugins.Browser，否则 nativePlugin('Browser').open({url})（@capacitor/browser，系统浏览器/Custom Tabs 打开，不离开 App）；都失败则 copyText 复制链接并 toast 提示。copyText 含 navigator.clipboard + execCommand 双兜底。

### 18.2 新增依赖 @capacitor/browser（用于 App 内外链）
- package.json dependencies 增加 "@capacitor/browser": "^8.0.4"；已 npm install 并 npx cap sync android。
- 自动生成文件已同步：android/app/capacitor.build.gradle（implementation project(':capacitor-browser')）、android/capacitor.settings.gradle（include capacitor-browser → node_modules/@capacitor/browser/android）。
- 关键坑：browser 1.9.0 依赖 androidx.browser:browser:1.9.0，强制 compileSdk 36，而本工程 compileSdk 35 → checkReleaseAarMetadata 报错。已在 android/app/build.gradle 顶部 configurations.all 里把 androidx.browser:browser 锁到 **1.8.0**（仅需 SDK34、API 兼容），不要升级 AGP/compileSdk。

### 18.3 Release 正式签名（此前只有 debug）
- 密钥库：android/app/cwm-release.jks，alias=cwm，store/key 密码=Cwm2026release，RSA2048，有效期 10000 天，dname CN=shiraijikuu, OU=Camera-WaterMark, O=shiraijikuu, L=Shenzhen, C=CN。
- android/keystore.properties（storeFile=cwm-release.jks，注意 file() 相对 app 模块目录，不要再带 app/ 前缀，否则 app/app/ 找不到）。
- android/app/build.gradle：新增 signingConfigs.release（仅当 keystore.properties 存在才赋值）+ buildTypes.release.signingConfig（有密钥用 release、无则回退 debug，保证他人 clone 缺密钥也能构建）。
- apksigner verify 结果：v1=true、v2=true（可正常安装/上架；v3/v4 未开不影响）。
- 产物：app-release.apk → E:\codex\Camera-WaterMark-3.0-release.apk（12.27MB，已签名）；debug 同步重打 13.46MB。
- 安全：.gitignore 已忽略 *.jks / *.keystore / android/keystore.properties / local.properties / build 产物。**签名私钥与密码务必离线备份（另存 E:\codex\CWM签名密钥备份\），一旦泄露或丢失：泄露=他人可冒充更新；丢失=以后无法用同签名覆盖升级，只能换包名。**

### 18.4 改动文件清单
- index.html（关于/openExternal/手机关于入口，四处同步，哈希 1F7AA7D8）
- package.json（+@capacitor/browser）
- android/app/build.gradle（release 签名 + browser 1.8.0 锁定）
- android/keystore.properties（新增，含密码，勿入库）
- android/app/cwm-release.jks（新增签名私钥，勿入库，已离线备份）
- android/app/capacitor.build.gradle、android/capacitor.settings.gradle（cap sync 生成）
- .gitignore（新增）
- 以上均已同步到 E:\codex\camera-watermark-android；index.html/package.json 同步到 camera-watermark-windows。

### 18.5 复现正式包命令（PowerShell，在 android 目录）
$env:JAVA_HOME="E:\jdk21\jdk-21.0.12.1+1"; $env:ANDROID_HOME="E:\sdk"; $env:ANDROID_SDK_ROOT="E:\sdk"; $env:PATH="$env:JAVA_HOME\bin;$env:PATH"; .\gradlew.bat assembleRelease
产物在 app/build/outputs/apk/release/app-release.apk。Windows 桌面正式安装包另用 electron-builder（package.json 已配 nsis+portable）：npm run build:win。


---

## 19. 修复：横排（上/下参数）参数标签框等大 + 左/中/右对齐（内核哈希 C6D89100）

### 19.1 根因
第14节的“两列对齐 + 带框标签等大”只写进了 drawBadges 的**竖排分支**（左参数/右参数/左右分离右块）；**横排分支**（排版布局=上参数/下参数，而默认布局正是“下参数”）仍调用旧 drawUnit：标签框宽度随文字（mm/F/S/ISO 大小不一），且整组恒居中、不响应 badgeAlign。所以桌面默认界面看起来“没改”。

### 19.2 修复（仅 index.html 的 drawBadges horizontal 分支）
- framed（字段模板不含“无框”）：先求 hMaxTag=max(各标签测量宽)，每个标签框统一画成 hMaxTag 宽、文字居中 → mm/F/S/ISO 框等大；数值统一跟在框后一个 gap。
- frameless（“无框”模板）：tagWof 直接取文字测量宽，行为与旧版完全一致（不带框不动）。
- 整组水平起点按 state.badgeAlign：左对齐=left+padx；居中=(l+r)/2-totalW/2；右对齐=right-padx-totalW（含品牌 logo/title 一起对齐）。
- 竖排分支未动（本就正确）。旧 drawUnit 现已无调用方（死代码，保留未删，可后续清理）。

### 19.3 验证（Electron 无头，桌面形态，零错误）
- 离屏横排：4 个标签框宽 [71,71,71,71] 全等；无框模板 rr 画框 0 次；超宽盒下左/中/右首框起点 143<257<372（distinct/order 均 true）。窄盒三档趋同是字号自适应放大到占满行宽的正常表现。
- 真实红米照片（E:\2 图）桌面形态、默认“下参数”+“mm / F / S / ISO”：底部横排 [mm][F][S][ISO] 四框等大、数值整齐（凭证 desktop-横排参数框等大对齐.png）。
- release/debug 均 BUILD SUCCESSFUL；四份内核 SHA256 一致 C6D89100。


---

## 20. Windows 正式打包（electron-builder，nsis 安装版 + portable 便携版）

### 20.1 package.json 调整
- 补元数据：author="shiraijikuu"、license="MIT"、homepage/repository 指向 https://github.com/shiraijikuu/camera-watermark。
- build.files 增加 "!node_modules/**/*"：桌面端 main.js 只用 electron 内置模块、index.html 无 node require（唯一 require 字样是 exifr UMD 运行时检测），故不把 @capacitor/* 等生产依赖打进桌面包，避免臃肿。
- win target=nsis+portable（x64），icon=cwm.ico，sign=null（不代码签名）；nsis 非一键、可改安装目录、建桌面快捷方式；输出目录 release/。

### 20.2 产物与校验
- release/Camera-WaterMark Setup 3.0.0.exe（安装版 83.43MB）、release/Camera-WaterMark 3.0.0.exe（便携版 83.13MB）；已复制到 E:\codex\Camera-WaterMark-3.0.0-Setup.exe / -portable.exe。体积主要是 Electron/Chromium 运行时。
- app.asar 9.21MB：解包校验含 index.html（SHA256 前缀 C6D89100，与最新内核一致）、main.js、cwm.ico、cwm-logo.png、presets 36 个文件；无 node_modules；包内 package.json author=shiraijikuu/license=MIT。
- 冒烟：启动 win-unpacked/Camera-WaterMark.exe，进程存活 6s 未崩溃退出。

### 20.3 复现命令
在工程根：npm run build:win（=electron-builder --win --x64，首次会下载 nsis 缓存）。仅便携版：npm run build:win-portable。

### 20.4 注意
- 未做 Windows 代码签名（个人开源通常不买证书）：用户首次双击安装时 SmartScreen 可能提示“未知发布者”，点“更多信息→仍要运行”即可；若以后要消除该提示，需购买 OV/EV 代码签名证书并配置 win.certificateFile。
- Android 包名仍为 com.shiraijikuu.cwm（少一个 u，属应用标识、非显示署名）；显示署名/证书 CN 已全部是 shiraijikuu。是否改 applicationId 待用户决定（发布后不可改）。


---

## 21. Android 包名重命名 com.shiraijiku.cwm → com.shiraijikuu.cwm（发布前一次性变更）

### 21.1 原因
统一为作者拼写 shiraijikuu。applicationId/namespace 是应用唯一标识，发布后不可改，故在上架前改齐。显示署名、签名证书 CN 此前已为 shiraijikuu。

### 21.2 改动清单
- android/app/build.gradle：namespace 与 applicationId 两处。
- capacitor.config.json、android/app/src/main/assets/capacitor.config.json：appId。
- android/app/src/main/res/values/strings.xml：package_name、custom_url_scheme。
- package.json：build.appId（electron-builder appId 一并统一）。
- Java 包目录迁移：src/main/java/com/shiraijiku/cwm/{MainActivity,GallerySaverPlugin}.java → src/main/java/com/shiraijikuu/cwm/，两文件首行 package 同步改；删除旧 com/shiraijiku 目录。
- AndroidManifest.xml **无需改**：Activity 用相对名 .MainActivity、FileProvider 用 ${applicationId}.fileprovider，自动跟随。

### 21.3 构建与验证（必须 clean，改 namespace 不能用增量缓存）
- npx cap sync android；gradlew clean assembleRelease assembleDebug（JDK21/SDK 路径同前）。
- aapt dump badging：package name='com.shiraijikuu.cwm'、launchable-activity=com.shiraijikuu.cwm.MainActivity；dex 新包类命中、com/shiraijiku/cwm 旧类 0 残留；apksigner v1+v2 通过。

### 21.4 注意
- 新 applicationId 对系统而言是“新应用”：手机上若装过旧 com.shiraijiku.cwm 的 debug 包，需先卸载再装，无法覆盖升级（未发布、无用户，无影响）。
- 签名 keystore 不变（签名与包名相互独立），仍是 cwm-release.jks / alias cwm。

---

## 22. 修复：手机形态补上「检查更新」入口

### 22.1 根因
手机形态 CSS `body.phone .statusbar{display:none}` 隐藏了底部状态栏，而「检查更新」按钮原本只存在于状态栏，导致手机端无入口；checkUpdate() 逻辑本身通用、手机可跑。

### 22.2 改动（仅 index.html）
- I18N zh/en 新增 checkUpdate 词条（检查更新 / Update）。
- 手机导出 sheet（.phone-only-export）在「关于」旁并排新增 btnCheckUpdateM，绑定 checkUpdate(false)。
- checkUpdate 更新弹窗的「下载」由 window.open 改为 openExternal(url)：手机走 @capacitor/browser 系统浏览器打开 Releases，桌面仍由 main.js 转系统浏览器。

### 22.3 验证
Electron 无头手机形态：按钮存在且 .phone-only-export display=flex；stub fetch 返回 9.9.9 后点击弹出「发现新版本」，点下载 openExternal 收到 releases URL；桌面状态栏 btnCheckUpdate 仍在；零控制台错误。clean assembleRelease/assembleDebug 成功，包名 com.shiraijikuu.cwm、签名 v1+v2 通过，四份内核哈希一致（42ab62b3）。凭证：E:/codex/手机导出页-检查更新入口.png。

---

## 23. 修复：手机形态补上「语言切换」入口 + 切换时刷新 sheet 标题

### 23.1 根因
语言下拉 langSel 只在底部状态栏，手机形态状态栏 display:none，导致手机无法切换语言；另外 sheet 标题 sheetTitle 在打开 sheet 时按当时语言一次性赋值，切语言后不会重刷（无 data-i18n）。

### 23.2 改动（仅 index.html）
- I18N zh/en 新增 language 词条（语言 / Language）。
- 抽出 setLang(v)：桌面 langSel 与手机 langSelM 共用，双向回写两个下拉的选中值，再统一 applyI18n/buildControls/bindStaticRepaint/fillTheme/scheduleRender。
- 手机导出 sheet 新增 langSelM（简体中文 / English）。
- setLang 末尾：若底部 sheet 处于打开状态，按当前激活 tab 重设 sheetTitle（导出/Export、编辑参数/Edit），修掉切语言后标题仍是旧语言。

### 23.3 验证
Electron 无头手机形态：两个下拉初始 zh 且手机端可见；手机切 en 后 LANG=en、桌面下拉同步 en、About/Update 等全部联动、sheetTitle=Export；桌面切回 zh 后手机下拉同步 zh、文本回中文；零控制台错误。assembleRelease/assembleDebug 成功，包名 com.shiraijikuu.cwm、签名通过。凭证：E:/codex/手机语言切换-英文界面.png。
