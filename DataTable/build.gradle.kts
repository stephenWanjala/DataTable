plugins {
    alias(libs.plugins.jetbrainsKotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.vanniktech.mavenPublish)
    id("java-library")

}
group = "io.github.stephenwanjala"
version = "0.2.0"
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

dependencies {
    // `api`, not `implementation`: the public API exposes Compose types (@Composable lambdas,
    // Modifier, TextStyle, Dp), so consumers need these on their compile classpath.
    api(compose.runtime)
    api(compose.foundation)
    api(compose.ui)

    // Desktop-only internals (VerticalScrollbar, HorizontalScrollbar, AWT cursors).
    implementation(compose.desktop.common)
}
