plugins {
    kotlin("jvm") version "1.7.0"
    kotlin("plugin.serialization") version "1.7.0"
}

val logbackVersion: String by project

dependencies {
    implementation("org.jetbrains.kotlinx", "kotlinx-serialization-json", "1.3.2")
    implementation("ch.qos.logback", "logback-classic", logbackVersion)
    implementation("org.litote.kmongo", "kmongo-serialization", "4.6.1")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
}

tasks.withType<JavaCompile> {
    sourceCompatibility = "1.8"
    targetCompatibility = "1.8"
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.test {
    useJUnitPlatform()
}