apply(plugin = "maven-publish")

project.group = providers.gradleProperty("pulse.group").get() as String
project.version = rootProject.extra["pulseVersion"] as String

project.afterEvaluate {
	the<PublishingExtension>().apply {
		repositories {
			// Local Maven for snapshots: ./gradlew publishToMavenLocal
			mavenLocal()

			// Remote Maven for releases: ./gradlew publish
			// Configure credentials via gradle.properties or environment variables:
			//   pulse.maven.url, pulse.maven.username, pulse.maven.password
			val mavenUrl = rootProject.findProperty("pulse.maven.url") as? String
			if (mavenUrl != null) {
				maven {
					name = "remote"
					url = uri(mavenUrl)
					credentials {
						username = rootProject.findProperty("pulse.maven.username") as? String
							?: System.getenv("PULSE_MAVEN_USERNAME") ?: ""
						password = rootProject.findProperty("pulse.maven.password") as? String
							?: System.getenv("PULSE_MAVEN_PASSWORD") ?: ""
					}
				}
			}
		}

		publications.withType<MavenPublication>().configureEach {
			pom {
				name.set(project.name)
				description.set("Pulse MVI — ${project.name}")
				url.set("https://github.com/nicoolasoo/PulseMVI")
				licenses {
					license {
						name.set("The Apache License, Version 2.0")
						url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
					}
				}
			}
		}
	}
}
