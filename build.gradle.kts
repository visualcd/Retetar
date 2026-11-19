plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidMultiplatformLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinSerialization) apply false
    alias(libs.plugins.ktor) apply false
    alias(libs.plugins.sqldelight) apply false
}

allprojects {
    val wasmLock = layout.buildDirectory.file("wasm/package-lock.json").get().asFile
    if (!wasmLock.exists()) {
        wasmLock.parentFile?.mkdirs()
        wasmLock.writeText("{}")
    }
    val jsLock = layout.buildDirectory.file("js/package-lock.json").get().asFile
    if (!jsLock.exists()) {
        jsLock.parentFile?.mkdirs()
        jsLock.writeText("{}")
    }

    tasks.matching { task ->
        task.name.endsWith("UpgradePackageLock") || task.javaClass.name.endsWith("LockStoreTask")
    }.configureEach {
        try {
            val method = (this as Any)::class.java.methods.firstOrNull { m -> m.name == "getInputFile" }
            val inputFileProp = method?.invoke(this)
            val file = when (inputFileProp) {
                is org.gradle.api.file.RegularFileProperty -> inputFileProp.orNull?.asFile
                is java.io.File -> inputFileProp
                else -> null
            }
            file?.let { f ->
                if (!f.exists()) {
                    f.parentFile?.mkdirs()
                    f.writeText("{}")
                }
            }
        } catch (_: Exception) {
            // ignore reflection errors
        }
    }
}