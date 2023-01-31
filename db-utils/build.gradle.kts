import java.util.Base64
import kotlin.text.String
/*
 * Copyright 2020 IceRock MAG Inc. Use of this source code is governed by the Apache 2.0 license.
 */

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("kotlin-kapt")
    id("maven-publish")
    id("java-library")
    id("signing")
}

apply(plugin = "kotlin")

group = "com.icerockdev"
version = "0.4.0"

val sourcesJar by tasks.registering(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

dependencies {
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
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${properties["junit_version"]}")
    testImplementation("io.zonky.test:embedded-postgres:${properties["embedded_postgres_version"]}")
    testImplementation(
        enforcedPlatform(
            "io.zonky.test.postgres:embedded-postgres-binaries-bom:${properties["embedded_postgres_binaries_version"]}"
        )
    )
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
    withJavadocJar()
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_11.toString()
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
    repositories.maven("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/") {
        name = "OSSRH"

        credentials {
            username = System.getenv("OSSRH_USER")
            password = System.getenv("OSSRH_KEY")
        }
    }
    publications {
        register("mavenJava", MavenPublication::class) {
            from(components["java"])
            artifact(sourcesJar.get())
            pom {
                name.set("Database tools")
                description.set("Database tools")
                url.set("https://github.com/icerockdev/db-utils")
                licenses {
                    license {
                        url.set("https://github.com/icerockdev/db-utils/blob/master/LICENSE.md")
                    }
                }

                developers {
                    developer {
                        id.set("YokiToki")
                        name.set("Stanislav")
                        email.set("skarakovski@icerockdev.com")
                    }

                    developer {
                        id.set("AlexeiiShvedov")
                        name.set("Alex Shvedov")
                        email.set("ashvedov@icerockdev.com")
                    }

                    developer {
                        id.set("oyakovlev")
                        name.set("Oleg Yakovlev")
                        email.set("oyakovlev@icerockdev.com")
                    }
                }

                scm {
                    connection.set("scm:git:ssh://github.com/icerockdev/db-utils.git")
                    developerConnection.set("scm:git:ssh://github.com/icerockdev/db-utils.git")
                    url.set("https://github.com/icerockdev/db-utils")
                }
            }
        }

        signing {
            setRequired({!properties.containsKey("libraryPublishToMavenLocal")})
            val signingKeyId: String? = System.getenv("SIGNING_KEY_ID")
            val signingPassword: String? = System.getenv("SIGNING_PASSWORD")
            val signingKey: String? = System.getenv("SIGNING_KEY")?.let { base64Key ->
                String(Base64.getDecoder().decode(base64Key))
            }
            useInMemoryPgpKeys(signingKeyId, signingKey, signingPassword)
            sign(publishing.publications["mavenJava"])
        }
    }
}
