plugins {
    alias(libs.plugins.jetbrainsKotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.vanniktech.mavenPublish)
    alias(libs.plugins.dokka)
    id("java-library")

}
group = "io.github.stephenwanjala"
version = "0.3.0"
java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}
mavenPublishing {
    publishToMavenCentral()

    signAllPublications()

    coordinates(group.toString(), "datatable", version.toString())

    pom {
        name = "Data Table Component For Compose UI"
        description =
            "A highly customizable, feature-rich DataTable component for Jetpack Compose Desktop  with smooth scrolling, advanced interactions, and extensive customization options."
        inceptionYear = "2025"
        url = "https://github.com/stephenWanjala/DataTable/"
        licenses {
            license {
                name = "The Apache License, Version 2.0"
                url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                distribution = "https://www.apache.org/licenses/LICENSE-2.0.txt"
            }
        }
        developers {
            developer {
                id = "stephenWanjala"
                name = "Wanjala Stephen"
                url = "https://github.com/stephenWanjala/"
            }
        }
        scm {
            url = " https://github.com/stephenWanjala/DataTable/"
            connection = "scm:git:git:/github.com/stephenWanjala/DataTable.git"
            developerConnection = "scm:git:ssh://git@github.com/stephenWanjala/DataTable.git"
        }
    }
}
kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11
    }
}

dokka {
    moduleName = "DataTable"
    dokkaSourceSets.main {
        // Internal helpers are an implementation detail; only document the public surface.
        documentedVisibilities.set(
            setOf(org.jetbrains.dokka.gradle.engine.parameters.VisibilityModifier.Public)
        )
        reportUndocumented = true
        skipDeprecated = false

        sourceLink {
            localDirectory = file("src/main/java")
            remoteUrl("https://github.com/stephenWanjala/DataTable/tree/master/DataTable/src/main/java")
            remoteLineSuffix = "#L"
        }
    }
    dokkaPublications.html {
        // MkDocs mounts this under /api, so keep the output beside the built site.
        outputDirectory = rootProject.layout.projectDirectory.dir("build/dokka")
    }
}

dependencies {
    // `api`, not `implementation`: the public API exposes Compose types (@Composable lambdas,
    // Modifier, TextStyle, Dp), so consumers need these on their compile classpath.
    api(compose.runtime)
    api(compose.foundation)
    api(compose.ui)

    // Desktop-only internals (VerticalScrollbar, HorizontalScrollbar, AWT cursors).
    implementation(compose.desktop.common)
}
