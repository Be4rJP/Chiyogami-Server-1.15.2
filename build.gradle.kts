plugins {
    java
    `maven-publish`

    // Nothing special about this, just keep it up to date
    id("com.github.johnrengelman.shadow") version "8.1.1" apply false

    // In general, keep this version in sync with upstream. Sometimes a newer version than upstream might work, but an older version is extremely likely to break.
    id("io.papermc.paperweight.patcher") version "1.5.5"
}

val paperMavenPublicUrl = "https://repo.papermc.io/repository/maven-public/"
val spigotDecompiler: Configuration by configurations.creating

repositories {
    mavenCentral()
    maven(paperMavenPublicUrl) {
        content {
            onlyForConfigurations(
                configurations.paperclip.name,
                spigotDecompiler.name,
            )
        }
    }
}

dependencies {
    //paramMappings("net.fabricmc:yarn:1.20.1+build.1:mergedv2")
    remapper("net.fabricmc:tiny-remapper:0.8.6:fat")
    decompiler("net.minecraftforge:forgeflower:2.0.627.2")
    spigotDecompiler("io.papermc:patched-spigot-fernflower:0.1+build.6")
    paperclip("io.papermc:paperclip:3.0.3")
}

allprojects {
    apply(plugin = "java")
    apply(plugin = "maven-publish")

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(17))
        }
    }
}

subprojects {
    tasks.withType<JavaCompile> {
        options.encoding = Charsets.UTF_8.name()
        options.release.set(17)
    }
    tasks.withType<Javadoc> {
        options.encoding = Charsets.UTF_8.name()
    }
    tasks.withType<ProcessResources> {
        filteringCharset = Charsets.UTF_8.name()
    }

    repositories {
        mavenCentral()
        maven(paperMavenPublicUrl)
    }
}

paperweight {
    serverProject.set(project(":chiyogami-server"))

    remapRepo.set(paperMavenPublicUrl)
    decompileRepo.set(paperMavenPublicUrl)

    usePaperUpstream(providers.gradleProperty("paperRef")) {
        withPaperPatcher {
            apiPatchDir.set(layout.projectDirectory.dir("patches/api"))
            apiOutputDir.set(layout.projectDirectory.dir("chiyogami-api"))

            serverPatchDir.set(layout.projectDirectory.dir("patches/server"))
            serverOutputDir.set(layout.projectDirectory.dir("chiyogami-server"))
        }
    }
}

//
// Everything below here is optional if you don't care about publishing API or dev bundles to your repository
//

tasks.generateDevelopmentBundle {
    apiCoordinates.set("com.github.bea4dev:chiyogami-api")
    mojangApiCoordinates.set("io.papermc.paper:paper-mojangapi")
    libraryRepositories.set(
        listOf(
            "https://repo.maven.apache.org/maven2/",
            paperMavenPublicUrl,
            // "https://my.repo/", // This should be a repo hosting your API (in this example, 'com.example.paperfork:forktest-api')
        )
    )
}

allprojects {
    // Publishing API:
    // ./gradlew :Chiyogami-API:publish[ToMavenLocal]
    publishing {
        repositories {
            maven {
                name = "myRepoSnapshots"
                url = uri("https://my.repo/")
                // See Gradle docs for how to provide credentials to PasswordCredentials
                // https://docs.gradle.org/current/samples/sample_publishing_credentials.html
                credentials(PasswordCredentials::class)
            }
        }
    }
}

publishing {
    // Publishing dev bundle:
    // ./gradlew publishDevBundlePublicationTo(MavenLocal|MyRepoSnapshotsRepository) -PpublishDevBundle
    if (project.hasProperty("publishDevBundle")) {
        publications.create<MavenPublication>("devBundle") {
            artifact(tasks.generateDevelopmentBundle) {
                artifactId = "dev-bundle"
            }
        }
    }
}
