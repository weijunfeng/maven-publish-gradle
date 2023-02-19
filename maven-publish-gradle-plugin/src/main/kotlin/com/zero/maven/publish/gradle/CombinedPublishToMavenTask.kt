package com.zero.maven.publish.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.tasks.Internal

open class CombinedPublishToMavenTask : DefaultTask() {
    @Internal
    var repository: MavenArtifactRepository? = null
}