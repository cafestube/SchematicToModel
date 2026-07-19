plugins {
    kotlin("jvm") version "2.1.20"
    id("com.gradleup.shadow") version "8.3.11"
}

group = "eu.cafestube"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven {
        name = "cafestubeRepository"
        url = uri("https://repo.cafestube.net/repository/maven-public-snapshots/")
    }
    maven {
        url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
    }
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("eu.cafestube:WetSchematics:2.0.8-SNAPSHOT")
    implementation("commons-cli:commons-cli:1.9.0")

    implementation("team.unnamed:creative-api:1.8.2-SNAPSHOT")
    implementation("team.unnamed:creative-serializer-minecraft:1.8.2-SNAPSHOT")
    implementation("net.kyori:adventure-nbt:5.2.0")
}

tasks.jar {
    manifest {
        attributes.put("Main-Class", "eu.cafestube.schematictomodel.SchematicToModelKt")
    }
}

tasks.named("assemble") {
    dependsOn(tasks.named("shadowJar"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}