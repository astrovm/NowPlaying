plugins {
    id("com.android.library")
}

android {
    compileSdk = 37

    defaultConfig {
        minSdk = 29

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
    namespace = "com.kieronquinn.app.leveldb"
}

dependencies {
    api("com.google.guava:guava:33.7.2-android")
    api("org.iq80.snappy:snappy:0.4")
    api("org.xerial.snappy:snappy-java:1.1.10.11")
}