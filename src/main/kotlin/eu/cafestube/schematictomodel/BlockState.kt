package eu.cafestube.schematictomodel

fun parseBlockState(blockData: String): BlockState {
    val data = blockData.split("[")

    if(data.size == 1)
        return BlockState(data[0], emptyMap())

    val dataStr = data[1].substring(0, data[1].indexOf("]"))
    val dataMap = parseBlockStateProperties(dataStr)


    return BlockState(data[0], dataMap)
}

fun parseBlockStateProperties(properties: String): Map<String, String> {
    val dataMap = mutableMapOf<String, String>()

    properties.split(",").forEach {
        val split = it.split("=")
        if(split.size < 2) return@forEach

        dataMap[split[0]] = split[1]
    }

    return dataMap
}

data class BlockState(val identifier: String, val properties: Map<String, String>) {

    override fun toString(): String {
        if(properties.isEmpty())
            return identifier
        return "$identifier[${properties.map { "${it.key}=${it.value}" }.joinToString(",")}]"
    }

}