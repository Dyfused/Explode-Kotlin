plugins {
    id("java")
    kotlin("jvm")
    application
    id("com.github.johnrengelman.shadow")
}

group = "taskeren.explode"
version = "1.0-SNAPSHOT"

val logbackVersion: String by project

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":dataprovider"))

    implementation("com.github.taskeren", "tconfig", "1.0")

    implementation("cn.hutool", "hutool-core", "5.8.4")
    implementation("ch.qos.logback", "logback-classic", logbackVersion)

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.2")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

application {
    mainClass.set("explode.rena.RenaAppKt")
}