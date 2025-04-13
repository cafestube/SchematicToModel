plugins {
    kotlin("jvm") version "2.1.0"
    id("com.gradleup.shadow") version "8.3.5"
}

group = "eu.cafestube"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven {
        name = "cafestubeRepository"
        url = uri("https://repo.cafestu.be/repository/maven-public-snapshots/")
    }
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("eu.cafestube:WetSchematics:2.0.8-SNAPSHOT")
    implementation("commons-cli:commons-cli:1.9.0")

    implementation("team.unnamed:creative-api:1.7.3")
    implementation("team.unnamed:creative-serializer-minecraft:1.7.3")
    implementation("net.kyori:adventure-nbt:4.20.0")
}

tasks.jar {
    manifest {
        attributes.put("Main-Class", "eu.cafestube.sprouts.tooling.island.splitter.IslandSplitterKt")
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