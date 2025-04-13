package eu.cafestube.schematictomodel

import eu.cafestube.schematics.schematic.Schematic
import net.kyori.adventure.key.Key
import team.unnamed.creative.base.CubeFace
import team.unnamed.creative.model.Element
import team.unnamed.creative.model.ElementFace
import team.unnamed.creative.model.Model
import team.unnamed.creative.model.ModelTexture
import team.unnamed.creative.model.ModelTextures
import team.unnamed.creative.texture.Texture
import team.unnamed.creative.texture.TextureUV

fun Schematic.toModel(): Model {
    val elements = mutableListOf<Element>()

    val textures = mutableMapOf<String, Pair<Int, ModelTexture>>()
    var textureId = 0

    for (x in 0 until width) {
        for (y in 0 until height) {
            for (z in 0 until length) {
                val block = getBlockData(x, y, z)

                if(block == "minecraft:air") {
                    continue
                }

                val blockState = parseBlockState(block)

                if(!textures.containsKey(blockState.identifier)) {
                    textures[blockState.identifier] = textureId to ModelTexture.ofKey(
                        Key.key("minecraft:block/${Key.key(blockState.identifier).value()}"),
                    )
                    textureId++
                }

                val id = textures[blockState.identifier]!!.first

                elements.add(
                    Element.element()
                        .from(x.toFloat(), y.toFloat(), z.toFloat())
                        .to(x.toFloat() + 1, y.toFloat() + 1, z.toFloat() + 1)
                        .addFace(CubeFace.UP, ElementFace.face()
                            .texture("#${id}")
                            .uv(TextureUV.uv(0F, 0F, 1F, 1F)) // use the full texture
                            .build())
                        .addFace(CubeFace.DOWN, ElementFace.face()
                            .texture("#${id}")
                            .uv(TextureUV.uv(0F, 0F, 1F, 1F)) // use the full texture
                            .build())
                        .addFace(CubeFace.EAST, ElementFace.face()
                            .texture("#${id}")
                            .uv(TextureUV.uv(0F, 0F, 1F, 1F)) // use the full texture
                            .build())
                        .addFace(CubeFace.NORTH, ElementFace.face()
                            .texture("#${id}")
                            .uv(TextureUV.uv(0F, 0F, 1F, 1F)) // use the full texture
                            .build())
                        .addFace(CubeFace.SOUTH, ElementFace.face()
                            .texture("#${id}")
                            .uv(TextureUV.uv(0F, 0F, 1F, 1F)) // use the full texture
                            .build())
                        .addFace(CubeFace.WEST, ElementFace.face()
                            .texture("#${id}")
                            .uv(TextureUV.uv(0F, 0F, 1F, 1F)) // use the full texture
                            .build())
                        .build()
                )


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