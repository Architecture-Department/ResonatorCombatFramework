// Bedrock 模型(geo.json)解析器。解析 minecraft:geometry 格式，提取骨骼和立方体几何数据（不含 UV）
package architecture.resonator_combat_framework.module.player_animation.bedrock.model

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.joml.Vector3f

/**
 * Bedrock 模型（geo.json）解析器。
 * 不依赖任何外部库，只依赖 Gson（Minecraft 自带）。
 */
object BedrockModelParser {

	/**
	 * 解析单个 geo.json 文件内容，返回其中包含的所有模型。
	 */
	fun parse(jsonString: String): List<BedrockModel> {
		return parse(JsonParser.parseString(jsonString))
	}

	/**
	 * 从 JsonElement 解析模型。
	 */
	fun parse(element: JsonElement): List<BedrockModel> {
		val root = element.asJsonObject
		val geometryArray = root.getAsJsonArray("minecraft:geometry") ?: return emptyList()

		val models = mutableListOf<BedrockModel>()
		for (geoEl in geometryArray) {
			val geoObj = geoEl.asJsonObject
			val model = parseGeometry(geoObj)
			if (model != null) models.add(model)
		}
		return models
	}

	private fun parseGeometry(geo: JsonObject): BedrockModel? {
		val desc = geo.getAsJsonObject("description") ?: return null
		val identifier = desc.get("identifier")?.asString ?: return null
		val bonesJson = geo.getAsJsonArray("bones") ?: return null

		val bones = mutableListOf<BedrockBone>()
		for (boneEl in bonesJson) {
			val bone = parseBone(boneEl.asJsonObject)
			if (bone != null) bones.add(bone)
		}
		return BedrockModel(identifier, bones)
	}

	private fun parseBone(boneObj: JsonObject): BedrockBone? {
		val name = boneObj.get("name")?.asString ?: return null
		val parent = boneObj.get("parent")?.asString
		val pivot = parseVector3f(boneObj.get("pivot"))
		val rotation = parseVector3f(boneObj.get("rotation"))
		val cubesJson = boneObj.getAsJsonArray("cubes")

		val cubes = cubesJson?.mapNotNull { parseCube(it.asJsonObject) } ?: emptyList()

		return BedrockBone(
			name = name,
			parent = parent,
			pivot = pivot ?: Vector3f(),
			rotation = rotation ?: Vector3f(),
			cubes = cubes
		)
	}

	private fun parseCube(cubeObj: JsonObject): BedrockCube? {
		val origin = parseVector3f(cubeObj.get("origin")) ?: return null
		val size = parseVector3f(cubeObj.get("size")) ?: return null
		val pivot = parseVector3f(cubeObj.get("pivot")) ?: Vector3f()
		val rotation = parseVector3f(cubeObj.get("rotation")) ?: Vector3f()
		val inflate = cubeObj.get("inflate")?.asFloat ?: 0f
		val mirror = cubeObj.get("mirror")?.asBoolean ?: false

		return BedrockCube(
			origin = origin,
			size = size,
			pivot = pivot,
			rotation = rotation,
			inflate = inflate,
			mirror = mirror
		)
	}

	/** 从 JSON 数组 [x, y, z] 解析 Vector3f，null 表示不存在 */
	private fun parseVector3f(element: JsonElement?): Vector3f? {
		if (element == null || !element.isJsonArray) return null
		val arr = element.asJsonArray
		if (arr.size() < 3) return null
		return Vector3f(arr[0].asFloat, arr[1].asFloat, arr[2].asFloat)
	}
}

