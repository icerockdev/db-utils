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
version = "0.1.0"

val sourcesJar by tasks.registering(Jar::class) {
    classifier = "sources"
    from(sourceSets.main.get().allSource)
}

dependencies {
    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:${properties["kotlin_version"]}")
    // Logging
    implementation("ch.qos.logback:logback-classic:${properties["logback_version"]}")
    // DB
    api("org.jetbrains.exposed:exposed:${properties["exposed_version"]}")
    implementation("com.zaxxer:HikariCP:${properties["hikari_cp_version"]}")
    // JSON support
    implementation("com.fasterxml.jackson.core:jackson-core:${properties["jackson_version"]}")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:${properties["jackson_version"]}")
    // PostgreSQL + PostGIS
    implementation("org.postgresql:postgresql:${properties["postgres_version"]}")
    implementation("net.postgis:postgis-jdbc:${properties["postgis_version"]}")
    // Raw SQL
    implementation("ca.krasnay:sqlbuilder:${properties["sqlbuilder_version"]}")
    // Tests
    testImplementation("org.junit.jupiter:junit-jupiter-api:${properties["junit_version"]}")
    testRuntime("org.junit.jupiter:junit-jupiter-engine:${properties["junit_version"]}")
    testImplementation("io.zonky.test:embedded-postgres:${properties["embedded_postgres_version"]}")
    testImplementation(enforcedPlatform("io.zonky.test.postgres:embedded-postgres-binaries-bom:${properties["embedded_postgres_binaries_version"]}"))
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

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
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
