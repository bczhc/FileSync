@file:Suppress("UnstableApiUsage")

import pers.zhc.gradle.plugins.ndk.rust.RustBuildPlugin
import pers.zhc.gradle.plugins.ndk.rust.RustBuildPlugin.RustBuildPluginExtension
import java.util.*

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

apply<RustBuildPlugin>()

android {
    namespace = "pers.zhc.android.filesync"
    compileSdk = 34

    defaultConfig {
        applicationId = "pers.zhc.android.filesync"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        asMap["debug"]!!.apply {
            storeFile = file("test.jks")
            storePassword = "123456"
            keyAlias = "key0"
            keyPassword = "123456"
        }
    }

    buildTypes {
        named("release") {
            isMinifyEnabled = false
            setProguardFiles(listOf(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"))
        }
    }
    buildTypes {
        val types = asMap
        types["debug"]!!.apply {
            isMinifyEnabled = false
            isShrinkResources = false
            isDebuggable = true
            isJniDebuggable = true
            signingConfig = signingConfigs["debug"]
        }
        types["release"]!!.apply {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles("proguard-rules.pro")
            isDebuggable = true
            isJniDebuggable = true
            signingConfig = signingConfigs["debug"]
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true
    }

    sourceSets {
        val sets = asMap
        sets["main"]!!.apply {
            jniLibs.srcDirs("jniLibs")
        }
    }
}

val configFile = File(rootProject.projectDir, "config.properties")
if (!configFile.exists()) {
    throw GradleException("config.properties not exists")
}
val configs = Properties().apply {
    load(configFile.reader())
}
val ndkBuildType = configs["ndk.buildType"] as String? ?: "debug"
val ndkTargets = (configs["ndk.target"] ?: throw GradleException("ndk.target missing")) as String
val ndkTargetsConfig = ndkTargets.split(',').map {
    val groupValues = Regex("^(.*)-([0-9]+)\$").findAll(it).first().groupValues
    mapOf(
        Pair("abi", groupValues[1]),
        Pair("api", groupValues[2].toInt())
    )
}

val jniOutputDir = file("jniLibs").also { it.mkdir() }

configure<RustBuildPluginExtension> {
    srcDir.set("$projectDir/src/main/rust")
    ndkDir.set(android.ndkDirectory.path)
    targets.set(ndkTargetsConfig)
    buildType.set(ndkBuildType)
    outputDir.set(jniOutputDir.path)
}

val compileRustTask = tasks.findByName(RustBuildPlugin.TASK_NAME())!!
project.tasks.getByName("preBuild").dependsOn(compileRustTask)

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.10.0")
    implementation("com.google.code.gson:gson:2.10.1")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
