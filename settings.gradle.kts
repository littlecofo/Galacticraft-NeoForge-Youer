pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net/") {
            name = "Fabric"
            content { includeGroupByRegex("net\\.fabricmc.*"); includeGroup("fabric-loom") }
        }
        maven("https://maven.architectury.dev/") {
            name = "Architectury"
            content { includeGroupByRegex("dev\\.architectury.*"); includeGroupByRegex("architectury-plugin.*") }
        }
        maven("https://maven.neoforged.net/releases/") {
            name = "NeoForged"
            content { includeGroupByRegex("net\\.neoforged.*"); includeGroup("net.minecraftforge") }
        }
        maven("https://maven.minecraftforge.net/") {
            content { includeGroup("net.minecraftforge"); includeGroup("de.oceanlabs.mcp") }
        }
        maven("https://libraries.minecraft.net/") {
            content { includeGroup("com.mojang") }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

rootProject.name = "Galacticraft"

include("common")
if (providers.gradleProperty("neoforgeOnly").orNull != "true") {
    include("fabric")
}
include("neoforge")
