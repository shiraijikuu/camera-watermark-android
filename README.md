# Camera-WaterMark — Android（v3.0）

> **官网 Website**：https://itangxs.top/camera-watermark/ （EXIF 相机水印 · 跨平台落地页）
> **在线试用 Try Online**：https://shiraijikuu.github.io/camera-watermark-pwa/
> **PWA 仓库 PWA Repository**：https://github.com/shiraijikuu/camera-watermark-pwa

[![官网 Website](https://img.shields.io/badge/官网-Website-blue?style=for-the-badge&logo=googlechrome&logoColor=white)](https://itangxs.top/camera-watermark/)
[![在线试用 Try Online](https://img.shields.io/badge/在线试用-Try%20Online-green?style=for-the-badge)](https://shiraijikuu.github.io/camera-watermark-pwa/)
[![GitHub](https://img.shields.io/badge/GitHub-源码-black?style=for-the-badge&logo=github&logoColor=white)](https://github.com/shiraijikuu/camera-watermark)

> 相机照片水印工具安卓端：离线读取 EXIF，添加文字 / 图片 / 模糊卡片水印，导出直存系统相册。
> 作者：**shiraijikuu**　|　协议：MIT

**EN:** Offline camera-photo watermark tool for Android. Reads EXIF (brand / model / focal / shutter / aperture / ISO / date) and stamps text, image, or blur-card watermarks; brand-logo & parameter-badge presets; 8 themes; exports straight to the system gallery (`Pictures/Camera-WaterMark`). Built with a single-file HTML5 Canvas kernel wrapped by Capacitor 8 (WebView).

**中文：** 安卓端离线相机照片水印工具。读取 EXIF（品牌 / 型号 / 焦距 / 快门 / 光圈 / ISO / 日期），添加文字 / 图片 / 模糊卡片水印；支持品牌标与参数框预设、8 套主题；导出直存系统相册（`Pictures/Camera-WaterMark`）。由单文件 HTML5 Canvas 内核 + Capacitor 8（WebView）构建。

## 跨平台仓库 / Cross-platform repositories
- **主仓库 / Main:** [shiraijikuu/camera-watermark](https://github.com/shiraijikuu/camera-watermark)
- **PWA 在线版 / PWA:** [shiraijikuu/camera-watermark-pwa](https://github.com/shiraijikuu/camera-watermark-pwa) · [在线试用](https://shiraijikuu.github.io/camera-watermark-pwa/)
- **Windows 版 / Windows:** [shiraijikuu/camera-watermark-windows](https://github.com/shiraijikuu/camera-watermark-windows)

## 构建 / Build
```powershell
# 在 android\ 目录（需 JDK21 + Android SDK）
.\gradlew.bat assembleRelease   # 正式签名包（有 keystore.properties 时自动用正式签名）
.\gradlew.bat assembleDebug     # 调试包
```
- 内核为 `www/index.html`；改动后执行 `npx cap sync android` 同步到原生 assets，再重新打包。
- 详细交接见 [`HANDOFF_FOR_CODEX.md`](HANDOFF_FOR_CODEX.md) 与 [`BUILD_ANDROID.md`](BUILD_ANDROID.md)。

## 许可 / License
MIT
