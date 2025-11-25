# 📸 智能图文发布助手

一个功能丰富的 Android 社交发布应用，集成了多图管理、智能位置标记和 AI 文案生成等高级功能。

## 🌟 项目简介

本项目是一个现代化的 Android 应用，旨在模拟主流社交平台的图文发布体验。它不仅实现了基础的多图上传、拍照、文本编辑等功能，更创新性地集成了两大亮点功能：

1.  **智能位置标记**：通过 GPS 获取经纬度，并利用反向地理编码技术，自动转换为用户可读的城市名称。
2.  **AI 智能文案生成**：集成智谱AI大语言模型，分析用户上传的图片，自动生成符合场景的趣味文案，极大地提升了创作效率和趣味性。

## 📱 应用截图
![应用界面](https://github.com/Telescope11/SmartSocialPublisher/blob/main/web.jpg)

## ✨ 主要功能

### 🚀 核心功能
-   **🖼️ 多图管理**：支持从相册选择多张图片（最多9张）或直接调用相机拍照。
-   **✏️ 富文本编辑**：支持输入文案，并实时显示字数统计（限制500字）。
-   **🔄 拖拽排序**：支持长按拖动图片，自由调整发布顺序。
-   **#️⃣ 快捷插入**：提供热门话题和@好友的快捷插入功能，通过底部弹窗选择。
-   **🗑️ 轻松删除**：点击图片上的删除按钮即可移除。

### 🧠 高级功能 
-   **📍 智能位置标记**：
    -   动态请求位置权限。
    -   获取设备经纬度坐标。
    -   调用 `Geocoder` 进行反向地理编码，将坐标精准转换为城市名称（如"北京市"）。
-   **🤖 AI 智能文案生成**：
    -   集成 **智谱AI (GLM-4V)** 多模态大模型。
    -   自动将用户上传的图片压缩并转换为 Base64 格式。
    -   发送网络请求至智谱AI API，分析图片内容。
    -   接收并解析返回结果，将生成的文案自动填入输入框。

## 🛠️ 技术栈

-   **语言**: Kotlin
-   **UI**: XML, ViewBinding
-   **架构**: MVVM 思想
-   **异步处理**: Kotlin Coroutines (`lifecycleScope`)
-   **网络请求**: OkHttp
-   **图片加载**: Glide
-   **UI组件**: Material Components, RecyclerView, BottomSheetDialog, AlertDialog
-   **硬件集成**: Camera API, LocationManager (GPS)
-   **外部API**: 智谱AI API

## 📋 项目结构

本项目采用标准的Android Studio项目结构，主要文件和目录组织如下：

*   **`app/`**：Android应用的核心模块。
    *   **`src/main/`**：主要源代码集。
        *   **`java/com/example/myapplication/`**：Kotlin源代码目录，存放所有业务逻辑。
            *   `MainActivity.kt`：应用的主活动，包含所有核心功能逻辑。
            *   `ImageAdapter.kt`：RecyclerView的适配器，用于管理和展示图片列表。
        *   **`res/`**：应用资源目录。
            *   **`layout/`**：存放所有XML界面布局文件。
                *   `activity_main.xml`：主界面的布局文件。
                *   `item_image.xml`：单个图片项的布局文件。
                *   `dialog_bottom_sheet_list.xml`：底部弹窗列表的布局文件。
            *   **`values/`**：存放应用中使用的常量资源，如字符串、颜色、尺寸等。
        *   `AndroidManifest.xml`：应用的清单文件，用于声明应用的组件、权限等元数据。
    *   **`build.gradle.kts`**：模块级别的构建脚本，定义该模块的依赖和配置。

*   **`README.md`**：项目的主说明文档，即您正在阅读的文件。


## 🚀 快速开始

### 前置条件

-   Android Studio Arctic Fox 或更高版本
-   Android SDK (API 24 或更高)
-   一台 Android 真机或模拟器
-   一个有效的 [智谱AI API Key](https://open.bigmodel.cn/)

### 安装步骤

1.  **克隆项目**
    bash
    git clone https://github.com/Telescope11/SmartSocialPublisher.git
    cd SmartSocialPublisher

2.  **配置 API Key**
    -   前往 [智谱AI开放平台](https://open.bigmodel.cn/) 注册并获取你的 API Key。
    -   打开 `app/src/main/java/com/example/myapplication/MainActivity.kt` 文件。
    -   找到以下代码行，并将 `"YOUR_API_KEY_HERE"` 替换为你自己的 Key：
        kotlin
    private const val ZHIPU_API_KEY = “YOUR_API_KEY_HERE”
    > ⚠️ **安全警告**: 为了演示方便，API Key 直接写在了客户端代码中。在生产环境中，**强烈建议**通过后端服务器来代理API请求，以保护密钥安全。

3.  **构建并运行**
    -   使用 Android Studio 打开项目。
    -   等待 Gradle 同步完成。
    -   连接你的 Android 设备或启动模拟器。
    -   点击运行按钮 (`▶`) 将应用安装到设备上。

4.  **授予权限**
    -   首次使用相机或位置功能时，系统会弹出权限请求框，请点击"允许"。

## 🤝 贡献

欢迎提交 Issue 来报告 bug 或提出新功能建议！如果你愿意贡献代码，可以 Fork 本项目，创建你的特性分支，然后提交 Pull Request。

## 📄 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情。

## 📧 联系方式

-   项目链接: [https://github.com/Telescope11/SmartSocialPublisher](https://github.com/Telescope11/SmartSocialPublisher)
-   我的邮箱 : (telescope11@qq.com)
