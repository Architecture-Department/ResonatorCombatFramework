package architecture.resonator_combat_framework.module.animation.keyframe_animation

import architecture.resonator_combat_framework.module.animation.LoopType
import architecture.resonator_combat_framework.module.animation.model.BonePose
import architecture.resonator_combat_framework.module.animation.model.PoseData
import architecture.resonator_combat_framework.module.animation.molang.EasingTypes
import architecture.resonator_combat_framework.module.animation.molang.MolangData
import architecture.resonator_combat_framework.module.animation.util.MirrorUtil
import com.google.gson.JsonObject
import org.joml.Vector2f
import org.joml.Vector3f
import kotlin.math.floor

/**
 * 关键帧动画数据类，表示一个完整的 Bedrock 格式动画定义。
 * 包含动画标识符、播放模式、时长、骨骼关键帧轨道，以及时间线上的声音/粒子/脚本事件。
 *
 * @property identifier 动画的唯一标识符
 * @property loop 播放模式：ONCE（一次）/ LOOP（循环）/ HOLD_ON_LAST（保持最后一帧）
 * @property length 动画总时长（秒）
 * @property bones 骨骼名称到骨骼动画轨道的映射
 * @property sounds 时间线上的声音事件列表
 * @property particles 时间线上的粒子事件列表
 * @property timelines 时间线上的脚本事件列表
 */
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

		/**
		 * 解析 JSON 根对象的 "animations" 段，每个条目注册为一个 KeyframeAnimation。
		 *
		 * @param root JSON 根对象
		 * @return 动画标识符到动画对象的映射表
		 */
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

	/**
	 * 计算动画在指定时刻的骨骼变换并写入 PoseData。
	 * 对每根骨骼插值计算位置/旋转/缩放，写入局部变换数据。
	 *
	 * @param time 动画时间（秒）
	 * @param poseData 目标姿态数据容器
	 * @param context MoLang 运行上下文
	 * @return 受此帧影响的骨骼名称集合
	 */
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
	 * 关键帧插值结果。
	 *
	 * @property value 插值后的 Vector3f 值
	 * @property noInterp 是否标记为不插值（STEP 模式）
	 */
	data class InterpResult(val value: Vector3f?, val noInterp: Boolean)

	/**
	 * 对关键帧序列进行插值，返回指定时刻的值。
	 * 支持 LINEAR（线性插值）、CATMULLROM（Catmull-Rom 样条插值）和 STEP（无插值）三种模式。
	 *
	 * @param frames 关键帧列表
	 * @param time 当前动画时间（秒）
	 * @param context MoLang 运行上下文
	 * @return 插值结果
	 */
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
	 * 分段 Catmull-Rom 样条求值。
	 * 将时间归一化到 [0,1] 后在控制点序列上进行样条插值。
	 *
	 * @param points 控制点列表（每个点包含时间和值）
	 * @param time 归一化时间 [0,1]
	 * @return 插值后的值
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
	 * 用于为对称攻击动画生成镜像版本（如左手版和右手版）。
	 *
	 * @return 镜像后的动画副本
	 */
	fun mirror(): KeyframeAnimation {
		val newBones = mutableMapOf<String, BoneTrack>()
		for ((name, boneAnim) in bones) {
			newBones[MirrorUtil.mirrorBoneName(name)] = boneAnim.mirrored()
		}
		return copy(
			bones = newBones,
			sounds = sounds.map { sound ->
				sound.copy(effects = sound.effects.map { effect ->
					effect.copy(locatorName = effect.locatorName?.let { MirrorUtil.mirrorBoneName(it) })
				})
			},
			particles = particles.map { particle ->
				particle.copy(effects = particle.effects.map { effect ->
					effect.copy(locatorName = effect.locatorName?.let { MirrorUtil.mirrorBoneName(it) })
				})
			}
		)
	}
}
