plugins {
    alias(libs.plugins.android.application)
}



android {
    namespace = "com.saif.jobnet"
    compileSdk = 34

    buildFeatures{
        viewBinding=true
    }
    defaultConfig {
        applicationId = "com.saif.jobnet"
        minSdk = 26
        targetSdk = 34
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.annotation)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation (libs.retrofit)
    implementation (libs.converter.gson)
    implementation (libs.room.runtime)
    annotationProcessor (libs.room.compiler)
    implementation (libs.shimmer)
    implementation (libs.poi)
    implementation (libs.poi.ooxml)
    implementation (libs.itextpdf)
    implementation(libs.android.image.cropper)


}