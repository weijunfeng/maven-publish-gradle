package com.zero.maven.publish.gradle.kmm

import com.zero.maven.publish.gradle.PublicationType
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.tasks.Internal

open class PublishAllIosTask : DefaultTask() {
    @Internal
    var publicationType: PublicationType? = null

    @Internal
    var repository: MavenArtifactRepository? = null
}