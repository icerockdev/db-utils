/*
 * Copyright 2020 IceRock MAG Inc. Use of this source code is governed by the Apache 2.0 license.
 */

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("kotlin-kapt")
    id("maven-publish")
    id("java-library")
}

apply(plugin = "kotlin")

group = "com.icerockdev"
version = "0.0.3"

val sourcesJar by tasks.registering(Jar::class) {
    classifier = "sources"
    from(sourceSets.main.get().allSource)
}

dependencies {
    // kotlin
    implementation(group = "org.jetbrains.kotlin", name = "kotlin-stdlib-jdk8", version = properties["kotlin_version"].toString())
    // logging
    implementation(group = "ch.qos.logback", name = "logback-classic", version = properties["logback_version"].toString())

    // DB
    api(group = "org.jetbrains.exposed", name = "exposed", version = properties["exposed_version"].toString())
    implementation(group = "com.zaxxer", name = "HikariCP", version = properties["hikari_cp_version"].toString())

    // JSON support
    implementation(group = "com.fasterxml.jackson.core", name = "jackson-core", version = properties["jackson_version"].toString())
    implementation(group = "com.fasterxml.jackson.module", name = "jackson-module-kotlin", version = properties["jackson_version"].toString())


    // postgres + postgis
    implementation(group = "org.postgresql", name = "postgresql", version = properties["postgres_version"].toString())
    implementation(group = "net.postgis", name = "postgis-jdbc", version = properties["postgis_version"].toString())
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

repositories {
    mavenCentral()
}

publishing {
    repositories.maven("https://api.bintray.com/maven/icerockdev/backend/db-utils/;publish=1") {
        name = "bintray"

        credentials {
            username = System.getProperty("BINTRAY_USER")
            password = System.getProperty("BINTRAY_KEY")
        }
    }
    publications {
        register("mavenJava", MavenPublication::class) {
            from(components["java"])
            artifact(sourcesJar.get())
        }
    }
}
