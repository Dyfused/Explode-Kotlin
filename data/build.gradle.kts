plugins {
    kotlin("jvm")
    kotlin("plugin.serialization") version "1.7.0"
    `maven-publish`
}

group = "explode"
version = "1.2.1"

val logbackVersion: String by project

dependencies {
    implementation(project(":pack"))

    implementation("org.jetbrains.kotlinx", "kotlinx-serialization-json", "1.3.2")
    implementation("ch.qos.logback", "logback-classic", logbackVersion)
    implementation("org.litote.kmongo", "kmongo-serialization", "4.6.1")
    implementation("com.github.taskeren", "tconfig", "1.0")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.0")
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

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "com.github.dyfused"
            artifactId = "explode-data"
            version = "1.2"

            from(components["java"])
        }
    }
}