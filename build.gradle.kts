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
    kotlin("jvm") version "1.7.0"
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

allprojects {
    repositories {
        mavenCentral()
        maven("https://jitpack.io")
    }
}

group = "taskeren"
version = "1.0-SNAPSHOT"
