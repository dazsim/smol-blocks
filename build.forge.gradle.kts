plugins {
    id("net.neoforged.moddev.legacyforge")
    id("neoforge-mutex")
}

val modVersion = providers.gradleProperty("version").orNull ?: sc.properties.get<String>("mod.version")
val mc = sc.current.version
version = modVersion
group = property("mod.group") as String
base.archivesName = "${property("mod.id")}-forge-$mc"

sourceSets.main {
    java.exclude("com/workshopcraft/smolblocks/fabric/**", "com/workshopcraft/smolblocks/neoforge/**", "com/workshopcraft/smolblocks/client/**",
        "com/workshopcraft/smolblocks/legacy/fabric/**", "com/workshopcraft/smolblocks/legacy/neoforge/**")
    resources.setSrcDirs(listOf(rootProject.file("src/legacy/1.20.1"), rootProject.file("src/main/resources")))
}

legacyForge {
    version = property("deps.forge") as String
    mods {
        register(property("mod.id") as String) {
            sourceSet(sourceSets.main.get())
        }
    }
    runs {
        register("client") {
            gameDirectory = rootProject.file("runs/${sc.current.project}")
            client()
        }
        register("server") {
            gameDirectory = rootProject.file("runs/${sc.current.project}-server")
            server()
        }
    }
}

mixin {
    add(sourceSets.main.get(), "smolblocks.refmap.json")
    config("smolblocks.mixins.json")
}

dependencies {
    compileOnly("org.jspecify:jspecify:1.0.0")
    annotationProcessor("org.spongepowered:mixin:0.8.5:processor")
}

java {
    withSourcesJar()
    targetCompatibility = JavaVersion.VERSION_17
    sourceCompatibility = JavaVersion.VERSION_17
    toolchain.languageVersion = JavaLanguageVersion.of(17)
}

tasks {
    processResources {
        val props = mapOf(
            "id" to sc.properties.get<String>("mod.id"),
            "name" to sc.properties.get<String>("mod.name"),
            "version" to modVersion,
            "author" to sc.properties.get<String>("mod.author"),
            "description" to sc.properties.get<String>("mod.description"),
            "minecraft" to sc.properties.get<String>("mod.mc_compat"),
            "java" to "17",
            "mixin_package" to "legacy.client.mixin",
            "mixin_class" to "LiquidBlockRendererMixin",
            "mixin_java" to "17",
            "refmap" to "\n  \"refmap\": \"smolblocks.refmap.json\",",
        )
        inputs.properties(props)
        filesMatching(listOf("META-INF/mods.toml", "pack.mcmeta", "smolblocks.mixins.json")) { expand(props) }
        exclude("fabric.mod.json", "smolblocks.fabric.mixins.json", "META-INF/neoforge.mods.toml", "assets/smolblocks/items/**", "data/smolblocks/recipe/**", "data/smolblocks/tags/block/**")
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }

    named("createMinecraftArtifacts") {
        dependsOn("stonecutterGenerate")
    }

    jar {
        manifest.attributes("MixinConfigs" to "smolblocks.mixins.json")
    }

    withType<Jar> {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        val name = project.property("mod.name")
        inputs.property("mod_name", name)
        from(rootProject.file("LICENSE")) { rename { "${it}_$name" } }
    }
}
