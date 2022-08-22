plugins {
	kotlin("jvm")
	application
	kotlin("plugin.serialization") version "1.7.0"
	id("com.github.johnrengelman.shadow")
}

group = "explode"
version = "1.0.0"

repositories {
	mavenCentral()
	maven("https://repo.mirai.mamoe.net/snapshots/")
	maven("https://jitpack.io")
}

val logbackVersion: String by project
val ktorVersion: String by project
val miraiVersion: String by project

dependencies {
	implementation(project(":data"))
	implementation(project(":mirai"))
	implementation(project(":server"))
}

tasks.test {
	useJUnitPlatform()
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
	kotlinOptions.jvmTarget = "1.8"
	kotlinOptions.freeCompilerArgs = listOf("-Xcontext-receivers")
}

application {
	mainClass.set("explode.omni.Omni")
}