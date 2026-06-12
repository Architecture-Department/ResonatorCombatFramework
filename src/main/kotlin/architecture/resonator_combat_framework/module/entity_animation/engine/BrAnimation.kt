package architecture.resonator_combat_framework.module.entity_animation.engine

import architecture.resonator_combat_framework.core.RcfConstants
import architecture.resonator_combat_framework.module.entity_animation.ProxyBone
import architecture.resonator_combat_framework.module.entity_animation.ProxyModel
import architecture.resonator_combat_framework.module.entity_animation.engine.molang.EasingTypes
import architecture.resonator_combat_framework.module.entity_animation.engine.molang.MathParser
import architecture.resonator_combat_framework.module.entity_animation.engine.molang.MolangData
import architecture.resonator_combat_framework.module.entity_animation.engine.molang.MolangValue
import com.google.gson.JsonObject
import org.joml.Vector2f
import org.joml.Vector3f
import kotlin.math.floor

data class BrAnimation(
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
		): MutableMap<String, BrAnimation> {
			val result = mutableMapOf<String, BrAnimation>()
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
					result[animId] = BrAnimation(animId, loop, length, bones, sounds, particles, timelines, expr)
				} catch (e: Exception) {
					RcfConstants.LOGGER.warn(
						"[ANIMATION/{}] Failed to parse anim_time_update: '{}' for {} exception: {}",
						side,
						exprStr,
						animId,
						e
					)
					result[animId] = BrAnimation(animId, loop, length, bones, sounds, particles, timelines)
				}
			}
			return result
		}
	}

	/** 计算动画在 time 时刻的骨骼变换并写入 proxyModel.localPos/localRot/localScale（局部变换） */
	fun computeAndWrite(
		time: Float,
		proxyModel: ProxyModel,
		context: MolangData? = null
	): Set<String> {
		val affected = mutableSetOf<String>()
		for ((boneName, boneAnim) in bones) {
			val pos = interpolateFrames(boneAnim.pos, time, context)
			val rot = interpolateFrames(boneAnim.rot, time, context)
			val scale = interpolateFrames(boneAnim.scale, time, context)

			val bone = proxyModel.getBone(boneName) ?: ProxyBone(boneName).also { proxyModel.addBone(it) }
			bone.setPosEmpty(pos == null)
			if (pos != null) bone.pos.set(pos) else bone.pos.set(0f, 0f, 0f)
			bone.setRotEmpty(rot == null)
			if (rot != null) bone.rotation.set(rot) else bone.rotation.set(0f, 0f, 0f)
			bone.setScaleEmpty(scale == null)
			if (scale != null) bone.scale.set(scale) else bone.scale.set(1f, 1f, 1f)
			affected.add(boneName)
		}
		return affected
	}

	/**
	 * 插值关键帧序列，返回 time 时刻的值
	 */
	internal fun interpolateFrames(
		frames: List<BrBoneKeyFrame>,
		time: Float,
		context: MolangData? = null
	): Vector3f? {
		if (frames.isEmpty()) return null
		val afterIdx = frames.indexOfFirst { it.time > time }
		if (afterIdx < 0) return Vector3f(frames.last().evaluateValue(context = context))
		if (afterIdx == 0) return Vector3f(frames.first().evaluateValue(context = context))

		val before = frames[afterIdx - 1]
		val after = frames[afterIdx]
		val weight = (time - before.time) / (after.time - before.time)

		return when {
			before.lerp == BrBoneKeyFrame.LerpMode.STEP -> Vector3f(before.evaluateValue(context = context))

			before.lerp == BrBoneKeyFrame.LerpMode.CATMULLROM || after.lerp == BrBoneKeyFrame.LerpMode.CATMULLROM -> {
				val beforePlus = if (afterIdx > 1) frames[afterIdx - 2] else null
				val afterPlus = if (afterIdx < frames.size - 1) frames[afterIdx + 1] else null
				val useFirst = beforePlus != null && !(before.hasPreData() && before.hasPostData())
				val useLast = afterPlus != null && !(after.hasPreData() && after.hasPostData())

				fun buildAxis(
					bp: (BrBoneKeyFrame) -> Float,
					bv: (BrBoneKeyFrame) -> Float,
					av: (BrBoneKeyFrame) -> Float,
					ap: (BrBoneKeyFrame) -> Float
				): Float {
					val pts = mutableListOf<Vector2f>()
					if (useFirst) pts.add(Vector2f(beforePlus.time, bp(beforePlus)))
					pts.add(Vector2f(before.time, bv(before)))
					pts.add(Vector2f(after.time, av(after)))
					if (useLast) pts.add(Vector2f(afterPlus.time, ap(afterPlus)))
					val adjW = weight + (if (useFirst) 1f else 0f)
					return splineLerp(pts, adjW / (pts.size - 1))
				}

				val cx = buildAxis(
					{ it.evaluatePost(context = context).x },
					{ it.evaluatePost(context = context).x },
					{ it.evaluatePre(context = context).x },
					{ it.evaluatePre(context = context).x }
				)
				val cy = buildAxis(
					{ it.evaluatePost(context = context).y },
					{ it.evaluatePost(context = context).y },
					{ it.evaluatePre(context = context).y },
					{ it.evaluatePre(context = context).y }
				)
				val cz = buildAxis(
					{ it.evaluatePost(context = context).z },
					{ it.evaluatePost(context = context).z },
					{ it.evaluatePre(context = context).z },
					{ it.evaluatePre(context = context).z }
				)
				Vector3f(cx, cy, cz)
			}

			else -> {
				val from = before.evaluatePost(context = context)
				val to = after.evaluatePre(context = context)
				Vector3f().set(from).lerp(to, weight)
			}
		}
	}

	/**
	 * 分段 Catmull-Rom 样条求值
	 */
	internal fun splineLerp(points: List<Vector2f>, time: Float): Float {
		val p = (points.size - 1) * time
		val intPoint = floor(p.toDouble()).toInt()
		val weight = p - intPoint
		val idx0 = if (intPoint == 0) 0 else (intPoint - 1).coerceAtMost(points.size - 1)
		val idx1 = intPoint.coerceAtMost(points.size - 1)
		val idx2 = if (intPoint > points.size - 2) points.size - 1 else intPoint + 1
		val idx3 = if (intPoint > points.size - 3) points.size - 1 else intPoint + 2
		return EasingTypes.catmullRom(
			weight.toDouble(), points[idx0].y.toDouble(), points[idx1].y.toDouble(),
			points[idx2].y.toDouble(), points[idx3].y.toDouble()
		).toFloat()
	}
}