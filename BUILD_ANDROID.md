# CWM 安卓 APK 构建说明

Web 内核（index.html + 36 个预设图）已同步到 `android/app/src/main/assets/public/`。
本机没有 Java/Android SDK 也没关系，**推荐用方式 A（云端自动打包）**。

---

## 方式 A：GitHub Actions 云端打包（推荐，无需本地环境）

1. 把整个项目 push 到 GitHub 仓库
2. 仓库已带 `.github/workflows/android-apk.yml`，push 到 main 后会自动编译
3. 也可在仓库页面 Actions → "Build Android APK" → Run workflow 手动触发
4. 等约 5-8 分钟，点进该次运行，在底部 Artifacts 下载 `CWM-debug-apk.zip`，解压即得 app-debug.apk
5. 把 APK 传到手机安装（需开启"允许安装未知来源应用"）

> 每次改完 `www/index.html`，先同步到 `android/app/src/main/assets/public/` 再 push，CI 就会出最新包。

## 方式 B：Android Studio 本地打包

1. 安装 Android Studio（勾选 Android SDK，JDK 用自带的 17）
2. Open → 选择本项目的 `android/` 目录
3. 等 Gradle 同步完成（首次下载依赖约 5-10 分钟）
4. 菜单 Build → Build Bundle(s) / APK(s) → Build APK(s)
5. 产物在 `android/app/build/outputs/apk/debug/app-debug.apk`

## 方式 C：命令行打包

前置：JDK 17 + Android SDK（compileSdk 35），设置 ANDROID_HOME。

```bash
cd android
./gradlew assembleDebug      # Windows 用 gradlew.bat assembleDebug
# 产物：android/app/build/outputs/apk/debug/app-debug.apk
```

## 发布 Release 签名版

1. Android Studio：Build → Generate Signed Bundle / APK → APK
2. Create new... 创建签名密钥（.jks），务必记住密码并妥善备份
3. 选 release，编译后得到可上架/正式分发的 APK
4. CI 出的是 debug 包，适合自用和测试；正式分发建议用签名 release 包

## 修改 Web 内核后如何同步

```bash
# 改完根目录 index.html 后
copy /Y index.html www\index.html
# 再把 www 内容覆盖到 android/app/src/main/assets/public/
xcopy /E /Y www\* android\app\src\main\assets\public\
```

## 版本信息

- minSdk 23（Android 6.0+），compile/targetSdk 35
- Gradle 8.11.1，AGP 8.7.2，需 JDK 17
- 包名 com.shiraijikuu.cwm，应用名 CWM
