import dev.nucleusframework.desktop.application.dsl.CompressionLevel
import dev.nucleusframework.desktop.application.dsl.TargetFormat


plugins {
    alias(libs.plugins.nucleusframework)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.ui.toolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmTest.dependencies {
            // The gallery is the only place the samples are exercised, so it gets a smoke test:
            // a sample that renders empty is a bug the library's own tests cannot see.
            implementation(libs.compose.ui.testJunit4)
            implementation(compose.desktop.currentOs)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs){
                exclude("org.jetbrains.compose.material")
            }
            implementation(libs.nucleus.application)
            implementation(libs.decorated.window.tao)
            implementation(libs.material.icons.extended)
            implementation(libs.kotlinx.coroutinesSwing)
            api(project(":DataTable"))

        }
    }
}

tasks.withType<Test>().configureEach {
    // Compose desktop UI tests render offscreen; no display needed.
    systemProperty("java.awt.headless", "true")
    testLogging {
        events("failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

nucleus.application {
    mainClass = "io.github.stephenwanjala.composedatatable.MainKt"
    nativeDistributions {
        packageName = "composedatatableDemo"
        // CI stamps the release tag here (.github/workflows/demo-release.yml); local builds get 1.0.0.
        packageVersion = providers.gradleProperty("demoVersion").getOrElse("1.0.0")
        modules("java.instrument", "jdk.unsupported")
        cleanupNativeLibs = true
        compressionLevel = CompressionLevel.Maximum
        targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Tar, TargetFormat.Deb)

        // electron-builder refuses to build a .deb without homepage + maintainer.
        description = "Demo application for the Compose DataTable component"
        vendor = "Wanjala Stephen"
        copyright = "© 2025 Wanjala Stephen. Licensed under the Apache License 2.0."
        homepage = "https://github.com/stephenWanjala/DataTable"

        linux {
            debMaintainer = "Wanjala Stephen <stephenwanjala145@gmail.com>"
            appCategory = "Development"
        }
    }
}

