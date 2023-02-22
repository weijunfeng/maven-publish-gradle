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
import java.util.*

/*
 * Copyright (c) 2021 wjf510.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

class KmmMavenPublish {
    fun afterEvaluate(project: Project, mavenPublishExtension: MavenPublishExtension) {
        project.setRepositories(mavenPublishExtension)
        project.setGroupId(mavenPublishExtension)
        project.createCombinedPublishTask()
        project.groupTask(mavenPublishExtension)
        project.doLastPrintUrl()
    }


    private fun publishAllIosToSingleRepoTaskName(repository: MavenArtifactRepository): String {
        return "publishAllIosPublicationsTo" + repository.name.capitalize(Locale.ROOT) + "Repository"
    }

    private fun publishAllMobileToSingleRepoTaskName(repository: MavenArtifactRepository): String {
        return "publishAllMobilePublicationsTo" + repository.name.capitalize(Locale.ROOT) + "Repository"
    }

    private fun DefaultMavenPublication.kotlinTarget(project: Project): KotlinTarget? {
        val component = this.component ?: return null
        val kotlin = project.extensions.getByType<KotlinMultiplatformExtension>()
        kotlin.targets
            .forEach {
                if (it.components.contains(component)) {
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

    private fun Project.groupTask(mavenPublishExtension: MavenPublishExtension) {
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

