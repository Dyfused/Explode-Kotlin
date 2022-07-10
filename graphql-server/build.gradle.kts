plugins {
    kotlin("jvm")
    kotlin("plugin.serialization") version "1.7.0"
    id("application")
    id("com.github.johnrengelman.shadow")
}

group = "taskeren.explode"
version = "1.0-SNAPSHOT"

val ktorVersion: String by project
val logbackVersion: String by project
val kotlinCoroutineVersion: String by project

dependencies {
    implementation(project(":dataprovider"))

    implementation("com.expediagroup", "graphql-kotlin-server", "6.0.0-alpha.4")
    implementation("com.graphql-java", "graphql-java-extended-scalars", "18.1")
    implementation("org.jetbrains.kotlinx", "kotlinx-serialization-json", "1.3.2")
    implementation("io.ktor", "ktor-server-core", ktorVersion)
    implementation("io.ktor", "ktor-server-netty", ktorVersion)
    implementation("io.ktor", "ktor-server-cors", ktorVersion)
    implementation("ch.qos.logback", "logback-classic", logbackVersion)
    implementation("org.jetbrains.kotlinx", "kotlinx-coroutines-jdk8", kotlinCoroutineVersion)
    implementation("org.jetbrains.kotlin", "kotlin-reflect", "1.7.0")
    implementation("com.github.taskeren", "tconfig", "1.0")
    implementation("org.litote.kmongo", "kmongo-serialization", "4.6.1")
    implementation("com.github.taskeren", "tconfig", "1.0")
    testImplementation(kotlin("test"))
    testImplementation("com.squareup.okhttp3:okhttp:4.10.0")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

application {
    mainClass.set("explode.AppKt")
}