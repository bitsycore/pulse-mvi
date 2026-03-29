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
val useSnapshot: Boolean = providers.gradleProperty("pulse.useSnapshot").map { it.toBoolean() }.get()
val pulseGroup: String = providers.gradleProperty("pulse.group").get()
val baseVersion: String = providers.gradleProperty("pulse.version").get()
// "+" suffix tells Gradle to resolve the latest matching version from mavenLocal
val snapshotVersion = "$baseVersion-SNAPSHOT-+"

kotlin {

	// ================================
	// MARK: Android
	// ================================

	android {
		compileSdk = rootProject.extra["compileSdk"] as Int
		minSdk = rootProject.extra["minSdk"] as Int
		namespace = "com.bitsycore.demo.pulse.android"
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
			if (useSnapshot) {
				implementation("$pulseGroup:pulse:$snapshotVersion")
				implementation("$pulseGroup:pulse-viewmodel:$snapshotVersion")
				implementation("$pulseGroup:pulse-compose:$snapshotVersion")
				implementation("$pulseGroup:pulse-savedstate:$snapshotVersion")
			} else {
				implementation(projects.pulse)
				implementation(projects.pulseViewmodel)
				implementation(projects.pulseCompose)
				implementation(projects.pulseSavedstate)
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
			implementation(compose.desktop.currentOs)
			implementation(libs.kotlinx.coroutines.swing)
		}
	}
}

compose.resources {
	publicResClass = false
	generateResClass = auto
}

compose.desktop {
	application {
		mainClass = "com.bitsycore.demo.pulse.MainKt"
		nativeDistributions {
			targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Exe, TargetFormat.Deb)
			packageName = "PulseDemo"
			packageVersion = "1.0.0"
		}
	}
}
