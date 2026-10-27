import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.roborazzi)
    alias(libs.plugins.maven.publish)
}

android {
    namespace = "me.weilunli.rubytext.compose"
    compileSdk = 36

    defaultConfig {
        minSdk = 21
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all { it.systemProperty("robolectric.graphicsMode", "NATIVE") }
        }
    }
}

androidComponents {
    // Compose UI tests need ui-test-manifest, which only the debug variant has.
    beforeVariants(selector().withBuildType("release")) { it.enableUnitTest = false }
}

kotlin {
    explicitApi()
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

composeCompiler {
    // RubyString and friends live in the plain-Kotlin core but are immutable.
    stabilityConfigurationFiles.add(layout.projectDirectory.file("compose-stability.conf"))
}

roborazzi {
    // Golden images live next to the tests once recorded (./gradlew recordRoborazziDebug).
    outputDir.set(layout.projectDirectory.dir("src/test/screenshots"))
}

dependencies {
    api(project(":rubytext-core"))
    api(libs.compose.runtime)
    api(libs.compose.ui)
    api(libs.compose.ui.text)

    testImplementation(libs.junit4)
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.roborazzi.junit.rule)
    testImplementation(libs.compose.ui.test.junit4)
    testImplementation(libs.compose.foundation)
    debugImplementation(libs.compose.ui.test.manifest)
}

mavenPublishing {
    publishToMavenCentral()
    if (providers.gradleProperty("signingInMemoryKey").isPresent) {
        signAllPublications()
    }
}
