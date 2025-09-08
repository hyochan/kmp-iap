import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

// Load local.properties
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}

// Load environment variables first (for CI)
System.getenv().forEach { (key, value) ->
    if (key.startsWith("ORG_GRADLE_PROJECT_")) {
        val propertyKey = key.removePrefix("ORG_GRADLE_PROJECT_")
        localProperties.setProperty(propertyKey, value)
        project.extensions.extraProperties.set(propertyKey, value)
    }
}

// Handle GPG key from environment variable or file
val envGpgKey = System.getenv("ORG_GRADLE_PROJECT_signingInMemoryKey")
if (envGpgKey != null && envGpgKey.isNotBlank()) {
    // CI/CD: Use environment variable directly
    val cleanedKey = envGpgKey.trim()
    localProperties.setProperty("signingInMemoryKey", cleanedKey)
    project.extensions.extraProperties.set("signingInMemoryKey", cleanedKey)
    println("DEBUG: GPG key loaded from environment variable (length: ${cleanedKey.length})")
} else {
    // Local development: Read GPG key from file if specified
    val keyFile = localProperties.getProperty("signingInMemoryKeyFile")
    if (keyFile != null) {
        val keyFileHandle = rootProject.file(keyFile)
        if (keyFileHandle.exists()) {
            val keyContent = keyFileHandle.readText().trim()
            localProperties.setProperty("signingInMemoryKey", keyContent)
            project.extensions.extraProperties.set("signingInMemoryKey", keyContent)
            println("DEBUG: GPG key loaded from file: $keyFile (length: ${keyContent.length})")
        } else {
            println("DEBUG: GPG key file not found: $keyFile")
        }
    } else {
        println("DEBUG: No GPG key configured (for CI, set ORG_GRADLE_PROJECT_signingInMemoryKey)")
    }
}

// Add local properties to project properties
localProperties.forEach { key, value ->
    // Set as project extra properties
    project.extra.set(key.toString(), value)
    // Also set via extensions for vanniktech plugin
    project.extensions.extraProperties.set(key.toString(), value)
    
    // For vanniktech plugin, also set as system properties
    if (key.toString().startsWith("maven") || key.toString().startsWith("central") || key.toString().startsWith("signing")) {
        System.setProperty(key.toString(), value.toString())
    }
}

// Ensure critical properties are available as both project and system properties
val criticalProperties = listOf(
    "mavenCentralUsername",
    "mavenCentralPassword", 
    "sonatypeRepositoryId",
    "sonatypeAutomaticRelease",
    "signingInMemoryKeyId",
    "signingInMemoryKey",
    "signingInMemoryKeyPassword",
    "libraryVersion"
)

criticalProperties.forEach { propName ->
    val value = localProperties.getProperty(propName) ?: project.findProperty(propName) as String?
    if (value != null) {
        project.extra.set(propName, value)
        System.setProperty(propName, value)
        // Also set as gradle property for vanniktech plugin
        project.extensions.extraProperties.set(propName, value)
        if (propName == "signingInMemoryKey") {
            println("DEBUG: signingInMemoryKey is set (length: ${value.length})")
            println("DEBUG: First 50 chars: ${value.take(50)}")
        }
    } else if (propName == "signingInMemoryKey") {
        println("DEBUG: signingInMemoryKey is NOT set!")
    }
}

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.vanniktechMavenPublish)
    alias(libs.plugins.dokka)
    alias(libs.plugins.kotlinxSerialization)
    // Enable CocoaPods integration for iOS
    id("org.jetbrains.kotlin.native.cocoapods")
    signing
}

group = "io.github.hyochan"
version = localProperties.getProperty("libraryVersion") ?: "1.0.0-alpha02"

kotlin {
    androidTarget {
        publishLibraryVariants("release")
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    // CocoaPods configuration for iOS linking and external Pods
cocoapods {
        summary = "KMP IAP: OpenIAP-compliant IAP library (Kotlin Multiplatform)"
        homepage = "https://github.com/hyochan/kmp-iap"
        ios.deploymentTarget = "15.0"
        // Use the example iOS Podfile to control CocoaPods settings (static linkage)
        podfile = project.file("../example/iosApp/Podfile")
        framework {
            baseName = "KmpIap"
            isStatic = true
        }

        // Install OpenIAP CocoaPods dependency (v1.1.6)
        // Pod name is lowercase 'openiap' per upstream spec
        pod("openiap") {
            version = "1.1.6"
        }
    }

// Patch the synthetic Podfile to force static frameworks (fixes SwiftUICore/main alias link issues)
tasks.register("patchSyntheticPodfile") {
    doLast {
        val podfile = file("build/cocoapods/synthetic/ios/Podfile")
        if (podfile.exists()) {
            var content = podfile.readText()
            // Force static linkage for frameworks
            content = content.replace("use_frameworks!", "use_frameworks! :linkage => :static")
            // Ensure we force static lib mode for 'openiap' target to avoid dylib '_main' alias
            if (!content.contains("MACH_O_TYPE") || !content.contains("OTHER_LDFLAGS")) {
                content = content.replace(
                    "post_install do |installer|",
                    "post_install do |installer|\n  installer.pods_project.targets.each do |target|\n    target.build_configurations.each do |config|\n      # Force static output for OpenIAP pod\n      if target.name == 'openiap'\n        config.build_settings['MACH_O_TYPE'] = 'staticlib'\n      end\n      # Remove debug-executable alias flags that inject _main into dylib link\n      if config.build_settings['OTHER_LDFLAGS']\n        flags = [config.build_settings['OTHER_LDFLAGS']].flatten.map(&:to_s)\n        flags = flags.reject { |f| f.include?('_main') || f.include?('___debug_main_executable_dylib_entry_point') }\n        config.build_settings['OTHER_LDFLAGS'] = flags\n      end\n      config.build_settings['BUILD_LIBRARY_FOR_DISTRIBUTION'] = 'YES'\n    end\n  end\n"
                )
            }
            podfile.writeText(content)
            println("Patched synthetic Podfile for static frameworks")
        } else {
            println("Synthetic Podfile not found to patch: ${podfile.path}")
        }
    }
}

// Ensure patch runs after pod install generates the synthetic Podfile
tasks.matching { it.name == "podInstallSyntheticIos" }.configureEach {
    finalizedBy("patchSyntheticPodfile")
}

// Clean synthetic Pods so patched Podfile takes effect on next install
tasks.register("cleanSyntheticPods") {
    doLast {
        val base = file("build/cocoapods/synthetic/ios")
        val podsDir = file("build/cocoapods/synthetic/ios/Pods")
        val lock = file("build/cocoapods/synthetic/ios/Podfile.lock")
        val xcworkspace = file("build/cocoapods/synthetic/ios/Pods/Pods.xcodeproj")
        if (podsDir.exists()) podsDir.deleteRecursively()
        if (lock.exists()) lock.delete()
        if (xcworkspace.exists()) xcworkspace.deleteRecursively()
        println("Deleted synthetic Pods to force re-install")
    }
}

// Enforce re-install after patching Podfile
tasks.matching { it.name == "podSetupBuildOpeniapIos" || it.name == "podBuildOpeniapIos" }.configureEach {
    dependsOn("cleanSyntheticPods")
    dependsOn("podInstallSyntheticIos")
}

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.datetime)
                implementation(libs.kotlinx.serialization.json)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotlinx.coroutines.test)
            }
        }
        val androidMain by getting {
            dependencies {
                implementation(libs.billing)
                implementation(libs.billing.ktx)
            }
        }
    }
}

android {
    namespace = "io.github.hyochan.kmpiap"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

// Task to update README version
val updateReadmeVersion = tasks.register("updateReadmeVersion") {
    doLast {
        val version = localProperties.getProperty("libraryVersion") ?: "1.0.0-alpha04"
        val readmeFile = rootProject.file("README.md")
        val content = readmeFile.readText()
        val updatedContent = content.replace(
            Regex("implementation\\(\"io\\.github\\.hyochan:kmp-iap:.*\"\\)"),
            "implementation(\"io.github.hyochan:kmp-iap:$version\")"
        )
        readmeFile.writeText(updatedContent)
        println("Updated README.md with version: $version")
    }
}

// Automatically update README when publishing
tasks.withType<PublishToMavenRepository> {
    dependsOn(updateReadmeVersion)
}

// Only configure publishing when we have signing credentials
val hasSigningKey = localProperties.getProperty("signingInMemoryKey") != null ||
    System.getenv("ORG_GRADLE_PROJECT_signingInMemoryKey") != null

mavenPublishing {
    // Explicitly use Central Portal instead of legacy Sonatype
    publishToMavenCentral(com.vanniktech.maven.publish.SonatypeHost.CENTRAL_PORTAL)
    
    // Only sign if we have credentials
    if (hasSigningKey) {
        signAllPublications()
    }
    
    coordinates("io.github.hyochan", "kmp-iap", localProperties.getProperty("libraryVersion") ?: "1.0.0-alpha02")
    
    // Configure publications with empty Javadoc JAR (Maven Central compatible)
    configure(
        com.vanniktech.maven.publish.KotlinMultiplatform(
            javadocJar = com.vanniktech.maven.publish.JavadocJar.Empty(),
            sourcesJar = true,
        )
    )
    
    pom {
        name.set("KMP IAP")
        description.set("A Kotlin Multiplatform library for in app purchase on Android, iOS, Desktop, and Web platforms")
        inceptionYear.set("2025")
        url.set("https://github.com/hyochan/kmp-iap")
        
        licenses {
            license {
                name.set("MIT License")
                url.set("https://opensource.org/licenses/MIT")
                distribution.set("repo")
            }
        }
        
        developers {
            developer {
                id.set("hyochan")
                name.set("Hyo Chan Jang")
                email.set("hyo@hyo.dev")
                url.set("https://github.com/hyochan/")
                organization.set("hyochan")
                organizationUrl.set("https://github.com/hyochan")
            }
        }
        
        scm {
            connection.set("scm:git:git://github.com/hyochan/kmp-iap.git")
            developerConnection.set("scm:git:ssh://git@github.com/hyochan/kmp-iap.git")
            url.set("https://github.com/hyochan/kmp-iap")
            tag.set("v${localProperties.getProperty("libraryVersion") ?: "1.0.0-alpha02"}")
        }
        
        issueManagement {
            system.set("GitHub")
            url.set("https://github.com/hyochan/kmp-iap/issues")
        }
    }
}

// Manual signing configuration (disabled in favor of vanniktech signing)
/*
signing {
    val signingKeyId: String? by project
    val signingPassword: String? by project
    
    // Try to read signing key from file if not available in properties
    val signingKey: String? = project.findProperty("signingKey") as String? ?: 
        rootProject.file("temp_private_key.asc").takeIf { it.exists() }?.readText()
    
    println("DEBUG: signingKeyId = ${signingKeyId?.substring(0, 8)}...")
    println("DEBUG: signingKey present = ${signingKey != null}")
    println("DEBUG: signingPassword present = ${signingPassword != null}")
    
    if (signingKeyId != null && signingKey != null) {
        println("DEBUG: Configuring in-memory PGP keys for signing")
        useInMemoryPgpKeys(signingKeyId, signingKey, signingPassword)
        sign(publishing.publications)
    } else {
        println("DEBUG: Signing disabled - missing required properties")
    }
}
*/
