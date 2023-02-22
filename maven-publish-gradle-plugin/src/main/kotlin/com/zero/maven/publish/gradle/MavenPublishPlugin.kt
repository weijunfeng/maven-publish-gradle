package com.zero.maven.publish.gradle

import com.zero.maven.publish.gradle.android.AndroidMavenPublish
import com.zero.maven.publish.gradle.kmm.KmmMavenPublish
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.create

class MavenPublishPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.plugins.apply("maven-publish")
        val mavenPublishExtension = project.extensions.create("mavenPublish", MavenPublishExtension::class)
        mavenPublishExtension.taskGroup.convention("mavenpublish")
        // 由于一些组件是在环境初始化后设置，需要在afterEvaluate配置maven信息
        project.afterAndroidPluginEvaluated {
            afterEvaluate(project, mavenPublishExtension)
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
}