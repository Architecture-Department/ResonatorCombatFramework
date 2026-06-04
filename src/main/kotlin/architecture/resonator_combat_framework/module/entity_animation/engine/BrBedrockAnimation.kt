package architecture.resonator_combat_framework.module.entity_animation.engine

import architecture.resonator_combat_framework.core.RcfConstants
import architecture.resonator_combat_framework.module.entity_animation.engine.molang.MathParser
import architecture.resonator_combat_framework.module.entity_animation.engine.molang.MolangValue
import com.google.gson.JsonObject

data class BrBedrockAnimation(
	val animId: String,
	/** 播放模式：ONCE / LOOP / HOLD_ON_LAST */
	val loop: LoopType = LoopType.ONCE,
	/** 动画总时长（秒） */
	val length: Float,
	/** 骨骼名 → 骨骼动画数据 */
	val bones: Map<String, BrBoneAnimation> = emptyMap(),
	val sounds: List<BrAnimationSound> = emptyList(),
	val particles: List<BrAnimationParticle> = emptyList(),
	val timelines: List<BrAnimationTimeline> = emptyList(),
	/** MoLang 时间推进表达式，默认 query.anim_time + query.delta_time */
	val animTimeUpdate: MolangValue? = null
) {
	enum class LoopType { ONCE, LOOP, HOLD_ON_LAST }

	companion object {
		/** 解析 JSON 根对象的 "animations" 段，每个条目注册为一个 BedrockAnimation */
		@JvmStatic
		fun parses(
			root: JsonObject,
			side: String = "?",
			exprCache: MutableMap<String, MolangValue> = mutableMapOf()
		): MutableMap<String, BrBedrockAnimation> {
			val result = mutableMapOf<String, BrBedrockAnimation>()
			val animations = root.getAsJsonObject("animations") ?: return result
			for ((animKey, animEl) in animations.entrySet()) {
				val animId = animKey
				val animDef = animEl.asJsonObject ?: continue

				val loopEl = animDef.get("loop")
				val loop = when {
					loopEl?.isJsonPrimitive == true && loopEl.asBoolean -> LoopType.LOOP
					loopEl?.asString == "hold_on_last_frame" -> LoopType.HOLD_ON_LAST
					else -> LoopType.ONCE
				}

				val bonesJson = animDef.getAsJsonObject("bones")
				val bones = if (bonesJson != null)
					BrBoneAnimation.parses(bonesJson) else emptyMap()

				val soundsJson = animDef.getAsJsonObject("sound_effects")
				val sounds = if (soundsJson != null)
					BrAnimationSound.parses(soundsJson) else emptyList()

				val particlesJson = animDef.getAsJsonObject("particle_effects")
				val particles = if (particlesJson != null)
					BrAnimationParticle.parses(particlesJson) else emptyList()

				val timelinesJson = animDef.getAsJsonObject("timeline")
				val timelines = if (timelinesJson != null)
					BrAnimationTimeline.parses(timelinesJson) else emptyList()

				val length = animDef.get("animation_length")?.asFloat ?: BrBoneAnimation.calcAnimLength(bones)
				// 解析 Molang anim_time_update
				val exprStr = animDef.get("anim_time_update")?.asString ?: "query.anim_time + query.delta_time"
				try {
					val mathValue = MathParser.compileMolang(exprStr)
					val expr = exprCache.getOrPut(exprStr) { mathValue }
					result[animId] = BrBedrockAnimation(animId, loop, length, bones, sounds, particles, timelines, expr)
				} catch (e: Exception) {
					RcfConstants.LOGGER.warn(
						"[ANIMATION/{}] Failed to parse anim_time_update: '{}' for {} exception: {}",
						side,
						exprStr,
						animId,
						e
					)
					result[animId] = BrBedrockAnimation(animId, loop, length, bones, sounds, particles, timelines)
				}
			}
			return result
		}
	}
}