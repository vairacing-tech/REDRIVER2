plugins {
    id("com.android.application")
}

val releaseSigningFile = file("${System.getProperty("user.home")}/.android/redriver2-release-signing.txt")
val releaseSigning = if (releaseSigningFile.isFile) {
    releaseSigningFile.readLines()
        .mapNotNull { line ->
            val separator = line.indexOf('=')
            if (separator <= 0) {
                null
            } else {
                line.substring(0, separator) to line.substring(separator + 1)
            }
        }
        .toMap()
} else {
    emptyMap()
}

android {
    namespace = "tech.vairacing.redriver2"
    compileSdk = 36
    ndkVersion = "27.3.13750724"

    defaultConfig {
        applicationId = "tech.vairacing.redriver2"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"

        ndk {
            abiFilters += "arm64-v8a"
        }

        externalNativeBuild {
            cmake {
                arguments += listOf(
                    "-DANDROID_STL=c++_shared",
                    "-DANDROID_PLATFORM=android-26"
                )
            }
        }
    }

    sourceSets {
        getByName("main") {
            java.setSrcDirs(listOf(
                "src/main/java",
                "../third_party/SDL/android-project/app/src/main/java"
            ))
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
        }
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }

    signingConfigs {
        if (releaseSigningFile.isFile) {
            create("release") {
                storeFile = file(releaseSigning.getValue("keystore"))
                storePassword = releaseSigning.getValue("storePassword")
                keyAlias = releaseSigning.getValue("alias")
                keyPassword = releaseSigning.getValue("keyPassword")
            }
        }
    }

    buildTypes {
        if (releaseSigningFile.isFile) {
            getByName("release") {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
}
