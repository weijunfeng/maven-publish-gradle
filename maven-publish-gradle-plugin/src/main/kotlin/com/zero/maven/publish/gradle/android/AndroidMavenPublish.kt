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
                val srcDirs = project.extensions.getByType<com.android.build.gradle.LibraryExtension>()
                    .sourceSets
                    .getByName("main")
                    .kotlin as com.android.build.gradle.internal.api.DefaultAndroidSourceDirectorySet
                from(*(srcDirs.srcDirs.toTypedArray()))
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