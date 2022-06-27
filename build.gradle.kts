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

allprojects {
    repositories {
        mavenCentral()
        maven("https://jitpack.io")
    }
}

group = "taskeren"
version = "1.0-SNAPSHOT"
