package architecture.resonator_combat_framework.module.entity_animation.animation.data

import architecture.goldenboughs_lib.api.AllOpe
import architecture.resonator_combat_framework.module.entity_animation.animation.model.BakingBrModel
import architecture.resonator_combat_framework.module.entity_animation.registry.BedrockModelRegistry
import com.google.gson.JsonElement
import com.google.gson.JsonObject

// 骨骼配置数据。包含过渡时间、骨骼标志、时间线
@ExposedCopyVisibility
@AllOpe
data class ProxyBoneConfigData
private constructor(
	val bones: Map<String, ProxyBoneFlags> = DEFAULT_FLAGS,
	val timeline: List<ProxyTimelineEntry> = emptyList(),
	val transitionTicks: Int = DEFAULT_TRANSITION_TICKS,
	private val fadeInTicks: Int = -1,
	private val fadeOutTicks: Int = -1,
	/** 额外模型数据定义（动画期间动态添加的模型数据） */
	val extraModel: BakingBrModel? = null
) {
	companion object {
		@JvmField
		val DEFAULT_FLAGS = mapOf("head" to ProxyBoneFlags(mapOf("lock" to false)))

		@JvmField
		val EMPTY = create(emptyMap(), emptyList(), DEFAULT_TRANSITION_TICKS)

		const val DEFAULT_TRANSITION_TICKS: Int = 3

		fun create(
			bones: Map<String, ProxyBoneFlags>,
			timeline: List<ProxyTimelineEntry>,
			transitionTicks: Int,
			fadeInTicks: Int = -1,
			fadeOutTicks: Int = -1,
			/** 额外模型数据定义（动画期间动态添加的模型数据） */
			extraModel: BakingBrModel? = null
		): ProxyBoneConfigData {
			val merged = mutableMapOf<String, ProxyBoneFlags>().apply {
				putAll(DEFAULT_FLAGS)
				putAll(bones)
			}
			return ProxyBoneConfigData(merged, timeline, transitionTicks, fadeInTicks, fadeOutTicks, extraModel)
		}

		/** 从 JSON 解析骨骼配置 */
		@JvmStatic
		fun parse(json: JsonObject, isClient: Boolean): ProxyBoneConfigData {
			val transitionTicks = json.get("transition")?.asInt ?: DEFAULT_TRANSITION_TICKS
			val fadeInTicks = json.get("fade_in")?.asInt ?: -1
			val fadeOutTicks = json.get("fade_out")?.asInt ?: -1
			val bones = parseBonesSection(json.get("bones"))

			val timelineJson = json.getAsJsonObject("timeline")
			val timeline = if (timelineJson != null) {
				val list = mutableListOf<ProxyTimelineEntry>()
				for ((timeRange, data) in timelineJson.entrySet()) {
					val parts = timeRange.split("-")
					val entry = data.asJsonObject
					list.add(
						ProxyTimelineEntry(
							from = parts.getOrNull(0)?.toFloatOrNull() ?: 0f,
							to = parts.getOrNull(1)?.toFloatOrNull() ?: 0f,
							bones = parseBonesSection(entry.get("bones"))
						)
					)
				}
				list
			} else emptyList()

			// 解析额外骨骼定义（动画期间动态添加的 BrBone 几何数据）
			val extraModel = json.get("extra_model_id")?.run {
				BedrockModelRegistry.getInstance(isClient).get(asString)
			} ?: json.get("extra_model")?.run {
				BakingBrModel.parses(this).firstOrNull()
			}

			return create(bones, timeline, transitionTicks, fadeInTicks, fadeOutTicks, extraModel)
		}

		private fun parseBonesSection(section: JsonElement?): Map<String, ProxyBoneFlags> {
			if (section == null || !section.isJsonObject) return emptyMap()
			val obj = section.asJsonObject
			val result = mutableMapOf<String, ProxyBoneFlags>()
			for ((boneName, boneElement) in obj.entrySet()) {
				if (!boneElement.isJsonObject) continue
				val boneObj = boneElement.asJsonObject
				result[boneName] = ProxyBoneFlags(flattenBoneFlags(boneObj))
			}
			return result
		}

		private fun flattenBoneFlags(obj: JsonObject): Map<String, Boolean> {
			val flat = mutableMapOf<String, Boolean>()
			for ((key, value) in obj.entrySet()) {
				when {
					value.isJsonPrimitive -> flat[key] = value.asBoolean
					value.isJsonObject -> {
						for ((subKey, subValue) in value.asJsonObject.entrySet()) {
							if (subValue.isJsonPrimitive) flat["$key.$subKey"] = subValue.asBoolean
						}
					}
				}
			}
			return flat
		}
	}

	/** 获取淡入 tick 数，未设置时退化为 transitionTicks */
	fun getFadeInTicks(): Int = if (fadeInTicks >= 0) fadeInTicks else transitionTicks

	/** 获取淡出 tick 数，未设置时退化为 transitionTicks */
	fun getFadeOutTicks(): Int = if (fadeOutTicks >= 0) fadeOutTicks else transitionTicks

	/** 根据当前动画时间合并基础配置和时间线配置 */
	fun resolveBoneFlags(animTime: Float): Map<String, ProxyBoneFlags> {
		val result = bones.toMutableMap()
		for (entry in timeline) {
			if (animTime >= entry.from && animTime < entry.to) {
				result.putAll(entry.bones)
			}
		}
		return result
	}

	/** 获取当前动画涉及的所有骨骼名（包括时间线中的） */
	fun resolveCurrentBoneNames(): Set<String> {
		val names = bones.keys.toMutableSet()
		for (entry in timeline) {
			names.addAll(entry.bones.keys)
		}
		return names
	}
}
