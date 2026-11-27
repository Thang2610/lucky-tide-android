plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Địa chỉ server mặc định điền sẵn ở màn hình đầu (người dùng đổi được trong app).
// Có thể truyền khi build:  ./gradlew assembleRelease -PserverUrl=https://game.tenmien.com
val serverUrl: String = (project.findProperty("serverUrl") as String?) ?: "https://"

android {
    namespace = "com.luckytide.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.luckytide.app"
        minSdk = 24
        targetSdk = 34
        versionCode = 2
        versionName = "1.1"
        buildConfigField("String", "SERVER_URL", "\"$serverUrl\"")
    }

    // Ký bản release bằng keystore riêng nếu có (xem README); không có thì dùng bản debug để cài thử
    signingConfigs {
        create("release") {
            val ks = System.getenv("KEYSTORE_FILE")
            if (ks != null && file(ks).exists()) {
                storeFile = file(ks)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true                          // R8: thu gọn + tối ưu mã -> APK nhẹ, mở nhanh hơn
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            val rel = signingConfigs.getByName("release")
            signingConfig = if (rel.storeFile != null) rel else signingConfigs.getByName("debug")
        }
    }
    buildFeatures { buildConfig = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.webkit:webkit:1.11.0")
}
