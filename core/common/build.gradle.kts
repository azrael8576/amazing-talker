plugins {
    id("at.android.library")
    id("at.android.library.compose")
    id("at.android.hilt")
}

android {
    namespace = "com.wei.amazingtalker.core.common"

    defaultConfig {
        testInstrumentationRunner = "com.wei.amazingtalker.core.testing.AtTestRunner"
    }
}

dependencies {
    implementation(project(":core:model"))

    // LifeCycle
    implementation(libs.androidx.lifecycle.runtimeCompose)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
}