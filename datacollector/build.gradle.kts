plugins {
	kotlin("jvm")
	kotlin("plugin.serialization") version "1.7.0"

}

version = "1.0"

dependencies {
	implementation("org.jetbrains.kotlinx", "kotlinx-serialization-json", "1.3.2")

	testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.2")
	testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.2")
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