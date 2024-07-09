import net.fabricmc.loader.impl.metadata.*
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

val versionMc: String by rootProject
val versionForge: String by rootProject

val sourceSets = extensions.getByType<SourceSetContainer>()

// Source sets that can contain the mod entrypoint file
val masterSourceSets = listOf("main", "testmod").mapNotNull(sourceSets::findByName).filter { it.java.srcDirs.any(File::exists) || it.resources.srcDirs.any(File::exists) }

masterSourceSets.forEach { sourceSet ->
    val modMetadataJson = sourceSet.java.srcDirs.map { it.parentFile.resolve("resources/fabric.mod.json") }.firstOrNull(File::exists) ?: return@forEach
    val baseTaskName = "ForgeModEntrypoint"
    val taskName = sourceSet.getTaskName("generate", baseTaskName)
    val targetDir = project.file("src/generated/${sourceSet.name}/java")
    val task = tasks.register(taskName, GenerateForgeModEntrypoint::class.java) {
        group = "sinytra"
        description = "Generates entrypoint files for ${sourceSet.name} fabric mod."
        project.tasks.findByName(sourceSet.getTaskName("generate", "ImplPackageInfos"))?.let { mustRunAfter(it) }

        // Only apply to default source directory since we also add the generated
        // sources to the source set.
        sourceRoots.from(sourceSet.java.srcDirs)
        outputDir.set(targetDir)
        fabricModJson.set(modMetadataJson)
        testEnvironment = sourceSet.name == "testmod"
        includeVersion.set(project.parent?.name == "deprecated")
    }
    sourceSet.java.srcDir(task)
    val cleanTask = tasks.register(sourceSet.getTaskName("clean", baseTaskName), Delete::class.java) {
        group = "sinytra"
        delete(file("src/generated/${sourceSet.name}/java"))
        project.tasks.findByName(sourceSet.getTaskName("clean", "ImplPackageInfos"))?.let { mustRunAfter(it) }
    }
    tasks.named("clean") {
        dependsOn(cleanTask)
    }
    tasks.named("generate") {
        dependsOn(task)
    }
}

abstract class GenerateForgeModEntrypoint : DefaultTask() {
    @get:SkipWhenEmpty
    @get:InputFiles
    val sourceRoots: ConfigurableFileCollection = project.objects.fileCollection()

    @get:InputFile
    val fabricModJson: RegularFileProperty = project.objects.fileProperty()

    @get:Input
    val includeVersion: Property<Boolean> = project.objects.property(Boolean::class)

    @get:Input
    val testEnvironment: Property<Boolean> = project.objects.property(Boolean::class)

    @get:OutputDirectory
    val outputDir: DirectoryProperty = project.objects.directoryProperty()

    private val projectNamePattern = "^fabric_(.+?)(?:_v\\d)?\$".toRegex()
    private val projectVersionNamePattern = "^fabric_(.+?_v\\d)?\$".toRegex()

    @TaskAction
    fun run() {
        val modMetadata = parseModMetadata(fabricModJson.asFile.get())
        val modid = normalizeModid(modMetadata.id)

        val className = "GeneratedEntryPoint"
        val packageName = packageNameForEntryPoint(modid, includeVersion.get())
        val packagePath = packageName.replace('/', '.')
        val packageDir = outputDir.file(packagePath).get().asFile.toPath()
        packageDir.createDirectories()
        val destFile = packageDir.resolve("$className.java")

        val commonEntrypoints =
            modMetadata.getEntrypoints("main").map(EntrypointMetadata::getValue).filter(::entryPointExists)
                .map { "new $it().onInitialize();" }
        val clientEntrypoints =
            modMetadata.getEntrypoints("client").map(EntrypointMetadata::getValue).filter(::entryPointExists)
                .map { "new $it().onInitializeClient();" }
        val serverEntrypoints =
            modMetadata.getEntrypoints("server").map(EntrypointMetadata::getValue).filter(::entryPointExists)
                .map { "new $it().onInitializeServer();" }
        val separator = "\n                    "
        val nestedSeparator = "\n                        "

        val commonEntrypointInit = if (commonEntrypoints.isNotEmpty()) {
            """// Initialize main entrypoints
                    ${commonEntrypoints.joinToString(separator)}"""
        } else ""
        val clientEntrypointInit = if (clientEntrypoints.isNotEmpty()) {
            """
                    // Initialize client entrypoints
                    if (net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) {
                        ${clientEntrypoints.joinToString(nestedSeparator)}
                    }"""
        } else ""
        val serverEntrypointInit = if (serverEntrypoints.isNotEmpty()) {
            """
                    // Initialize server entrypoints
                    if (net.neoforged.fml.loading.FMLEnvironment.dist.isDedicatedServer()) {
                        ${serverEntrypoints.joinToString(nestedSeparator)}
                    }"""
        } else ""
        val entrypointInitializers = listOf(commonEntrypointInit, clientEntrypointInit, serverEntrypointInit)
            .filter(String::isNotEmpty)
            .joinToString(separator = separator)
        val testEnvSetup = if (testEnvironment.get())
            """// Setup test environment
                    net.neoforged.neoforge.registries.GameData.unfreezeData();$separator"""
        else ""

        val template = """
            package $packageName;
            
            @net.neoforged.fml.common.Mod($className.MOD_ID)
            public class $className {
                public static final String MOD_ID = "$modid";  
                public static final String RAW_MOD_ID = "${modMetadata.id}";  
            
                public $className(net.neoforged.bus.api.IEventBus bus) {
                    $testEnvSetup$entrypointInitializers
                    ${addGametests(modMetadata)}
                    ${addDatagen(modMetadata)}
                }
            }
        """.trimIndent()

        destFile.writeText(template)
    }

    private fun addGametests(modMetadata: LoaderModMetadata): String {
        val entrypoints = modMetadata.getEntrypoints("fabric-gametest").map(EntrypointMetadata::getValue).takeIf { it.isNotEmpty() } ?: return ""
        val lines = entrypoints.joinToString(separator = "\n                        ") { "event.register(cpw.mods.modlauncher.api.LambdaExceptionUtils.uncheck(() -> Class.forName(\"$it\")));" }
        return """bus.addListener(net.neoforged.neoforge.event.RegisterGameTestsEvent.class, event -> {
                        $lines
                    });"""
    }

    private fun addDatagen(modMetadata: LoaderModMetadata): String {
        val entrypoints = modMetadata.getEntrypoints("fabric-datagen").map(EntrypointMetadata::getValue).takeIf { it.isNotEmpty() } ?: return ""
        return entrypoints.joinToString(separator = "\n                        ") {
            "bus.addListener(net.neoforged.neoforge.data.event.GatherDataEvent.class, event -> net.fabricmc.fabric.impl.datagen.FabricDataGenHelper.runDatagenForMod(MOD_ID, RAW_MOD_ID, new $it(), event));"
        }
    }

    private fun packageNameForEntryPoint(modid: String, includeVersion: Boolean): String {
        val uniqueName = (if (includeVersion) projectVersionNamePattern else projectNamePattern).find(modid)?.groups?.get(1)?.value
            ?: throw RuntimeException("Unable to determine generated package name for mod $modid")
        return "org.sinytra.fabric.$uniqueName.generated"
    }

    private fun entryPointExists(path: String): Boolean {
        return sourceRoots.any { root -> root.resolve(path.replace('.', '/') + ".java").exists() }
    }

    private fun normalizeModid(modid: String): String {
        return modid.replace('-', '_')
    }

    private fun parseModMetadata(file: File): LoaderModMetadata {
        return file.inputStream().use {
            ModMetadataParser.parseMetadata(
                it,
                "",
                listOf(),
                VersionOverrides(),
                DependencyOverrides(project.file("nonexistent").toPath()),
                false
            )
        }
    }
}