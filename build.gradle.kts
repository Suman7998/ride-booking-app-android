// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    // Keeping empty; versions declared in app/build.gradle.kts
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}
