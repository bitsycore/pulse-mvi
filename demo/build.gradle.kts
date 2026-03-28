import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
	alias(libs.plugins.kotlin.multiplatform)
	alias(libs.plugins.kotlin.compose)
	alias(libs.plugins.compose.multiplatform)
	alias(libs.plugins.kotlin.serialization)
}

val javaVersion: JavaVersion by rootProject.extra

kotlin {

	jvm {
		compilerOptions {
			jvmTarget = JvmTarget.fromTarget(javaVersion.toString())
		}
	}

	sourceSets {
		commonMain.dependencies {
			implementation(project(":pulse"))
			implementation(project(":pulse-viewmodel"))
			implementation(project(":pulse-compose"))
			implementation(project(":pulse-savedstate"))

			implementation(libs.jetbrains.compose.material3)
			implementation(libs.jetbrains.compose.foundation)
			implementation(libs.jetbrains.compose.ui)
			implementation(libs.jetbrains.androidx.lifecycle.viewmodel.compose)
			implementation(libs.jetbrains.androidx.navigation3.ui)
			implementation(libs.jetbrains.androidx.lifecycle.viewmodel.navigation3)
		}

		jvmMain.dependencies {
			implementation(compose.desktop.currentOs)
			implementation(libs.kotlinx.coroutines.swing)
		}
	}
}

compose.desktop {
	application {
		mainClass = "com.bitsycore.demo.MainKt"
		nativeDistributions {
			targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
			packageName = "PulseDemo"
			packageVersion = "1.0.0"
		}
	}
}
