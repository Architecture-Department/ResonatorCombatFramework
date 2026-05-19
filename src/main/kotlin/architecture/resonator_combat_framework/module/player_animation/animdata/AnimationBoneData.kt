package architecture.resonator_combat_framework.module.player_animation.animdata

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import net.minecraft.client.Minecraft
import net.minecraft.resources.ResourceLocation

typealias BoneFlags = Map<String, Boolean>

data class TimelineEntry(
	val from: Float,
	val to: Float,
	val bones: Map<String, BoneFlags>
)

data class AnimationBoneData(
	val bones: Map<String, BoneFlags> = emptyMap(),
	val timeline: List<TimelineEntry> = emptyList()
) {
	// 给定动画时间，解析当前生效的骨骼标记（合并根级+时间轴）
	fun resolveBoneStates(animTime: Float): Map<String, Set<String>> {
		val result = mutableMapOf<String, MutableSet<String>>()
		// 根级
		val rootEntries = this.bones.entries
		for (entry in rootEntries) {
			val bone = entry.key
			val flags = entry.value
			val set = result.getOrPut(bone) { mutableSetOf() }
			for (f in flags) {
				if (f.value) set.add(f.key)
			}
		}
		// 时间轴
		for (tl in this.timeline) {
			if (animTime < tl.from || animTime >= tl.to) continue
			for (bEntry in tl.bones.entries) {
				val bone = bEntry.key
				val flags = bEntry.value
				val set = result.getOrPut(bone) { mutableSetOf() }
				for (f in flags) {
					if (f.value) set.add(f.key)
				}
			}
		}
		return result
	}

	companion object {
		private val GSON = Gson()

		fun load(animId: String): AnimationBoneData {
			val rl = ResourceLocation.fromNamespaceAndPath(
				"resonator_combat_framework", "eyelib/animdata/$animId"
			)
			val mc = Minecraft.getInstance()
			val manager = mc.resourceManager
			val opt = manager.getResource(rl.withSuffix(".json"))
			if (opt.isEmpty) return AnimationBoneData()
			val resource = opt.get()
			val reader = resource.openAsReader()
			val type = object : TypeToken<Map<String, Any>>() {}.type
			val raw: Map<String, Any> = GSON.fromJson(reader, type)
			reader.close()
			return parse(raw)
		}

		@Suppress("UNCHECKED_CAST")
		private fun parse(raw: Map<String, Any>): AnimationBoneData {
			val bones = parseBonesSection(raw["bones"])
			val timelineSection = raw["timeline"] as? Map<String, Any> ?: return AnimationBoneData(bones, emptyList())
			val timeline = mutableListOf<TimelineEntry>()
			for ((timeRange, data) in timelineSection.entries) {
				val parts = timeRange.split("-")
				val dataMap = data as? Map<String, Any> ?: continue
				timeline.add(
					TimelineEntry(
						from = parts.getOrNull(0)?.toFloatOrNull() ?: 0f,
						to = parts.getOrNull(1)?.toFloatOrNull() ?: 0f,
						bones = parseBonesSection(dataMap["bones"])
					)
				)
			}
			return AnimationBoneData(bones, timeline)
		}

		@Suppress("UNCHECKED_CAST")
		private fun parseBonesSection(section: Any?): Map<String, BoneFlags> {
			if (section !is Map<*, *>) return emptyMap()
			val result = mutableMapOf<String, BoneFlags>()
			for ((boneName, flagsRaw) in section.entries) {
				val flagMap = flagsRaw as? Map<String, Any> ?: continue
				val flags = mutableMapOf<String, Boolean>()
				for ((flagName, flagValue) in flagMap.entries) {
					flags[flagName] = flagValue == true || flagValue == 1.0 || flagValue == "true"
				}
				result[boneName as String] = flags
			}
			return result
		}
	}
}
