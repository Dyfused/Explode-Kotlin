plugins {
    kotlin("jvm")
    application
    kotlin("plugin.serialization") version "1.7.0"
    id("com.github.johnrengelman.shadow")
}

val miraiVersion: String by project
val logbackVersion: String by project

group = "explode"
version = "1.0.1"

repositories {
    mavenCentral()
    maven("https://repo.mirai.mamoe.net/snapshots/")
}

dependencies {
    implementation(project(":data"))
    implementation("org.litote.kmongo", "kmongo-serialization", "4.6.1")

    implementation("org.jetbrains.kotlinx", "kotlinx-coroutines-jdk8", "1.6.2")
    implementation("org.jetbrains.kotlinx", "kotlinx-serialization-json", "1.3.2")

    implementation("ch.qos.logback", "logback-classic", logbackVersion)

    implementation("net.mamoe:mirai-core:$miraiVersion")
    implementation("net.mamoe:mirai-console:$miraiVersion")

    api("net.mamoe:mirai-core:$miraiVersion")
    api("net.mamoe:mirai-console-terminal:$miraiVersion")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.0")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

application {
    mainClass.set("explode.mirai.MiraiMainKt")
}