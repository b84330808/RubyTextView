pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "RubyTextView"

// Pure Kotlin: parser, Zhuyin/Pinyin helpers and the layout engine. Builds anywhere.
include(":rubytext-core")
// Optional: offline Chinese to Zhuyin/Pinyin (reading tables are CC BY-SA 4.0).
include(":rubytext-zh")
// Optional: offline Japanese furigana with kuromoji (mecab-ipadic, NAIST license).
include(":rubytext-ja")

// The Compose library and the sample app need the Android SDK. Without one,
// only :rubytext-core is configured so its tests still run.
val androidSdkDir: File? = run {
    val props = java.util.Properties()
    val localProps = file("local.properties")
    if (localProps.isFile) localProps.inputStream().use { props.load(it) }
    val path = props.getProperty("sdk.dir")
        ?: System.getenv("ANDROID_HOME")
        ?: System.getenv("ANDROID_SDK_ROOT")
    path?.let(::File)?.takeIf { it.isDirectory }
}

if (androidSdkDir != null) {
    include(":rubytext-compose", ":sample")
} else {
    println(
        "w: Android SDK not found (set sdk.dir in local.properties or ANDROID_HOME). " +
            "Only the pure-Kotlin modules are included in this build."
    )
}
