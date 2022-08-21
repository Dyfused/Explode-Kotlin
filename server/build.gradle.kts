import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization") version "1.7.0"
    id("application")
    id("com.github.johnrengelman.shadow")
    `maven-publish`
}

group = "taskeren.explode"
version = "1.4.0"

val ktorVersion: String by project
val logbackVersion: String by project
val kotlinCoroutineVersion: String by project

dependencies {
    implementation(project(":data"))

    implementation("com.expediagroup", "graphql-kotlin-server", "6.0.0-alpha.4")
    implementation("com.graphql-java", "graphql-java-extended-scalars", "18.1")
    implementation("org.jetbrains.kotlinx", "kotlinx-serialization-json", "1.3.2")
    implementation("io.ktor", "ktor-server-core", ktorVersion)
    implementation("io.ktor", "ktor-server-netty", ktorVersion)
    implementation("io.ktor", "ktor-server-cors", ktorVersion)
    implementation("io.ktor", "ktor-server-auth", ktorVersion)
    implementation("io.ktor", "ktor-server-status-pages", ktorVersion)
    implementation("io.ktor", "ktor-server-content-negotiation", ktorVersion)
    implementation("io.ktor", "ktor-serialization-kotlinx-json", ktorVersion)
    implementation("ch.qos.logback", "logback-classic", logbackVersion)
    implementation("org.jetbrains.kotlinx", "kotlinx-coroutines-jdk8", kotlinCoroutineVersion)
    implementation("org.jetbrains.kotlin", "kotlin-reflect", "1.7.0")
    implementation("com.github.taskeren", "tconfig", "1.0")
    implementation("org.litote.kmongo", "kmongo-serialization", "4.6.1")
    implementation("com.github.taskeren", "tconfig", "1.0")
    testImplementation(kotlin("test"))
    testImplementation("com.squareup.okhttp3:okhttp:4.10.0")
    // testImplementation("io.ktor", "ktor-server-test-host", ktorVersion)
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
    kotlinOptions.freeCompilerArgs = listOf("-Xcontext-receivers")
}

application {
    mainClass.set("explode.AppKt")
}

val gitHash: String by lazy {
    val stdout = ByteArrayOutputStream()
    exec {
        commandLine = listOf("git", "rev-parse", "--short", "HEAD")
        standardOutput = stdout
    }
    stdout.toString().trim()
}

tasks.jar {
    manifest {
        attributes["Built-By"] = System.getProperty("user.name")
        attributes["Built-Timestamp"] = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").format(Date())
        attributes["Built-Revision"] = gitHash
        attributes["Created-By"] = "Gradle ${gradle.gradleVersion}"
        attributes["Build-JDK"] = "${System.getProperty("java.version")} (${System.getProperty("java.vendor")})"
        attributes["Build-OS"] = "${System.getProperty("os.name")} ${System.getProperty("os.arch")} ${System.getProperty("os.version")}"
    }
}

tasks.processResources {
    filesMatching("**/explode.json") {
        expand("version" to gitHash)
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "com.github.dyfused"
            artifactId = "explode-server"
            version = "1.2"

            from(components["java"])
        }
    }
}