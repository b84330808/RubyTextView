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
    testImplementation(libs.kotlin.test)
    testImplementation(libs.junit4)
}

tasks.test {
    useJUnit()
    maxHeapSize = "1g"
}

// The reading tables are CC BY-SA 4.0: ship the notices inside the jar.
tasks.jar {
    into("META-INF/rubytext-zh") {
        from("NOTICE.md")
        from("licenses")
    }
}

mavenPublishing {
    publishToMavenCentral()
    if (providers.gradleProperty("signingInMemoryKey").isPresent) {
        signAllPublications()
    }
    pom {
        licenses {
            license {
                name.set("Creative Commons Attribution-ShareAlike 4.0 International (reading tables)")
                url.set("https://creativecommons.org/licenses/by-sa/4.0/")
                distribution.set("repo")
            }
        }
    }
}
