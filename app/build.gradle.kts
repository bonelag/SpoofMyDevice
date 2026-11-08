plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

val shadowhookNative by configurations.creating
val shadowhookDirectory = layout.buildDirectory.dir("shadowhook-native")
val prepareShadowhookNative by tasks.registering(Sync::class) {
    from(provider { zipTree(shadowhookNative.singleFile) }) {
        include("jni/**/libshadowhook.so", "prefab/modules/shadowhook/include/**")
    }
    into(shadowhookDirectory)
}
tasks.configureEach {
    if (name.startsWith("configureCMake") || name.startsWith("buildCMake")) dependsOn(prepareShadowhookNative)
}

android {
    namespace = "com.devicespooflab.hooks"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.spoofmydevice"
        minSdk = 26
        targetSdk = 37
        versionCode = 14
        versionName = "2.0.6"
        externalNativeBuild {
            cmake {
                arguments += "-DSMD_SHADOWHOOK_DIR=" + shadowhookDirectory.get().asFile.absolutePath.replace('\\', '/')
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    ndkVersion = "27.0.12077973"
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    packaging {
        jniLibs {
            pickFirsts += "**/libshadowhook.so"
        }
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.06.00")

    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3:1.5.0-alpha24")
    implementation("androidx.compose.material3:material3-window-size-class")
    implementation("com.materialkolor:material-kolor:4.1.1")
    implementation("top.yukonga.miuix.kmp:miuix-ui:0.9.4")
    implementation("com.bytedance.android:shadowhook:2.0.1")
    shadowhookNative("com.bytedance.android:shadowhook:2.0.1@aar")
    implementation("com.google.android.material:material:1.14.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
    // compileOnly("de.robv.android.xposed:api:82") Failed to resolve
    compileOnly(files("libs/api-82.jar"))

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
}
