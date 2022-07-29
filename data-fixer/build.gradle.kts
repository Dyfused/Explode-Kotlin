plugins {
    kotlin("jvm")
    kotlin("plugin.serialization") version "1.7.0"
    id("application")
    id("com.github.johnrengelman.shadow")
}

group = "explode"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":dataprovider"))

    implementation("org.litote.kmongo", "kmongo-serialization", "4.6.1")
    implementation("org.jetbrains.kotlinx", "kotlinx-serialization-json", "1.3.2")
    implementation("com.github.taskeren", "tconfig", "1.0")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.2")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

application {
    mainClass.set("explode.datafixer.App")
}