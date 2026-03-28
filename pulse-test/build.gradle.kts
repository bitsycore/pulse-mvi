import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
	alias(libs.plugins.kotlin.multiplatform)
}

val javaVersion: JavaVersion by rootProject.extra

kotlin {

	jvm {
		compilerOptions {
			jvmTarget = JvmTarget.fromTarget(javaVersion.toString())
		}
	}

	iosArm64()
	iosSimulatorArm64()
	iosX64()

	macosArm64()

	linuxArm64()
	linuxX64()

	mingwX64()

	sourceSets {
		all {
			languageSettings.optIn("kotlin.time.ExperimentalTime")
			compilerOptions {
				freeCompilerArgs.add("-Xexpect-actual-classes")
				freeCompilerArgs.add("-Xcontext-parameters")
				freeCompilerArgs.add("-Xcontext-sensitive-resolution")
				freeCompilerArgs.add("-Xexplicit-backing-fields")
			}
		}

		commonMain.dependencies {
			api(project(":pulse"))
			implementation(libs.kotlin.test)
			implementation(libs.kotlinx.coroutines.test)
		}
	}
}
