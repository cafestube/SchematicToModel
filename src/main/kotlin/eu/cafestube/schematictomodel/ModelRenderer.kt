package eu.cafestube.schematictomodel

import eu.cafestube.schematics.schematic.Schematic
import net.kyori.adventure.key.Key
import team.unnamed.creative.base.CubeFace
import team.unnamed.creative.base.Vector3Float
import team.unnamed.creative.blockstate.Condition
import team.unnamed.creative.blockstate.MultiVariant
import team.unnamed.creative.blockstate.Variant
import team.unnamed.creative.item.Item
import team.unnamed.creative.item.ItemModel
import team.unnamed.creative.item.tint.TintSource
import team.unnamed.creative.blockstate.BlockState as RPBlockState
import team.unnamed.creative.model.Element
import team.unnamed.creative.model.ElementFace
import team.unnamed.creative.model.ElementRotation
import team.unnamed.creative.model.Model
import team.unnamed.creative.model.ModelTexture
import team.unnamed.creative.model.ModelTextures
import team.unnamed.creative.serialize.minecraft.blockstate.BlockStateSerializer
import team.unnamed.creative.serialize.minecraft.model.ModelSerializer
import team.unnamed.creative.texture.TextureUV
import java.awt.Color
import java.io.ByteArrayInputStream
import kotlin.collections.mutableMapOf

const val SCALE = 1.0F / 16.0F

val grassColored = listOf<String>(
    "minecraft:grass_block",
    "minecraft:fern",
    "minecraft:short_grass",
    "minecraft:potted_fern",
    "minecraft:bush"
)

val grassColoredByTextureId = mapOf(
    "minecraft:pink_petals" to 0,
    "minecraft:wildflowers" to 0
)

val foliageColored = listOf(
    "minecraft:oak_leaves",
    "minecraft:jungle_leaves",
    "minecraft:acacia_leaves",
    "minecraft:dark_oak_leaves",
    "minecraft:vine",
    "minecraft:mangrove_leaves"
)

class ModelRenderer(val schematic: Schematic, val clientResources: ClientResources) {

    private val textures = mutableMapOf<String, Pair<Int, ModelTexture>>()
    private var textureId = 0
    private val tints = mutableMapOf<TintSource, Int>()

    fun getOrCreateTintIndex(color: TintSource): Int {
        if (tints.containsKey(color)) {
            return tints[color]!!
        }

        val index = tints.size
        tints[color] = index
        return index
    }

    fun getOrAddTexture(key: Key): Int {
        if (textures.containsKey(key.value())) {
            return textures[key.value()]!!.first
        }

        textures[key.value()] = textureId to ModelTexture.ofKey(key)
        textureId++
        return textureId - 1
    }

    fun getOrAddTexture(model: Model, reference: String): Int {
        val texture = model.textures().variables()[reference] ?: error("Texture $reference not found in model")

        if(texture.key() != null) {
            return getOrAddTexture(texture.key()!!)
        }

        return getOrAddTexture(model, texture.reference()!!)
    }

    fun getOrAddTexture(model: ModelTextures, reference: String): Int {
        val texture = model.variables()[reference] ?: error("Texture $reference not found in model")

        if(texture.key() != null) {
            return getOrAddTexture(texture.key()!!)
        }

        return getOrAddTexture(model, texture.reference()!!)
    }

    fun buildModel(): Model {
        val elements = mutableListOf<Element>()


        for (x in 0 until schematic.width) {
            for (y in 0 until schematic.height) {
                for (z in 0 until schematic.length) {
                    val block = schematic.getBlockData(x, y, z)

                    if(block == "minecraft:air") {
                        continue
                    }

                    val blockState = parseBlockState(block)

                    val blockId = Key.key(blockState.identifier)

                    val rpState = clientResources.getEntry("assets/minecraft/blockstates/${blockId.value()}.json")?.let {
                        BlockStateSerializer.INSTANCE.deserialize(ByteArrayInputStream(it), Key.key("minecraft:blockstates/${blockId.value()}.json"))
                    }

                    if(rpState != null) {
                        val variant = findVariants(rpState, blockState)

                        variant.forEach { mutliVariants ->
                            val variant = mutliVariants.variants().random()

                            val model = findModel(variant) ?: return@forEach
                            val fullModel = createFullModel(model)

                            if(fullModel.elements().isEmpty()) {
                                if(fullModel.key() == Key.key("minecraft", "models/block/banner.json")) {
                                    if(blockState.properties.contains("facing")) {

                                        val dyeColor = DyeColor.valueOf(blockState.identifier.replace("minecraft:", "").replace("_wall_banner", "").uppercase())
                                        elements.addAll(renderWallBanner(dyeColor, blockState.properties["facing"] ?: "north", x, y, z))
                                    } else {
                                        elements.addAll(renderBanner(blockState.properties["rotation"]?.toInt() ?: 0, x, y, z))
                                    }
                                } else if(fullModel.key() == Key.key("minecraft", "models/block/bed.json")) {
                                    val color = Key.key(blockState.identifier).value().replace("_bed", "")
                                    val texture = Key.key("minecraft", "entity/bed/$color")

                                    elements.addAll(renderBed(texture, x, y, z, blockState.properties["part"] == "foot", blockState.properties["facing"] ?: "north"))
                                } else {
                                    println("No elements found for $blockId ${fullModel.key()}")
                                }
                            } else {
                                //TODO: handle uvlock

                                val tint = if(grassColored.contains(blockState.identifier)) {
                                    TintSource.grass(0.8F, 0.4F) //Plains
                                } else null

                                elements.addAll(fullModel.elements().map {
                                    it.transformElement(fullModel.textures(), SCALE, x.toFloat(), y.toFloat(), z.toFloat(), variant.x(), variant.y(), tint)
                                })
                            }
                        }
                    } else {
                        println("No variant found for $blockId")
                    }
                }
            }
        }

        return Model.model()
            .key(Key.key("cafestube", "generated"))
            .elements(elements)
            .textures(
                ModelTextures.builder()
                    .variables(textures.values.associate { it.first.toString() to it.second })
                    .build()
            )
            .build()
    }

    private fun renderBed(texture: Key, x: Int, y: Int, z: Int, isFoot: Boolean, facing: String): Collection<Element> {
        val bed = getExtraModel("bed_${if(isFoot) "foot" else "head"}")
        val yRot = yRotFromFacing(facing)

        val textures = ModelTextures.builder()
            .addVariable("bed", ModelTexture.ofKey(texture))
            .build()

        return bed.elements()
            .map {
                it.transformElement(textures, SCALE, x.toFloat(), y.toFloat(), z.toFloat(), 0, yRot)
            }
    }

    private fun renderBanner(rotation: Int, x: Int, y: Int, z: Int): Collection<Element> {
        return TODO("Normal banners are not supported yet")
    }

    private fun yRotFromFacing(facing: String): Int {
        return when (facing) {
            "north" -> 0
            "east" -> 90
            "south" -> 180
            "west" -> 270
            else -> error("Invalid facing $facing")
        }
    }

    private fun renderWallBanner(dyeColor: DyeColor, facing: String, x: Int, y: Int, z: Int): Collection<Element> {
        val wallBanner = getExtraModel("wall_banner")
        val yRot = yRotFromFacing(facing)

        return wallBanner.elements()
            .mapIndexed { index, it ->
                val tint = if(index == 1) TintSource.constant(dyeColor.rgb) else null
                it.transformElement(wallBanner.textures(), SCALE, x.toFloat(), y.toFloat(), z.toFloat(), 0, yRot, tint)
            }
    }

    private fun createFullModel(model: Model): Model {
        if(model.parent() == null) return model

        val parent = clientResources.getEntry("assets/minecraft/models/${model.parent()!!.value()}.json")?.let {
            ModelSerializer.INSTANCE.deserialize(ByteArrayInputStream(it), model.parent())
        } ?: return model

        val mergedParent = createFullModel(parent)

        val newModel = Model.model()
            .key(mergedParent.key())
            .parent(mergedParent.parent())

            .textures(
                ModelTextures.builder()
                    .variables(model.textures().variables() + mergedParent.textures().variables())
                    .particle(model.textures().particle() ?: mergedParent.textures().particle())
                    .build()
            )
            .elements(model.elements() + mergedParent.elements())

//            .ambientOcclusion(model.ambientOcclusion()) //Figure out how to handle
            .display(model.display() + mergedParent.display())
            .guiLight(model.guiLight())


            .build()

        return newModel
    }

    fun buildItemModelDef(): Item {

        return Item.item(Key.key("gen"), ItemModel.reference(Key.key("gen"), this.tints.entries.sortedBy { it.value }.map { it.key }))
    }

    private fun getExtraModel(id: String): Model {
        return ModelRenderer::class.java.getResource("/extra_model/$id.json")?.let {
            ModelSerializer.INSTANCE.deserialize(it.openStream(), Key.key("extra_models/$id.json"))
        } ?: error("Could not find extra model $id")
    }

    private fun findModel(variant: Variant): Model? {
        val model = clientResources.getEntry("assets/minecraft/models/${variant.model().value()}.json")?.let {
            ModelSerializer.INSTANCE.deserialize(ByteArrayInputStream(it), Key.key("minecraft:models/${variant.model().value()}.json"))
        }

        if(model != null) {
            return model
        }
        println("No model found for ${variant.model().value()}")
        return null
    }

    private fun findVariants(rpState: RPBlockState, blockState: BlockState): List<MultiVariant> {
        if(rpState.variants().isEmpty()) {
            return rpState.multipart().filter { applies(it.condition(), blockState) }.map { it.variant() }
        }

        val mappedVariants = rpState.variants().mapKeys { parseBlockStateProperties(it.key) }
        val variant = firstMatching(mappedVariants, blockState.properties) ?: rpState.variants()[""]

        if (variant == null) {
            println("No variant found for ${blockState.identifier} with properties ${blockState.properties}")
            return listOf(rpState.variants().entries.first().value)
        }

        return listOf(variant)
    }

    fun firstMatching(mappedVariants: Map<Map<String, String>, MultiVariant>, input: Map<String, String>): MultiVariant? {
        for (entry in mappedVariants.entries) {
            val properties = entry.key
            val variant = entry.value

            if (properties.all { input[it.key] == it.value }) {
                return variant
            }
        }

        return null
    }

    private fun applies(condition: Condition, state: BlockState): Boolean {
        if(condition == Condition.NONE) return true

        if(condition is Condition.And) {
            return condition.conditions().all { applies(it, state) }
        } else if(condition is Condition.Or) {
            return condition.conditions().any { applies(it, state) }
        } else if(condition is Condition.Match) {
            return state.properties[condition.key()] == if(condition.value() is String) condition.value() else condition.value().toString()
        }
        throw IllegalArgumentException("Unknown condition type: ${condition.javaClass}")
    }

    private fun Element.transformElement(textures: ModelTextures, scale: Float, moveX: Float, moveY: Float, moveZ: Float, rotX: Int, rotY: Int, colorTint: TintSource? = null): Element {
        var newFrom: Vector3Float = from().multiply(scale).add(moveX, moveY, moveZ)
        var newTo: Vector3Float = to().multiply(scale).add(moveX, moveY, moveZ)

        val center = Vector3Float(
            moveX + 0.5F,
            moveY + 0.5F,
            moveZ + 0.5F
        )
        newTo = rotate90(newTo, center, rotX, rotY)
        newFrom = rotate90(newFrom, center, rotX, rotY)

        val from = Vector3Float(
            if(newTo.x() < newFrom.x()) newTo.x() else newFrom.x(),
            if(newTo.y() < newFrom.y()) newTo.y() else newFrom.y(),
            if(newTo.z() < newFrom.z()) newTo.z() else newFrom.z()
        )

        val to = Vector3Float(
            if(newTo.x() > newFrom.x()) newTo.x() else newFrom.x(),
            if(newTo.y() > newFrom.y()) newTo.y() else newFrom.y(),
            if(newTo.z() > newFrom.z()) newTo.z() else newFrom.z()
        )

        val tintIndex = colorTint?.let { getOrCreateTintIndex(colorTint) }

        @Suppress("UNNECESSARY_SAFE_CALL") //Even tho rotation is annotated as not-null, rotation can be null
        return Element.element()
            .faces(faces().map { (key, it) ->

                return@map rotate90(key, rotX, rotY) to ElementFace.face()
                    .texture("#" + getOrAddTexture(textures, it.texture().removePrefix("#")))
                    .uv(it.uv0() ?: TextureUV.uv(0.0F, 0.0F, 1.0F, 1.0F))
                    .cullFace(it.cullFace())
                    .rotation(it.rotation())
                    .tintIndex(tintIndex ?: it.tintIndex())
                    .build()

            }.toMap())


            .from(from)
            .to(to)
            .shade(shade())
            .rotation(rotation()?.let { ElementRotation.of(rotation().origin()?.multiply(scale)?.add(moveX, moveY, moveZ), rotation().axis(), rotation().angle(), rotation().rescale()) })
            .build()

    }

    private fun rotate90(cubeFace: CubeFace, rotX: Int, rotY: Int): CubeFace {
        val dir = when (cubeFace) {
            CubeFace.WEST  -> Vector3Float(-1f, 0f, 0f)
            CubeFace.EAST  -> Vector3Float(1f, 0f, 0f)
            CubeFace.DOWN  -> Vector3Float(0f, -1f, 0f)
            CubeFace.UP    -> Vector3Float(0f, 1f, 0f)
            CubeFace.NORTH -> Vector3Float(0f, 0f, -1f)
            CubeFace.SOUTH -> Vector3Float(0f, 0f, 1f)
        }

        val rotated = rotate90(dir, Vector3Float(0f, 0f, 0f), rotX, rotY)

        return when {
            rotated.x() == -1f && rotated.y() == 0f && rotated.z() == 0f -> CubeFace.WEST
            rotated.x() ==  1f && rotated.y() == 0f && rotated.z() == 0f -> CubeFace.EAST
            rotated.x() ==  0f && rotated.y() == -1f && rotated.z() == 0f -> CubeFace.DOWN
            rotated.x() ==  0f && rotated.y() ==  1f && rotated.z() == 0f -> CubeFace.UP
            rotated.x() ==  0f && rotated.y() == 0f && rotated.z() == -1f -> CubeFace.NORTH
            rotated.x() ==  0f && rotated.y() == 0f && rotated.z() ==  1f -> CubeFace.SOUTH
            else -> throw IllegalArgumentException("Invalid rotated vector: $rotated")
        }
    }

    private fun rotate90(point: Vector3Float, center: Vector3Float, rotX: Int, rotY: Int): Vector3Float {
        var x = point.x() - center.x()
        var y = point.y() - center.y()
        var z = point.z() - center.z()

        val rx = ((rotX % 360) + 360) % 360
        val ry = ((rotY % 360) + 360) % 360

        when (rx) {
            90 -> {
                val temp = y
                y = z
                z = -temp
            }
            180 -> {
                y = -y
                z = -z
            }
            270 -> {
                val temp = y
                y = -z
                z = temp
            }
        }

        when (ry) {
            90 -> {
                val temp = x
                x = -z
                z = temp
            }
            180 -> {
                x = -x
                z = -z
            }
            270 -> {
                val temp = x
                x = z
                z = -temp
            }
        }

        return Vector3Float(
            x + center.x(),
            y + center.y(),
            z + center.z()
        )
    }

}