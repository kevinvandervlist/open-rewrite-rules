plugins {
    `java-library`

    id("org.jetbrains.kotlin.jvm") version "1.5.21"
    id("nebula.release") version "15.3.1"

    id("nebula.maven-manifest") version "17.3.2"
    id("nebula.maven-resolved-dependencies") version "17.3.2"

    id("nebula.info") version "9.3.0"

    id("nebula.javadoc-jar") version "17.3.2"
    id("nebula.source-jar") version "17.3.2"

    id("org.openrewrite.rewrite") version("5.14.0")
}



configure<nebula.plugin.release.git.base.ReleasePluginExtension> {
    defaultVersionStrategy = nebula.plugin.release.NetflixOssStrategies.SNAPSHOT(project)
}

group = "nl.kevinvandervlist.recipes"
version = "0.0.1"
description = "Rewrite recipes."

repositories {
    mavenLocal()
    maven {
        url = uri("https://oss.sonatype.org/content/repositories/snapshots/")
    }
    mavenCentral()
}

configurations.all {
    resolutionStrategy {
        cacheChangingModulesFor(0, TimeUnit.SECONDS)
        cacheDynamicVersionsFor(0, TimeUnit.SECONDS)
    }
}

val rewriteVersion = "latest.integration"

dependencies {
    compileOnly("org.projectlombok:lombok:latest.release")
    annotationProcessor("org.projectlombok:lombok:latest.release")

    implementation("org.openrewrite:rewrite-java:${rewriteVersion}")
    runtimeOnly("org.openrewrite:rewrite-java-11:${rewriteVersion}")

    testImplementation("org.jetbrains.kotlin:kotlin-reflect")
    testImplementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    testImplementation("org.junit.jupiter:junit-jupiter-api:latest.release")
    testImplementation("org.junit.jupiter:junit-jupiter-params:latest.release")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:latest.release")

    testImplementation("org.openrewrite:rewrite-test:${rewriteVersion}")
    testImplementation("org.assertj:assertj-core:latest.release")
    rewrite("nl.kevinvandervlist.recipes:company-recipes:0.0.1")
}

rewrite {
    activeRecipe("nl.kevinvandervlist.RecipeCollection")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
    jvmArgs = listOf("-XX:+UnlockDiagnosticVMOptions", "-XX:+ShowHiddenFrames")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}
