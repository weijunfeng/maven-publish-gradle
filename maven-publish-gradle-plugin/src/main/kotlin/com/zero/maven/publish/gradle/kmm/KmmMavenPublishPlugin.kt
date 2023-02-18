package com.zero.maven.publish.gradle.kmm

import com.zero.maven.publish.gradle.*
import org.gradle.api.Project
import org.gradle.api.artifacts.repositories.ArtifactRepository
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.component.SoftwareComponent
import org.gradle.api.publish.PublicationContainer
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.internal.publication.DefaultMavenPublication
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinSoftwareComponent

class KmmMavenPublishPlugin : BaseMavenPublishPlugin {
    override fun afterEvaluate(project: Project, mavenPublishExtension: MavenPublishExtension) {
        project.gradlePublishing(mavenPublishExtension)
        project.createPublishAllTask(
            project.gradlePublishing.repositories.filterIsInstance<MavenArtifactRepository>(),
            mavenPublishExtension
        )
        project.handlePublishAllTaskDepends(mavenPublishExtension)
        project.groupPublishToMavenTask(mavenPublishExtension)
        project.initRepositoryUrl(project.gradlePublishing.publications, mavenPublishExtension)
        project.doLastPrintUrl(mavenPublishExtension)
    }

    private fun Project.handlePublishAllTaskDepends(mavenPublishExtension: MavenPublishExtension) {
        tasks.withType<PublishToMavenRepository>().forEach {
            val publication = it.publication
            val publicationType = publication.publicationType(mavenPublishExtension) ?: return@forEach
            if (publication !is DefaultMavenPublication) {
                return
            }
            if (publication.component == null) {
                return
            }
            val kotlin = extensions.getByType<KotlinMultiplatformExtension>()
            val androidTarget = kotlin.targets.filterIsInstance<KotlinAndroidTarget>().firstOrNull()
            if (publication.kotlinTarget(this@handlePublishAllTaskDepends) != androidTarget) {
                tasks.named(publishAllIosToSingleRepoTaskName(publicationType, it.repository)) {
                    this.dependsOn(it)
                }
            }
            tasks.named(publishAllToSingleRepoTaskName(publicationType, it.repository)) {
                this.dependsOn(it)
            }
        }
    }

    private fun Project.gradlePublishing(mavenPublishExtension: MavenPublishExtension) {
        gradlePublishing {
            repositories {
                publishRepository(this@gradlePublishing, mavenPublishExtension)
            }
            publications {
                publishPublication(this@gradlePublishing, mavenPublishExtension)
            }
        }
    }

    private fun PublicationContainer.publishPublication(
        project: Project,
        mavenPublishExtension: MavenPublishExtension
    ) {
        project.components.withType<KotlinSoftwareComponent>()
            .forEach { kotlinSoftwareComponent ->
                kotlinSoftwareComponent.variants.forEach { softwareComponent ->
                    mavenPublishExtension.publicationTypes.forEach { publicationType ->
                        createPublication(softwareComponent, publicationType, mavenPublishExtension, project, kotlinSoftwareComponent)
                    }
                }
            }
    }

    private fun PublicationContainer.createPublication(
        softwareComponent: SoftwareComponent,
        publicationType: PublicationType,
        mavenPublishExtension: MavenPublishExtension,
        project: Project,
        kotlinSoftwareComponent: KotlinSoftwareComponent
    ) {
        this.create<MavenPublication>(softwareComponent.publicationName(publicationType)) {
            from(softwareComponent)
            groupId = mavenPublishExtension.publishGroupId.get()
            artifactId = project.name + "-" + softwareComponent.name
            version = mavenPublishExtension.publicationVersion(publicationType)
            kotlinSoftwareComponent.sourcesArtifacts.forEach {
                artifact(it)
            }
        }
    }

    private fun Project.createPublishAllTask(repositories: List<MavenArtifactRepository>, mavenPublishExtension: MavenPublishExtension) {
        repositories.forEach { mavenArtifactRepository ->
            mavenPublishExtension.publicationTypes.forEach {
                this@createPublishAllTask.tasks.register(
                    publishAllIosToSingleRepoTaskName(it, mavenArtifactRepository),
                    PublishAllIosTask::class
                ) {
                    this.group = mavenPublishExtension.taskGroup.get()
                    this.publicationType = it
                    this.repository = mavenArtifactRepository
                }
                this@createPublishAllTask.tasks.register(
                    publishAllToSingleRepoTaskName(it, mavenArtifactRepository),
                    PublishAllIosTask::class
                ) {
                    this.group = mavenPublishExtension.taskGroup.get()
                    this.publicationType = it
                    this.repository = mavenArtifactRepository
                }
            }
        }
    }

    private fun publishAllIosToSingleRepoTaskName(publicationType: PublicationType, repository: ArtifactRepository): String {
        return "publishAllIos${publicationType.typeName}PublicationsTo" + repository.name.capitalize() + "Repository"
    }

    private fun publishAllToSingleRepoTaskName(publicationType: PublicationType, repository: ArtifactRepository): String {
        return "publishAll${publicationType.typeName}PublicationsTo" + repository.name.capitalize() + "Repository"
    }

    companion object {
        fun DefaultMavenPublication.kotlinTarget(project: Project): KotlinTarget? {
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
    }
}

