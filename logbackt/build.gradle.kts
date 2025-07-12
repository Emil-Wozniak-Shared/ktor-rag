plugins {
    base
    alias(libs.plugins.kotlin.jvm)
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))
    implementation(kotlin("script-runtime"))
    implementation(kotlin("scripting-jsr223"))
    implementation(libs.logback.classic)
    implementation(libs.logback.core)
}
