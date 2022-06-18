import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    repositories {
        mavenCentral()
        mavenLocal {
            content {
                includeGroup("com.expediagroup")
            }
        }
    }
}

plugins {
    kotlin("jvm") version "1.6.21"
    id("application")
//    id("com.expediagroup.graphql")
}

group = "taskeren"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val ktorVersion: String by project
val logbackVersion: String by project
val kotlinCoroutineVersion: String by project

dependencies {
    implementation("com.expediagroup", "graphql-kotlin-server", "6.0.0-alpha.4")
    implementation("com.graphql-java", "graphql-java-extended-scalars", "18.1")
    implementation("io.ktor", "ktor-server-core", ktorVersion)
    implementation("io.ktor", "ktor-server-netty", ktorVersion)
    implementation("ch.qos.logback", "logback-classic", logbackVersion)
    implementation("org.jetbrains.kotlinx", "kotlinx-coroutines-jdk8", kotlinCoroutineVersion)
    testImplementation(kotlin("test"))
    testImplementation("com.squareup.okhttp3:okhttp:4.10.0")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}