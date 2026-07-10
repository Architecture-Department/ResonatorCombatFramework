package architecture.resonator_combat_framework.module.animation.data

import architecture.goldenboughs_lib.api.AllOpe
import architecture.goldenboughs_lib.util.LibUtil.rlOf
import architecture.resonator_combat_framework.module.animation.util.MirrorUtil
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import net.minecraft.resources.ResourceLocation

/**
 * 骨骼配置数据。包含过渡时间、骨骼标志、时间线等动画行为配置。
 *
 * 用于控制动画播放时的骨骼混合行为、淡入淡出时间以及动画期间的骨骼标志动态切换。
 */
@ExposedCopyVisibility
@AllOpe
data class BoneConfig
private constructor(
	/** 骨骼名到标志配置的映射 */
	val bones: Map<String, BoneFlags> = DEFAULT_FLAGS,
	/** 动画时间线配置，在不同时间段动态覆盖骨骼标志 */
	val timeline: List<TimelineEntry> = emptyList(),
	/** 默认过渡时间（秒，淡入/淡出的备用值） */
	val transitionTime: Float = DEFAULT_TRANSITION_TIME,
	private val fadeInTime: Float = -1f,
	private val fadeOutTime: Float = -1f,
	/** 额外模型数据定义（动画期间动态添加的模型数据） */
	val extraModelId: ResourceLocation? = null
) {
	companion object {

		@JvmField
		val DEFAULT_FLAGS = mapOf("head" to BoneFlags(mapOf("lock" to false)))

		@JvmField
		val EMPTY = of(emptyMap(), emptyList(), DEFAULT_TRANSITION_TIME)

		const val DEFAULT_TRANSITION_TIME: Float = 3f / 20f

		/**
		 * 创建 [BoneConfig] 实例，自动合并默认骨骼标志。
		 *
		 * @param bones 骨骼标志映射
		 * @param timeline 时间线条目
		 * @param transitionTime 默认过渡时间（秒）
		 * @param fadeInTime 淡入时间（秒，-1 使用 transitionTime）
		 * @param fadeOutTime 淡出时间（秒，-1 使用 transitionTime）
		 * @param extraModelId 额外模型 ID
		 * @return 合并了默认标志的 [BoneConfig]
		 */
		@JvmStatic
		fun of(
			bones: Map<String, BoneFlags>,
			timeline: List<TimelineEntry>,
			transitionTime: Float,
			fadeInTime: Float = -1f,
			fadeOutTime: Float = -1f,
			extraModelId: ResourceLocation? = null
		): BoneConfig {
			val merged = mutableMapOf<String, BoneFlags>().apply {
				putAll(DEFAULT_FLAGS)
				putAll(bones)
			}
			return BoneConfig(merged, timeline, transitionTime, fadeInTime, fadeOutTime, extraModelId)
		}

		/** 从 JSON 解析骨骼配置 */
		@JvmStatic
		fun parse(json: JsonObject): BoneConfig {
			val transitionTime = (json.get("transition")?.asInt ?: 3) / 20f
			val fadeInTime = (json.get("fade_in")?.asInt ?: -1) / 20f
			val fadeOutTime = (json.get("fade_out")?.asInt ?: -1) / 20f
			val bones = parseBonesSection(json.get("bones"))

			val timelineJson = json.getAsJsonObject("timeline")
			val timeline = if (timelineJson == null) emptyList() else {
				val list = mutableListOf<TimelineEntry>()
				for ((timeRange, data) in timelineJson.entrySet()) {
					val parts = timeRange.split("-")
					val entry = data.asJsonObject
					list.add(
						TimelineEntry(
							from = parts.getOrNull(0)?.toFloatOrNull() ?: 0f,
							to = parts.getOrNull(1)?.toFloatOrNull() ?: 0f,
							bones = parseBonesSection(entry.get("bones"))
						)
					)
				}
				list
			}

			// 解析额外骨骼定义（动画期间动态添加的 BrBone 几何数据）
			val extraModel = json.get("extra_model_id")?.asString?.let { rlOf(it) }

			return of(bones, timeline, transitionTime, fadeInTime, fadeOutTime, extraModel)
		}

		private fun parseBonesSection(section: JsonElement?): Map<String, BoneFlags> {
			if (section == null || !section.isJsonObject) return emptyMap()
			val obj = section.asJsonObject
			val result = mutableMapOf<String, BoneFlags>()
			for ((boneName, boneElement) in obj.entrySet()) {
				if (!boneElement.isJsonObject) continue
				val boneObj = boneElement.asJsonObject
				result[boneName] = BoneFlags(flattenBoneFlags(boneObj))
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

	/** 获取淡入 tick 数，未设置时退化为 transitionTime */
	fun getFadeInTime(): Float = if (fadeInTime >= 0f) fadeInTime else transitionTime

	/** 获取淡出 tick 数，未设置时退化为 transitionTime */
	fun getFadeOutTime(): Float = if (fadeOutTime >= 0f) fadeOutTime else transitionTime

	/** 根据当前动画时间合并基础配置和时间线配置 */
	fun resolveBoneFlags(animTime: Float): Map<String, BoneFlags> {
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

	/** 返回镜像后的配置（交换左右骨骼名） */
	fun mirror(): BoneConfig {
		val mirroredBones = bones.mapKeys { MirrorUtil.mirrorBoneName(it.key) }
		val mirroredTimeline = timeline.map { entry ->
			entry.copy(bones = entry.bones.mapKeys { MirrorUtil.mirrorBoneName(it.key) })
		}
		return copy(bones = mirroredBones, timeline = mirroredTimeline)
	}

	/** 与另一个配置合并（[other] 覆盖 [this]） */
	fun merge(other: BoneConfig): BoneConfig {
		val mergedBones = bones.toMutableMap()
		mergedBones.putAll(other.bones)
		val mergedTimeline = timeline + other.timeline
		return of(
			bones = mergedBones,
			timeline = mergedTimeline,
			transitionTime = other.transitionTime.takeIf { it != DEFAULT_TRANSITION_TIME } ?: transitionTime,
			fadeInTime = other.fadeInTime.takeIf { it >= 0f } ?: this.fadeInTime,
			fadeOutTime = other.fadeOutTime.takeIf { it >= 0f } ?: this.fadeOutTime,
			extraModelId = other.extraModelId ?: this.extraModelId,
		)
	}
}
