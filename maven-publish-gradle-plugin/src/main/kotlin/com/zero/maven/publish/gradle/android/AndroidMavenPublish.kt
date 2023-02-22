package com.zero.maven.publish.gradle.android

import com.zero.maven.publish.gradle.*
import org.gradle.api.Project
import org.gradle.api.plugins.internal.DefaultAdhocSoftwareComponent
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.internal.publication.DefaultMavenPublication
import org.gradle.api.publish.maven.tasks.AbstractPublishToMaven
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType

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

class AndroidMavenPublish {
    fun afterEvaluate(project: Project, mavenPublishExtension: MavenPublishExtension) {
        createSourceJarTask(project)
        project.setPublications()
        project.setRepositories(mavenPublishExtension)
        project.setGroupId(mavenPublishExtension)
        project.groupTask(mavenPublishExtension)
        project.doLastPrintUrl()
    }

    private fun createSourceJarTask(project: Project) {
        project.tasks.register("sourceJar", org.gradle.jvm.tasks.Jar::class) {
            archiveClassifier.set("sources")
            if (project.plugins.hasPlugin("com.android.library")
                || project.plugins.hasPlugin("com.android.application")
                || project.plugins.hasPlugin("android-library")
            ) {
                val androidSourceSet = project.extensions.getByType<com.android.build.gradle.LibraryExtension>()
                    .sourceSets
                    .getByName("main")
                val kotlinSrcDirs = androidSourceSet
                    .kotlin as com.android.build.gradle.internal.api.DefaultAndroidSourceDirectorySet
                val javaSrcDirs = androidSourceSet
                    .java as com.android.build.gradle.internal.api.DefaultAndroidSourceDirectorySet
                val srcDirs = kotlinSrcDirs.srcDirs + javaSrcDirs.srcDirs
                from(*(srcDirs.toTypedArray()))
            } else if (project.plugins.hasPlugin("java")
                || project.plugins.hasPlugin("java-library")
            ) {
                from(
                    project.extensions.getByType<SourceSetContainer>()
                        .getByName("main")
                        .allJava
                        .srcDirs
                )
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
            it.group = mavenPublishExtension.taskGroup.get()
        }
    }

    private fun Project.setPublications() {
        gradlePublishing {
            publications {
                project.components.withType<DefaultAdhocSoftwareComponent>()
                    .forEach { softwareComponent ->
                        create<MavenPublication>(softwareComponent.name) {
                            from(softwareComponent)
                            artifactId = project.name + "-" + softwareComponent.name
                            artifact(project.tasks.getByName("sourceJar"))
                        }
                    }
            }
        }
    }
}