# maven-publish-gradle
## **一个对maven-publish的快速配置扩展工具**

## 版本
最新版 1.0.4

## **特性**

1. 支持快速配置`maven` 发布配置信息
2. 支持输出依赖配置信息
3. 支持配置发布任务组

## 使用

1. 在需要使用的组件的build.gradle.kts的最后添加引入plugin

```jsx
plugins {
    id("io.github.wjf510.maven.publish").version("xxx")
}
mavenPublish {
    mavenUsername.set("")

    mavenPassword.set("")

    // 配置Snapshot发布地址，启用Snapshot版本发布
    mavenSnapshotUrl = "https://github.com" 

    // 配置Release发布地址，启用Release版本发布
    mavenReleaseUrl = "https://github.com"

    publishGroupId.set("com.weijunfeng.maven.plugin")

    // 发布版本前缀，打包时自动添加发布类型["-LOCAL","-SNAPSHOT","-RELEASE"]
    publishVersionPrefix.set("1.01.1")
}
```