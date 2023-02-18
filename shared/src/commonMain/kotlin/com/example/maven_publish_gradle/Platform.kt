package com.example.maven_publish_gradle

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform