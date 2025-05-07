import com.vanniktech.maven.publish.SonatypeHost

plugins {
    id("com.diffplug.spotless") version "6.25.0"
    id("com.vanniktech.maven.publish") version "0.31.0"
    id("signing")
}

subprojects.forEach { subProj ->
    subProj.apply(plugin = "java")
    subProj.apply(plugin = "com.diffplug.spotless")
    subProj.apply(plugin = "com.vanniktech.maven.publish")
    subProj.apply(plugin = "signing")

    subProj.group = "gg.rohan.delegasm"
    subProj.version = "0.2.0-SNAPSHOT"

    subProj.repositories {
        mavenCentral()
    }

    subProj.extensions.configure<JavaPluginExtension>("java") {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    subProj.tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
    }

    subProj.spotless {
        java {
            licenseHeader(rootDir.resolve("license-header.txt").readText())
            toggleOffOn()
        }
    }

    if (!subProj.hasProperty("shouldPublish") || subProj.findProperty("shouldPublish") == "true") {
        subProj.mavenPublishing {
            publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
            signAllPublications()

            coordinates(subProj.group as String, subProj.name, subProj.version as String)

            pom {
                name.set("Delegasm")
                description.set("An annotation processor to create classes that are delegated by other classes.")
                inceptionYear.set("2025")
                url.set("https://github.com/AV3RG/delegasm")

                licenses {
                    license {
                        name.set("The MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                        distribution.set("https://opensource.org/licenses/MIT")
                    }
                }
                developers {
                    developer {
                        id.set("AV3RG")
                        name.set("Rohan")
                        url.set("https://github.com/AV3RG")
                    }
                }
                scm {
                    url.set("https://github.com/AV3RG/delegasm")
                    connection.set("scm:git:git://github.com/AV3RG/delegasm.git")
                    developerConnection.set("scm:git:git://github.com/AV3RG/delegasm.git")
                }
            }
        }

        subProj.signing {
            useGpgCmd()
        }
    }
}