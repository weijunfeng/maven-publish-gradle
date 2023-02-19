package com.zero.maven.publish.gradle.kmm

import com.zero.maven.publish.gradle.*
import org.gradle.api.Project
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.publish.maven.internal.publication.DefaultMavenPublication
import org.gradle.api.publish.maven.tasks.AbstractPublishToMaven
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget

class KmmMavenPublishPlugin : BaseMavenPublishPlugin {
    override fun afterEvaluate(project: Project, mavenPublishExtension: MavenPublishExtension) {
        project.setRepositories()
        project.setGroupId()
        project.createCombinedPublishTask()
        project.groupTask()
        project.doLastPrintUrl()
    }


    private fun publishAllIosToSingleRepoTaskName(repository: MavenArtifactRepository): String {
        return "publishAllIosPublicationsTo" + repository.name.capitalize() + "Repository"
    }

    private fun publishAllMobileToSingleRepoTaskName(repository: MavenArtifactRepository): String {
        return "publishAllMobilePublicationsTo" + repository.name.capitalize() + "Repository"
    }

    private fun DefaultMavenPublication.kotlinTarget(project: Project): KotlinTarget? {
        if (this.component == null) {
            return null
        }
        val kotlin = project.extensions.getByType<KotlinMultiplatformExtension>()
        kotlin.targets
            .forEach {
                if (it.components.contains(this.component)) {
                    return it
                }
            }
        return null
    }


    private fun Project.createCombinedPublishTask() {
        gradlePublishing.repositories.forEach { mavenArtifactRepository ->
            if (mavenArtifactRepository !is MavenArtifactRepository) {
                return@forEach
            }
            tasks.register(
                publishAllIosToSingleRepoTaskName(mavenArtifactRepository),
                CombinedPublishToMavenTask::class
            ) {
                repository = mavenArtifactRepository
            }
            tasks.register(
                publishAllMobileToSingleRepoTaskName(mavenArtifactRepository),
                CombinedPublishToMavenTask::class
            ) {
                repository = mavenArtifactRepository
            }
        }
    }

    private fun Project.groupTask() {
        tasks.withType<AbstractPublishToMaven>().forEach {
            if (it !is PublishToMavenRepository) {
                return@forEach
            }
            val publication = it.publication
            if (publication !is DefaultMavenPublication) {
                return@forEach
            }
            val kotlinPublicationTarget = publication.kotlinTarget(this)
            if (kotlinPublicationTarget !is org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget
                && kotlinPublicationTarget !is org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
            ) {
                return@forEach
            }
            if (kotlinPublicationTarget is org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget) {
                tasks.named(publishAllIosToSingleRepoTaskName(it.repository)) {
                    this.dependsOn(it)
                    this.group = mavenPublishExtension.taskGroup.get()
                }
            }
            tasks.named(publishAllMobileToSingleRepoTaskName(it.repository)) {
                this.dependsOn(it)
                this.group = mavenPublishExtension.taskGroup.get()
            }
            it.group = mavenPublishExtension.taskGroup.get()
        }
    }
}

