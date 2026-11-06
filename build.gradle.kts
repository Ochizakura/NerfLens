
plugins {
    id("com.gtnewhorizons.gtnhconvention")
}

version = "0.2.0-rc2"

tasks.withType<Jar>().configureEach {
    includeEmptyDirs = false
}

val compatibilityCheck = tasks.register<JavaExec>("compatibilityCheck") {
    dependsOn(tasks.named("testClasses"))
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("nerflens.NerfCompatibilityChecks")
    args(layout.buildDirectory.dir("compatibility-check").get().asFile.absolutePath)
    workingDir(layout.buildDirectory.dir("compatibility-check").get().asFile)
    doFirst {
        workingDir.mkdirs()
    }
    javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(17))
    })
}

tasks.named("check") {
    dependsOn(compatibilityCheck)
}

// Verification is a standalone Java entry point rather than a JUnit suite.
tasks.named<Test>("test") {
    failOnNoDiscoveredTests = false
}
