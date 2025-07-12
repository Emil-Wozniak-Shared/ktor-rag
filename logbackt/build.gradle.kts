plugins {
    alias(libs.plugins.kotlin.jvm)
    java
}

group = "pl"
version = "0.0.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.logback.classic)
    implementation(libs.logback.core)
}
