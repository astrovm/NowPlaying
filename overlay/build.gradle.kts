import com.google.protobuf.gradle.id

plugins {
    id("com.android.application")
    id("kotlin-parcelize")
    id("com.google.protobuf")
}
apply {
    plugin("kotlin-android")
}

android {
    compileSdk = 37

    defaultConfig {
        applicationId = "com.kieronquinn.app.pixelambientmusic"
        minSdk = 29
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        prefab = true
    }
    buildFeatures {
        aidl = true
        buildConfig = true
    }
    packaging {
        jniLibs.useLegacyPackaging = true
        resources.merges += setOf("META-INF/LICENSE.md", "META-INF/NOTICE.md")
    }
    testOptions { unitTests.isIncludeAndroidResources = true }
    namespace = "com.kieronquinn.app.pixelambientmusic"
}

dependencies {
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.17")
    //Refer to code from system stubs + manifest code stubs, but don't include in APK
    compileOnly(project(mapOf("path" to ":systemstubs")))
    implementation(project(mapOf("path" to ":leveldb")))
    implementation("androidx.core:core:1.19.1")
    implementation("org.lsposed.hiddenapibypass:hiddenapibypass:6.1")
    implementation("com.aliucord:Aliuhook:1.1.4")
    implementation("top.canyie.pine:core:0.3.0")
    implementation("com.google.protobuf:protobuf-javalite:4.36.2")
    implementation("com.google.code.gson:gson:2.14.0")
    implementation("com.squareup.picasso:picasso:2.71828")
    implementation("org.bouncycastle:bcpkix-jdk18on:1.86")
    implementation("org.bouncycastle:bcprov-jdk18on:1.86")
}

protobuf {
    protoc { artifact = "com.google.protobuf:protoc:4.36.2" }
    generateProtoTasks {
        all().forEach { task ->
            task.builtins { id("java") { option("lite") } }
        }
    }
}

// Exercise database counting within the heap budget of the affected physical device.
tasks.withType<Test>().configureEach { maxHeapSize = "256m" }
