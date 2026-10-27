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
    api(project(":rubytext-core"))
    // Apache-2.0; bundles mecab-ipadic, which is under the NAIST/ICOT license (see NOTICE.md).
    api(libs.kuromoji.ipadic)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.junit4)
}

tasks.test {
    useJUnit()
    maxHeapSize = "1g"
}

tasks.jar {
    into("META-INF/rubytext-ja") {
        from("NOTICE.md")
    }
}

mavenPublishing {
    publishToMavenCentral()
    if (providers.gradleProperty("signingInMemoryKey").isPresent) {
        signAllPublications()
    }
}
