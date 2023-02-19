package com.zero.maven.publish.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.create

interface BaseMavenPublishPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.plugins.apply("maven-publish")
        val mavenPublishExtension = project.extensions.create("mavenPublish", MavenPublishExtension::class)
        mavenPublishExtension.taskGroup.convention("mavenpublish")
        // 由于一些组件是在环境初始化后设置，需要在afterEvaluate配置maven信息
        project.afterEvaluate {
            afterEvaluate(project, mavenPublishExtension)
        }
    }

    fun afterEvaluate(project: Project, mavenPublishExtension: MavenPublishExtension)
}