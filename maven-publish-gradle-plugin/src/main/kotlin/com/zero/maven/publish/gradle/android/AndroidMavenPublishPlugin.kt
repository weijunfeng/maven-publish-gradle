package com.zero.maven.publish.gradle.android

import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.internal.api.DefaultAndroidSourceDirectorySet
import com.zero.maven.publish.gradle.*
import org.gradle.api.Project
import org.gradle.api.plugins.internal.DefaultAdhocSoftwareComponent
import org.gradle.api.publish.PublicationContainer
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType

class AndroidMavenPublishPlugin : BaseMavenPublishPlugin {
    override fun afterEvaluate(project: Project, mavenPublishExtension: MavenPublishExtension) {
        createSourceJarTask(project)
        project.gradlePublishing(mavenPublishExtension)
        project.groupPublishToMavenTask(mavenPublishExtension)
        project.initRepositoryUrl(project.gradlePublishing.publications, mavenPublishExtension)
        project.doLastPrintUrl(mavenPublishExtension)
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
        project.components.withType<DefaultAdhocSoftwareComponent>()
            .forEach { softwareComponent ->
                mavenPublishExtension.publicationTypes.forEach { publicationType ->
                    createPublication(softwareComponent, publicationType, mavenPublishExtension, project)
                }
            }
    }

    private fun PublicationContainer.createPublication(
        softwareComponent: DefaultAdhocSoftwareComponent,
        publicationType: PublicationType,
        mavenPublishExtension: MavenPublishExtension,
        project: Project
    ) {
        create<MavenPublication>(softwareComponent.publicationName(publicationType)) {
            from(softwareComponent)
            groupId = mavenPublishExtension.publishGroupId.get()
            artifactId = project.name + "-" + softwareComponent.name
            version = mavenPublishExtension.publicationVersion(publicationType)
            artifact(project.tasks.getByName("sourceJar"))
        }
    }

    private fun createSourceJarTask(project: Project) {
        project.tasks.register("sourceJar", Jar::class) {
            archiveClassifier.set("sources")
            if (project.plugins.hasPlugin("com.android.library")
                || project.plugins.hasPlugin("com.android.application")
                || project.plugins.hasPlugin("android-library")
            ) {
                val srcDirs = project.extensions.getByType<LibraryExtension>()
                    .sourceSets
                    .getByName("main")
                    .kotlin as DefaultAndroidSourceDirectorySet
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
}