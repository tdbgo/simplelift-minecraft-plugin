import org.gradle.api.attributes.java.TargetJvmVersion

plugins {
    java
}

group = "com.playcity"
version = "0.3.3-SNAPSHOT"

val apiGroup: String by project
val apiArtifact: String by project
val apiVersion: String by project
val javaToolchainVersion: String by project
val javaRelease: String by project

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(javaToolchainVersion.toInt()))
    }
    withSourcesJar()
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
}

configurations.configureEach {
    if (isCanBeResolved) {
        attributes.attribute(
            TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE,
            javaToolchainVersion.toInt()
        )
    }
}

dependencies {
    compileOnly("$apiGroup:$apiArtifact:$apiVersion")

    testImplementation(platform("org.junit:junit-bom:5.13.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("$apiGroup:$apiArtifact:$apiVersion")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(javaRelease.toInt())
    // Cross-compiling a modern Paper API to Java 8 otherwise emits hundreds of
    // annotation/classfile warnings that do not describe plugin defects.
    options.compilerArgs.add("-Xlint:all,-options,-classfile")
}

tasks.processResources {
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand("version" to project.version)
    }
}

tasks.jar {
    archiveBaseName.set("SimpleLift")
    from("LICENSE", "THIRD_PARTY_NOTICES.md")
    manifest {
        attributes(
            "Implementation-Title" to project.name,
            "Implementation-Version" to project.version,
            "Built-Jdk-Spec" to javaRelease
        )
    }
}

tasks.named<Jar>("sourcesJar") {
    from("LICENSE", "THIRD_PARTY_NOTICES.md")
}

tasks.test {
    useJUnitPlatform()
}
