package architecture.resonator_combat_framework.module.entity_animation.animation.baking_animation

import architecture.resonator_combat_framework.module.entity_animation.animation.LoopType
import architecture.resonator_combat_framework.module.entity_animation.animation.model.BonePose
import architecture.resonator_combat_framework.module.entity_animation.animation.model.PoseData
import architecture.resonator_combat_framework.module.entity_animation.animation.molang.EasingTypes
import architecture.resonator_combat_framework.module.entity_animation.animation.molang.MolangData
import architecture.resonator_combat_framework.module.entity_animation.util.AnimationMirrorUtil
import com.google.gson.JsonObject
import org.joml.Vector2f
import org.joml.Vector3f
import kotlin.math.floor

data class KeyframeAnimation
@JvmOverloads constructor(
	val identifier: String,
	/** 播放模式：ONCE / LOOP / HOLD_ON_LAST */
	val loop: LoopType = LoopType.ONCE,
	/** 动画总时长（秒） */
	val length: Float,
	/** 骨骼名 → 骨骼动画数据 */
	val bones: Map<String, BoneTrack> = emptyMap(),
	val sounds: List<SoundEvent> = emptyList(),
	val particles: List<ParticleEvent> = emptyList(),
	val timelines: List<TimelineEvent> = emptyList(),
) {
	companion object {

		@JvmField
		val EMPTY = KeyframeAnimation("empty", length = 0f)

		/** 解析 JSON 根对象的 "animations" 段，每个条目注册为一个 BedrockAnimation */
		@JvmStatic
		fun parses(root: JsonObject): MutableMap<String, KeyframeAnimation> {
			val result = mutableMapOf<String, KeyframeAnimation>()
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
				val bones = if (bonesJson != null) BoneTrack.parses(bonesJson) else emptyMap()

				val soundsJson = animDef.getAsJsonObject("sound_effects")
				val sounds = if (soundsJson != null) SoundEvent.parses(soundsJson) else emptyList()

				val particlesJson = animDef.getAsJsonObject("particle_effects")
				val particles =
					if (particlesJson != null) ParticleEvent.parses(particlesJson) else emptyList()

				val timelinesJson = animDef.getAsJsonObject("timeline")
				val timelines =
					if (timelinesJson != null) TimelineEvent.parses(timelinesJson) else emptyList()

				val length = animDef.get("animation_length")?.asFloat ?: BoneTrack.calcAnimLength(bones)
				result[animId] = KeyframeAnimation(animId, loop, length, bones, sounds, particles, timelines)
			}
			return result
		}
	}

	/** 计算动画在 time 时刻的骨骼变换并写入 proxyModel.localPos/localRot/localScale（局部变换） */
	fun computeAndWrite(time: Float, poseData: PoseData, context: MolangData? = null): Set<String> {
		val affected = mutableSetOf<String>()
		for ((boneName, boneAnim) in bones) {
			val posRes = interpolateFrames(boneAnim.pos, time, context)
			val rotRes = interpolateFrames(boneAnim.rot, time, context)
			val scaleRes = interpolateFrames(boneAnim.scale, time, context)

			val bone = poseData.getBone(boneName) ?: BonePose(boneName).also { poseData.addBone(it) }
			bone.setPosEmpty(posRes.value == null)
			if (posRes.value != null) bone.pos.set(posRes.value) else bone.pos.set(0f, 0f, 0f)
			bone.setRotEmpty(rotRes.value == null)
			if (rotRes.value != null) bone.rotation.set(rotRes.value) else bone.rotation.set(0f, 0f, 0f)
			bone.setScaleEmpty(scaleRes.value == null)
			if (scaleRes.value != null) bone.scale.set(scaleRes.value) else bone.scale.set(1f, 1f, 1f)
			bone.noInterp = posRes.noInterp || rotRes.noInterp || scaleRes.noInterp
			affected.add(boneName)
		}
		return affected
	}

	/**
	 * 插值关键帧序列，返回 time 时刻的值
	 */
	data class InterpResult(val value: Vector3f?, val noInterp: Boolean)

	internal fun interpolateFrames(
		frames: List<Keyframe>, time: Float, context: MolangData? = null
	): InterpResult {
		if (frames.isEmpty()) return InterpResult(null, false)
		val afterIdx = frames.indexOfFirst { it.time > time }
		if (afterIdx < 0) return InterpResult(Vector3f(frames.last().evaluateValue(context = context)), false)
		if (afterIdx == 0) return InterpResult(Vector3f(frames.first().evaluateValue(context = context)), false)

		val before = frames[afterIdx - 1]
		val after = frames[afterIdx]
		val weight = (time - before.time) / (after.time - before.time)

		return when {
			before.lerp == Keyframe.LerpMode.STEP -> InterpResult(
				Vector3f(before.evaluateValue(context = context)),
				true
			)

			before.lerp == Keyframe.LerpMode.CATMULLROM || after.lerp == Keyframe.LerpMode.CATMULLROM -> {
				val beforePlus = if (afterIdx > 1) frames[afterIdx - 2] else null
				val afterPlus = if (afterIdx < frames.size - 1) frames[afterIdx + 1] else null
				val useFirst = beforePlus != null && !(before.hasPreData() && before.hasPostData())
				val useLast = afterPlus != null && !(after.hasPreData() && after.hasPostData())

				fun buildAxis(
					bp: (Keyframe) -> Float,
					bv: (Keyframe) -> Float,
					av: (Keyframe) -> Float,
					ap: (Keyframe) -> Float
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
					{ it.evaluatePre(context = context).x })
				val cy = buildAxis(
					{ it.evaluatePost(context = context).y },
					{ it.evaluatePost(context = context).y },
					{ it.evaluatePre(context = context).y },
					{ it.evaluatePre(context = context).y })
				val cz = buildAxis(
					{ it.evaluatePost(context = context).z },
					{ it.evaluatePost(context = context).z },
					{ it.evaluatePre(context = context).z },
					{ it.evaluatePre(context = context).z })
				InterpResult(Vector3f(cx, cy, cz), false)
			}

			else -> {
				val from = before.evaluatePost(context = context)
				val to = after.evaluatePre(context = context)
				InterpResult(Vector3f(from).lerp(to, weight), false)
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
			weight.toDouble(),
			points[idx0].y.toDouble(),
			points[idx1].y.toDouble(),
			points[idx2].y.toDouble(),
			points[idx3].y.toDouble()
		).toFloat()
	}

	/**
	 * 返回镜像后的动画副本：左右骨骼名称互换，位置 X 取反，旋转 Y/Z 取反。
	 */
	fun mirror(): KeyframeAnimation {
		val newBones = mutableMapOf<String, BoneTrack>()
		for ((name, boneAnim) in bones) {
			newBones[AnimationMirrorUtil.mirrorBoneName(name)] = boneAnim.mirrored()
		}
		return copy(
			bones = newBones,
			sounds = sounds.map { sound ->
				sound.copy(effects = sound.effects.map { effect ->
					effect.copy(locatorName = effect.locatorName?.let { AnimationMirrorUtil.mirrorBoneName(it) })
				})
			},
			particles = particles.map { particle ->
				particle.copy(effects = particle.effects.map { effect ->
					effect.copy(locatorName = effect.locatorName?.let { AnimationMirrorUtil.mirrorBoneName(it) })
				})
			}
		)
	}
}

