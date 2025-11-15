package eu.cafestube.schematictomodel

import eu.cafestube.schematics.SchematicIO
import org.apache.commons.cli.*
import team.unnamed.creative.serialize.minecraft.item.ItemSerializer
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
    val itemOutput = Option.builder("io").argName("item-output")
        .longOpt("item-output").hasArg().type(Path::class.java).converter(Converter.PATH).required()
        .desc("The item-model output").build()
    val scale = Option.builder("s").argName("scale")
        .longOpt("scale").hasArg().type(Float::class.java).converter(Converter.NUMBER).required(false)
        .desc("scaling factor (default: 1.0F)").build()

    options.addOption(input)
    options.addOption(output)
    options.addOption(itemOutput)
    options.addOption(scale)

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

    val scaleValue = cmd.getParsedOptionValue<Number>(scale)?.toFloat() ?: 1.0F
    println("Using scale $scaleValue")
    val modelRenderer = ModelRenderer(schematic, ClientResources(), scaleValue)
    val model = modelRenderer.buildModel()

    FileOutputStream(modelOutputPath.toFile()).use { outputStream ->
        ModelSerializer.INSTANCE.serialize(model, outputStream, 71)
    }

    val itemModelOutputPath = cmd.getParsedOptionValue<Path>(itemOutput)
    val itemModel = modelRenderer.buildItemModelDef()
    FileOutputStream(itemModelOutputPath.toFile()).use { outputStream ->
        ItemSerializer.INSTANCE.serialize(itemModel, outputStream, 71)
    }
}