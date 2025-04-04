plugins {
    id("com.diffplug.spotless") version "6.25.0"
    id("maven-publish")
    id("signing")
}

subprojects.forEach { subProj ->
    subProj.apply(plugin = "java")
    subProj.apply(plugin = "com.diffplug.spotless")
    subProj.apply(plugin = "maven-publish")
    subProj.apply(plugin = "signing")

    subProj.group = "gg.rohan.delegasm"
    subProj.version = "0.2.0-SNAPSHOT"

    subProj.repositories {
        mavenCentral()
    }

    subProj.tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release.set(8)
    }

    subProj.spotless {
        java {
            licenseHeader(rootDir.resolve("license-header.txt").readText())
            toggleOffOn()
        }
    }

    if (!subProj.hasProperty("shouldPublish") || subProj.findProperty("shouldPublish") == "true") {
        subProj.publishing {
            publications {
                create<MavenPublication>("maven") {
                    from(subProj.components["java"])

                    pom {
                        name.set("Delegasm")
                        description.set("A library for creating delegate classes at runtime")
                        url.set("https://github.com/yourusername/delegasm")

                        licenses {
                            license {
                                name.set("MIT License")
                                url.set("https://mit-license.org/")
                            }
                        }

                        developers {
                            developer {
                                id.set("AV3RG")
                                name.set("Rohan Goyal")
                                email.set("delegasm@rohan.gg")
                            }
                        }

                        scm {
                            connection.set("scm:git:git://github.com/AV3RG/delegasm.git")
                            developerConnection.set("scm:git:ssh://github.com/AV3RG/delegasm.git")
                            url.set("https://github.com/AV3RG/delegasm")
                        }
                    }
                }
            }

            repositories {
                maven {
                    val releasesRepoUrl = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
                    val snapshotsRepoUrl = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
                    url = if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl

                    credentials {
                        username = project.properties.get("ossrhUsername") as String?
                        password = project.properties.get("ossrhPassword") as String?
                    }
                }
            }
        }

        subProj.signing {
            useGpgCmd()
            sign(subProj.publishing.publications["maven"])
        }
    }
}