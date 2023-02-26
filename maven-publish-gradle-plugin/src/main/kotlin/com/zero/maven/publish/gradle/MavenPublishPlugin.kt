package com.zero.maven.publish.gradle

import com.zero.maven.publish.gradle.android.AndroidMavenPublish
import com.zero.maven.publish.gradle.kmm.KmmMavenPublish
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.extra
import org.gradle.kotlin.dsl.withType
import java.util.*

class MavenPublishPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.plugins.apply("maven-publish")
        project.plugins.apply("signing")
        val defaultConfig = project.readDefaultConfig()
        val mavenPublishExtension = project.createMavenPublishExtension(defaultConfig)
        // 由于一些组件是在环境初始化后设置，需要在afterEvaluate配置maven信息
        project.afterEvaluate {
            project.afterAndroidPluginEvaluated {
                afterEvaluate(project, mavenPublishExtension)
                project.configPom(mavenPublishExtension)
            }
        }
        project.signing(
            defaultConfig.signing_keyId,
            defaultConfig.signing_password,
            defaultConfig.signing_secretKeyRingFile
        )
    }

    private fun Project.createMavenPublishExtension(defaultConfig: DefaultConfig): MavenPublishExtension {
        val mavenPublishExtension = extensions.create("mavenPublish", MavenPublishExtension::class)
        val ossrhUsername = defaultConfig.ossrhUsername
        val ossrhPassword = defaultConfig.ossrhPassword
        if (!ossrhPassword.isNullOrEmpty()) {
            mavenPublishExtension.mavenPassword.convention(ossrhPassword)
        }
        if (!ossrhUsername.isNullOrEmpty()) {
            mavenPublishExtension.mavenUsername.convention(ossrhUsername)
        }
        mavenPublishExtension.taskGroup.convention("mavenpublish")
        mavenPublishExtension.pom.convention {
            // ignore
        }
        return mavenPublishExtension
    }

    private fun Project.configPom(mavenPublishExtension: MavenPublishExtension) {
        gradlePublishing.publications.withType<MavenPublication> {
            pom {
                mavenPublishExtension.pom.get().execute(this)
            }
        }
    }

    private fun Project.getExtraString(name: String) = extra[name]?.toString()

    private fun Project.readDefaultConfig(): DefaultConfig {
        // 必须设置到extra中否则signing失败
        val ext = extra
        ext["signing.keyId"] = null
        ext["signing.password"] = null
        ext["signing.secretKeyRingFile"] = null
        ext["ossrhUsername"] = null
        ext["ossrhPassword"] = null
        val localPropsFile = rootProject.file("local.properties")
        if (localPropsFile.exists()) {
            localPropsFile.reader()
                .use { Properties().apply { load(it) } }
                .onEach { (name, value) -> ext[name.toString()] = value.toString() }
        } else {
            ext["signing.keyId"] = System.getenv("SIGNING_KEY_ID")
            ext["signing.password"] = System.getenv("SIGNING_PASSWORD")
            ext["signing.secretKeyRingFile"] = System.getenv("SIGNING_SECRET_KEY_RING_FILE")
            ext["ossrhUsername"] = System.getenv("OSSRH_USERNAME")
            ext["ossrhPassword"] = System.getenv("OSSRH_PASSWORD")
        }
        return DefaultConfig(
            getExtraString("signing.keyId"),
            getExtraString("signing.password"),
            getExtraString("signing.secretKeyRingFile"),
            getExtraString("ossrhUsername"),
            getExtraString("ossrhPassword"),
        )
    }

    private fun Project.signing(keyId: String?, secretKey: String?, secretKeyRingFile: String?) {
        if (!keyId.isNullOrEmpty() && !secretKey.isNullOrEmpty() && !secretKeyRingFile.isNullOrEmpty()) {
            gradleSigning {
                sign(gradlePublishing.publications)
            }
        }
    }

    private fun afterEvaluate(project: Project, mavenPublishExtension: MavenPublishExtension) {
        val hostPlatform = HostPlatform.current
        if (project.plugins.hasPlugin("org.jetbrains.kotlin.multiplatform") && hostPlatform == HostPlatform.MacOS) {
            KmmMavenPublish().afterEvaluate(project, mavenPublishExtension)
        } else {
            AndroidMavenPublish().afterEvaluate(project, mavenPublishExtension)
        }
    }

    class DefaultConfig(
        val signing_keyId: String?,

        val signing_password: String?,

        val signing_secretKeyRingFile: String?,

        val ossrhUsername: String?,

        val ossrhPassword: String?
    )
}