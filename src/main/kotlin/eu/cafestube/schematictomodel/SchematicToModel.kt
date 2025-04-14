package eu.cafestube.schematictomodel

import eu.cafestube.schematics.SchematicIO
import org.apache.commons.cli.CommandLine
import org.apache.commons.cli.Converter
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.HelpFormatter
import org.apache.commons.cli.Option
import org.apache.commons.cli.Options
import team.unnamed.creative.serialize.ResourcePackWriter
import team.unnamed.creative.serialize.minecraft.model.ModelSerializer
import java.io.FileOutputStream
import java.nio.file.Path
import java.text.ParseException
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val options = Options()

    val input = Option.builder("i").argName("input")
        .longOpt("input").hasArg().type(Path::class.java).converter(Converter.PATH).required()
        .desc("input file path").build()
    val output = Option.builder("o").argName("output")
        .longOpt("output").hasArg().type(Path::class.java).converter(Converter.PATH).required()
        .desc("The model output").build()

    options.addOption(input)
    options.addOption(output)

    val cmd: CommandLine = try {
        DefaultParser().parse(options, args)
    } catch (e: ParseException) {
        println(e.message)
        HelpFormatter().printHelp("schematic-to-model", options)

        exitProcess(1)
        return
    }
    val inputPath = cmd.getParsedOptionValue<Path>(input)
    val modelOutputPath = cmd.getParsedOptionValue<Path>(output)

    val schematic = SchematicIO.parseSchematic(inputPath.toFile())
    val modelRenderer = ModelRenderer(schematic, ClientResources())
    val model = modelRenderer.buildModel()

    FileOutputStream(modelOutputPath.toFile()).use { outputStream ->
        ModelSerializer.INSTANCE.serialize(model, outputStream)
    }
}