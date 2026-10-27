import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.maven.publish)
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

kotlin {
    explicitApi()
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

dependencies {
    testImplementation(libs.kotlin.test)
    testImplementation(libs.junit4)
}

tasks.test {
    useJUnit()
}

// Renders the layout engine's output with java.awt into build/previews/*.png,
// so the geometry can be checked by eye without Android. Needs a CJK font
// with Bopomofo (Microsoft JhengHei, Noto Sans CJK, PingFang ...).
tasks.register<JavaExec>("renderPreviews") {
    group = "verification"
    description = "Draws sample layouts to build/previews with java.awt."
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("me.weilunli.rubytext.layout.AwtPreviewKt")
    args(layout.buildDirectory.dir("previews").get().asFile.absolutePath)
    systemProperty("java.awt.headless", "true")
}

mavenPublishing {
    publishToMavenCentral()
    if (providers.gradleProperty("signingInMemoryKey").isPresent) {
        signAllPublications()
    }
}
