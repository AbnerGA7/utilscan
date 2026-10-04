plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.abnerga.utilscan"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.abnerga.utilscan"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    // Los modelos .tflite se mapean en memoria directamente desde el APK: no deben comprimirse.
    androidResources {
        noCompress += "tflite"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.coroutines.android)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    implementation(libs.litert)
    implementation(libs.litert.gpu)
    implementation(libs.litert.gpu.api)

    testImplementation(libs.junit)
}

// ---------------------------------------------------------------------------------------------
// Descarga el modelo pre-entrenado (publicado como release por .github/workflows/export-model.yml)
// a src/main/assets/models si todavía no está. Así el repositorio no guarda binarios pesados.
// ---------------------------------------------------------------------------------------------
val modelsDir = layout.projectDirectory.dir("src/main/assets/models")
val releasesUrl = "https://github.com/AbnerGA7/utilscan/releases/download"

/** (tag del release, archivo, obligatorio) */
val modelFiles = listOf(
    Triple("model-school_supplies", "school_supplies.tflite", false),
    Triple("model-school_supplies", "school_supplies_labels.txt", false),
    Triple("model-yolov8s_oiv7", "yolov8s_oiv7.tflite", true),
    Triple("model-yolov8s_oiv7", "yolov8s_oiv7_labels.txt", true),
)

val downloadModels by tasks.registering {
    group = "utilscan"
    description = "Descarga los modelos .tflite publicados en los releases del repositorio"
    outputs.files(modelFiles.map { modelsDir.file(it.second) })
    doLast {
        modelsDir.asFile.mkdirs()
        modelFiles.forEach { (tag, name, required) ->
            val target = modelsDir.file(name).asFile
            if (target.exists() && target.length() > 0) return@forEach
            logger.lifecycle("Descargando $name ...")
            val tmp = File(target.parentFile, "$name.part")
            try {
                uri("$releasesUrl/$tag/$name").toURL().openStream().use { input ->
                    tmp.outputStream().use { output -> input.copyTo(output) }
                }
                tmp.renameTo(target)
            } catch (e: java.io.IOException) {
                tmp.delete()
                if (required) throw e
                logger.warn("No se encontró $name (opcional): ${e.message}")
            }
        }
    }
}

tasks.named("preBuild") { dependsOn(downloadModels) }
