import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
	alias(libs.plugins.kotlin.multiplatform)
	alias(libs.plugins.kotlin.compose)
	alias(libs.plugins.compose.multiplatform)
	alias(libs.plugins.kotlin.serialization)
	alias(libs.plugins.android.kotlin.multiplatform.library)
}

val javaVersion = rootProject.extra["javaVersion"] as JavaVersion
val pulseGroup: String = providers.gradleProperty("pulse.group").get()
val baseVersion: String = providers.gradleProperty("pulse.version").get()

kotlin {

	// ================================
	// MARK: Android
	// ================================

	android {
		compileSdk = rootProject.extra["compileSdk"] as Int
		minSdk = rootProject.extra["minSdk"] as Int
		namespace = "com.bitsycore.demo.pulse"
		compilerOptions {
			jvmTarget = JvmTarget.fromTarget(javaVersion.toString())
		}
		androidResources.enable = true
	}

	// ================================
	// MARK: JVM
	// ================================

	jvm {
		compilerOptions {
			jvmTarget = JvmTarget.fromTarget(javaVersion.toString())
		}
	}

	// ================================
	// MARK: Dependencies
	// ================================

	sourceSets {
		commonMain.dependencies {
			providers.gradleProperty("pulse.demo.use").orNull.let { mode ->
				listOf(
					":pulse",
					":pulse-viewmodel",
					":pulse-compose",
					":pulse-savedstate"
				).forEach { lib ->
					when (mode) {
						"snapshot" -> implementation("$pulseGroup:$lib:$baseVersion-SNAPSHOT-+")
						"release" -> implementation("$pulseGroup:$lib:$baseVersion")
						"local" -> implementation(project(lib))
						else -> error("Unknown pulse.demo.use=$mode")
					}
				}
			}
			implementation(libs.jetbrains.compose.material3)
			implementation(libs.jetbrains.compose.foundation)
			implementation(libs.jetbrains.compose.ui)
			implementation(libs.jetbrains.androidx.lifecycle.viewmodel.compose)
			implementation(libs.jetbrains.androidx.navigation3.ui)
			implementation(libs.jetbrains.androidx.lifecycle.viewmodel.navigation3)
			implementation(libs.jetbrains.compose.ui.tooling.preview)
			implementation(libs.jetbrains.compose.components.resources)
		}

		jvmMain.dependencies {
			if (providers.gradleProperty("pulse.demo.allDesktopPlatform").map { it.toBoolean() }.get()) {
				implementation(libs.jetbrains.compose.desktop.jvm.windows.x64)
				implementation(libs.jetbrains.compose.desktop.jvm.windows.arm64)
				implementation(libs.jetbrains.compose.desktop.jvm.linux.x64)
				implementation(libs.jetbrains.compose.desktop.jvm.linux.arm64)
				implementation(libs.jetbrains.compose.desktop.jvm.macos.arm64)
				implementation(libs.jetbrains.compose.desktop.jvm.macos.x64)
			} else {
				implementation(compose.desktop.currentOs)
			}
			implementation(libs.kotlinx.coroutines.swing)
		}
	}
}

compose.resources {
	packageOfResClass = "com.bitsycore.demo.pulse.generated.resources"
	publicResClass = false
	generateResClass = auto
}

compose.desktop {
	application {
		mainClass = "com.bitsycore.demo.pulse.MainKt"
		nativeDistributions {
			targetFormats(TargetFormat.AppImage)
			packageName = "PulseDemo"
			packageVersion = baseVersion
		}
	}
}
