plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val uploadKey = providers.environmentVariable("FISH_UPLOAD_KEYSTORE").orNull
val uploadAlias = providers.environmentVariable("FISH_UPLOAD_ALIAS").orNull
val uploadPassword = providers.environmentVariable("FISH_UPLOAD_PASSWORD").orNull
android {
    namespace = "com.deploydulupulangnanti.tekatekigambarikan"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.deploydulupulangnanti.tekatekigambarikan"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
    }

    if (uploadKey != null && uploadAlias != null && uploadPassword != null) {
        signingConfigs.create("upload") {
            storeFile = rootProject.file(uploadKey)
            storePassword = uploadPassword
            keyAlias = uploadAlias
            keyPassword = uploadPassword
        }
        buildTypes.named("release") {
            signingConfig = signingConfigs.getByName("upload")
        }
    }

    buildFeatures { compose = true }
    androidResources { noCompress += "ogg" }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.10.00"))
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.4")
}
