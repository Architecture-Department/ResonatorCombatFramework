package architecture.resonator_combat_framework.module.entity_animation.animation

import architecture.goldenboughs_lib.util.LibUtil
import architecture.resonator_combat_framework.core.RcfConstants
import architecture.resonator_combat_framework.module.entity_animation.animation.model.ProxyBone
import architecture.resonator_combat_framework.module.entity_animation.animation.model.ProxyModel
import architecture.resonator_combat_framework.module.entity_animation.animation.molang.*
import architecture.resonator_combat_framework.module.entity_animation.animation.molang.value.Constant
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import net.minecraft.commands.CommandSourceStack
import net.minecraft.core.particles.ParticleOptions
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundSource
import net.minecraft.util.StringUtil
import net.minecraft.world.entity.Entity
import net.minecraft.world.phys.Vec3
import org.joml.Vector2f
import org.joml.Vector3d
import org.joml.Vector3f
import kotlin.math.floor

data class BakingBrAnimation
@JvmOverloads
constructor(
	val animId: String,
	/** 播放模式：ONCE / LOOP / HOLD_ON_LAST */
	val loop: LoopType = LoopType.ONCE,
	/** 动画总时长（秒） */
	val length: Float,
	/** 骨骼名 → 骨骼动画数据 */
	val bones: Map<String, BakingBrBoneAnimation> = emptyMap(),
	val sounds: List<BakingBrAnimationSound> = emptyList(),
	val particles: List<BakingBrAnimationParticle> = emptyList(),
	val timelines: List<BakingBrAnimationTimeline> = emptyList(),
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
		): MutableMap<String, BakingBrAnimation> {
			val result = mutableMapOf<String, BakingBrAnimation>()
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
					BakingBrBoneAnimation.parses(bonesJson) else emptyMap()

				val soundsJson = animDef.getAsJsonObject("sound_effects")
				val sounds = if (soundsJson != null)
					BakingBrAnimationSound.parses(soundsJson) else emptyList()

				val particlesJson = animDef.getAsJsonObject("particle_effects")
				val particles = if (particlesJson != null)
					BakingBrAnimationParticle.parses(particlesJson) else emptyList()

				val timelinesJson = animDef.getAsJsonObject("timeline")
				val timelines = if (timelinesJson != null)
					BakingBrAnimationTimeline.parses(timelinesJson) else emptyList()

				val length = animDef.get("animation_length")?.asFloat ?: BakingBrBoneAnimation.calcAnimLength(bones)
				// 解析 Molang anim_time_update
				val exprStr = animDef.get("anim_time_update")?.asString ?: "query.anim_time + query.delta_time"
				try {
					val mathValue = MathParser.compileMolang(exprStr)
					val expr = exprCache.getOrPut(exprStr) { mathValue }
					result[animId] = BakingBrAnimation(animId, loop, length, bones, sounds, particles, timelines, expr)
				} catch (e: Exception) {
					RcfConstants.LOGGER.warn(
						"[ANIMATION/{}] Failed to parse anim_time_update: '{}' for {} exception: {}",
						side,
						exprStr,
						animId,
						e
					)
					result[animId] = BakingBrAnimation(animId, loop, length, bones, sounds, particles, timelines)
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
		frames: List<BakingBrBoneKeyFrame>,
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
			before.lerp == BakingBrBoneKeyFrame.LerpMode.STEP -> Vector3f(before.evaluateValue(context = context))

			before.lerp == BakingBrBoneKeyFrame.LerpMode.CATMULLROM || after.lerp == BakingBrBoneKeyFrame.LerpMode.CATMULLROM -> {
				val beforePlus = if (afterIdx > 1) frames[afterIdx - 2] else null
				val afterPlus = if (afterIdx < frames.size - 1) frames[afterIdx + 1] else null
				val useFirst = beforePlus != null && !(before.hasPreData() && before.hasPostData())
				val useLast = afterPlus != null && !(after.hasPreData() && after.hasPostData())

				fun buildAxis(
					bp: (BakingBrBoneKeyFrame) -> Float,
					bv: (BakingBrBoneKeyFrame) -> Float,
					av: (BakingBrBoneKeyFrame) -> Float,
					ap: (BakingBrBoneKeyFrame) -> Float
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
				Vector3f(from).lerp(to, weight)
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

data class BakingBrAnimationParticle
@JvmOverloads
constructor(
	val time: Float,
	val effects: List<Effect> = emptyList()
) {
	fun apply(entity: Entity, pos: Vector3d, context: MolangData? = null) {
		effects.forEach { it.apply(entity, pos, context) }
	}

	data class Effect(
		val particleId: ResourceLocation,
		val locator: String? = null,
		val bindToActor: Boolean = true,
		val preEffectScript: MolangValue? = null
	) {
		fun apply(entity: Entity, pos: Vector3d, context: MolangData? = null) {
			preEffectScript?.get(context)
			val particleType = BuiltInRegistries.PARTICLE_TYPE.get(particleId) ?: return
			val emitPos = if (bindToActor) entity.position() else Vec3(pos.x, pos.y, pos.z)
			if (entity.level().isClientSide) {
				@Suppress("UNCHECKED_CAST")
				val particle = particleType as? ParticleOptions
				if (particle != null) {
					entity.level().addParticle(particle, emitPos.x, emitPos.y, emitPos.z, 0.0, 0.0, 0.0)
				}
			}
		}
	}

	companion object {
		fun parses(particlesJson: JsonObject): List<BakingBrAnimationParticle> {
			val list = mutableListOf<BakingBrAnimationParticle>()
			particlesJson.asMap().forEach { (key, value) ->
				val effects = mutableListOf<Effect>()
				if (!value.isJsonArray) {
					parseEffect(value)?.let { effects.add(it) }
				} else {
					value.asJsonArray.forEach { parseEffect(it)?.let { e -> effects.add(e) } }
				}
				list.add(BakingBrAnimationParticle(key.toFloat(), effects))
			}
			return list
		}

		private fun parseEffect(element: JsonElement?): Effect? {
			element ?: return null
			val obj = element.asJsonObject
			val particleId = LibUtil.rlOf(obj.get("effect").asString)
			val boneName = obj.get("locator")?.asString
			val bindToActor = obj.get("bind_to_actor")?.asBoolean ?: true
			val preEffectScript = obj.get("pre_effect_script")?.let { MathParser.compileMolang(it.asString) }
			return Effect(particleId, boneName, bindToActor, preEffectScript)
		}
	}
}

data class BakingBrAnimationSound
@JvmOverloads
constructor(
	val time: Float,
	val effects: List<Effect> = emptyList()
) {
	fun apply(entity: Entity, pos: Vector3d, context: MolangData? = null) {
		effects.forEach { it.apply(entity, pos, context) }
	}

	data class Effect(
		val soundId: ResourceLocation,
		val locator: String? = null,
		val bindToActor: Boolean = true,
		val preEffectScript: MolangValue? = null
	) {
		fun apply(entity: Entity, pos: Vector3d, context: MolangData? = null) {
			preEffectScript?.get(context)
			val soundEvent = BuiltInRegistries.SOUND_EVENT.get(soundId) ?: return
			val x = if (bindToActor) entity.x else pos.x
			val y = if (bindToActor) entity.y else pos.y
			val z = if (bindToActor) entity.z else pos.z
			entity.level().playSound(null, x, y, z, soundEvent, SoundSource.PLAYERS, 1f, 1f)
		}
	}

	companion object {
		fun parses(soundsJson: JsonObject): List<BakingBrAnimationSound> {
			val list = mutableListOf<BakingBrAnimationSound>()
			soundsJson.asMap().forEach { (key, value) ->
				val effects = mutableListOf<Effect>()
				if (!value.isJsonArray) {
					parseEffect(value)?.let { effects.add(it) }
				} else {
					value.asJsonArray.forEach { parseEffect(it)?.let { e -> effects.add(e) } }
				}
				list.add(BakingBrAnimationSound(key.toFloat(), effects))
			}
			return list
		}

		private fun parseEffect(element: JsonElement?): Effect? {
			element ?: return null
			val obj = element.asJsonObject
			val soundId = LibUtil.rlOf(obj.get("effect").asString)
			val locators = obj.get("locator")?.asString
			return Effect(soundId, locators)
		}
	}
}

data class BakingBrAnimationTimeline
@JvmOverloads
constructor(
	val time: Float,
	val molangs: List<MolangValue> = emptyList(),
	val commands: List<String> = emptyList(),
	val entityEvents: List<String> = emptyList()
) {
	fun apply(entity: Entity, context: MolangData? = null) {
		molangs.forEach { it.get(context) }

		entityEvents.forEach { event -> processEntityEvent(entity, event) }

		if (entity.level() !is ServerLevel) return
		val level = entity.level() as ServerLevel
		val server = level.server
		if (commands.isEmpty()) return

		@Suppress("TYPE_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
		val source = CommandSourceStack(
			entity, entity.position(), entity.rotationVector,
			level, 2, entity.name.string, entity.displayName, server, entity
		)
		commands.forEach { cmd ->
			if (StringUtil.isNullOrEmpty(cmd)) return@forEach
			try {
				server.commands.performPrefixedCommand(source, cmd)
			} catch (e: Exception) {
				RcfConstants.LOGGER.warn("[Timeline] Command failed: {} {} {} ", cmd, entity, e)
			}
		}
	}

	private fun processEntityEvent(entity: Entity, event: String) {
		val trimmed = event.trim()
		when {
			trimmed.startsWith("@s ") || trimmed == "@s" -> {
				val eventName = trimmed.removePrefix("@s").trim()
				if (eventName.isNotEmpty()) {
					RcfConstants.LOGGER.debug("[Timeline] Entity event: {} {}", eventName, entity)
				}
			}

			else -> {
				RcfConstants.LOGGER.warn("[Timeline] Unsupported entity event: {}", trimmed)
			}
		}
	}

	companion object {
		fun parses(timelinesJson: JsonObject): List<BakingBrAnimationTimeline> {
			val list = mutableListOf<BakingBrAnimationTimeline>()
			timelinesJson.asMap().forEach { (key, value) ->
				val molangs = mutableListOf<MolangValue>()
				val commands = mutableListOf<String>()
				val entityEvents = mutableListOf<String>()
				value.asString.split(";").forEach {
					val trimmed = it.trim()
					when {
						trimmed.startsWith("/") -> commands.add(it)
						trimmed.startsWith("@") -> entityEvents.add(it)
						else -> molangs.add(MathParser.compileMolang(it))
					}
				}
				list.add(BakingBrAnimationTimeline(key.toFloat(), molangs, commands, entityEvents))
			}
			return list
		}
	}
}

data class BakingBrBoneAnimation
@JvmOverloads
constructor(
	val pos: List<BakingBrBoneKeyFrame> = emptyList(),
	val rot: List<BakingBrBoneKeyFrame> = emptyList(),
	val scale: List<BakingBrBoneKeyFrame> = emptyList()
) {
	companion object {
		/** 从所有骨骼关键帧中取最大时间作为动画长度，至少 1 秒 */
		@JvmStatic
		fun calcAnimLength(bones: Map<String, BakingBrBoneAnimation>): Float {
			var maxTime = 0f
			for ((_, ba) in bones) {
				for (kf in ba.pos) {
					if (kf.time > maxTime) maxTime = kf.time
				}
				for (kf in ba.rot) {
					if (kf.time > maxTime) maxTime = kf.time
				}
				for (kf in ba.scale) {
					if (kf.time > maxTime) maxTime = kf.time
				}
			}
			return if (maxTime > 0f) maxTime else 1f  // 至少 1 秒（会被外部 *20）
		}

		/** 解析骨骼字典：{ 骨骼名 → { position/rotation/scale } } */
		@JvmStatic
		fun parses(bonesJson: JsonObject): Map<String, BakingBrBoneAnimation> {
			val bones = mutableMapOf<String, BakingBrBoneAnimation>()
			for ((name, bj) in bonesJson.entrySet()) {
				val bObj = bj.asJsonObject
				bones[name] = BakingBrBoneAnimation(
					BakingBrBoneKeyFrame.parse(bObj.get("position")),
					BakingBrBoneKeyFrame.parse(bObj.get("rotation")),
					BakingBrBoneKeyFrame.parse(bObj.get("scale"))
				)
			}
			return bones
		}
	}
}

data class BakingBrBoneKeyFrame
@JvmOverloads
constructor(
	/** 关键帧时间（秒） */
	val time: Float,
	/** 插值模式 */
	val lerp: LerpMode = LerpMode.LINEAR,
	/** 关键帧值（简单数组 [x,y,z] 时使用） */
	val value: MolangVector3 = MolangVector3(),
	/** 入切线（对象格式 {"pre":[...]} 时使用） */
	val pre: MolangVector3 = MolangVector3(),
	/** 出切线（对象格式 {"post":[...]} 时使用） */
	val post: MolangVector3 = MolangVector3()
) {
	/** 取关键帧值：value → post → pre → (0,0,0) */
	fun evaluateValue(out: Vector3f = Vector3f(), context: MolangData? = null): Vector3f {
		if (!value.allNull()) return value.evaluate(out, context)
		if (!post.allNull()) return post.evaluate(out, context)
		if (!pre.allNull()) return pre.evaluate(out, context)
		return out.set(0f, 0f, 0f)
	}

	/** 取入切线：pre → value → post → (0,0,0) */
	fun evaluatePre(out: Vector3f = Vector3f(), context: MolangData? = null): Vector3f {
		if (!pre.allNull()) return pre.evaluate(out, context)
		if (!value.allNull()) return value.evaluate(out, context)
		if (!post.allNull()) return post.evaluate(out, context)
		return out.set(0f, 0f, 0f)
	}

	/** 取出切线：post → value → pre → (0,0,0) */
	fun evaluatePost(out: Vector3f = Vector3f(), context: MolangData? = null): Vector3f {
		if (!post.allNull()) return post.evaluate(out, context)
		if (!value.allNull()) return value.evaluate(out, context)
		if (!pre.allNull()) return pre.evaluate(out, context)
		return out.set(0f, 0f, 0f)
	}

	fun hasPreData(): Boolean = pre.allNull().not()
	fun hasPostData(): Boolean = post.allNull().not()

	enum class LerpMode { LINEAR, CATMULLROM, STEP }

	companion object {
		/** 解析单组关键帧数据：简单数组 [x,y,z] 或对象 {pre, post, lerp_mode} */
		@JvmStatic
		fun parse(element: JsonElement?): List<BakingBrBoneKeyFrame> {
			element ?: return emptyList()

			// 单组值：[x, y, z]（数字→Constant，字符串→MoLang）
			if (element.isJsonArray) {
				return listOf(BakingBrBoneKeyFrame(time = 0f, value = parseMolangVector(element.asJsonArray)))
			}

			if (!element.isJsonObject) return emptyList()
			val obj = element.asJsonObject

			val frames = mutableListOf<BakingBrBoneKeyFrame>()
			for ((timeStr, valEl) in obj.entrySet()) {
				val time = timeStr.toFloatOrNull() ?: continue

				when {
					// 值数组：[x, y, z]
					valEl.isJsonArray -> {
						frames.add(BakingBrBoneKeyFrame(time = time, value = parseMolangVector(valEl.asJsonArray)))
					}

					// 对象：{ "pre": [...], "post": [...], "lerp_mode": "..." }
					valEl.isJsonObject -> {
						val o = valEl.asJsonObject
						val pre =
							o.get("pre")?.takeIf { it.isJsonArray }?.let { parseMolangVector(it.asJsonArray) } ?: MolangVector3()
						val post =
							o.get("post")?.takeIf { it.isJsonArray }?.let { parseMolangVector(it.asJsonArray) } ?: MolangVector3()
						val lerpMode = o.get("lerp_mode")?.run {
							when (val mode = asString) {
								"catmullrom" -> LerpMode.CATMULLROM
								else -> {
									RcfConstants.LOGGER.warn("Unknown lerp_mode: {}", mode)
									LerpMode.LINEAR
								}
							}
						} ?: LerpMode.STEP
						frames.add(BakingBrBoneKeyFrame(time = time, pre = pre, post = post, lerp = lerpMode))
					}

					// 单一 MoLang 字符串：三个轴用同一个表达式
					valEl.isJsonPrimitive && valEl.asJsonPrimitive.isString -> {
						val expr = try {
							MathParser.compileMolang(valEl.asString)
						} catch (_: Exception) {
							null
						}
						if (expr != null) {
							frames.add(BakingBrBoneKeyFrame(time = time, value = MolangVector3(expr, expr, expr)))
						}
					}
				}
			}
			return frames.sortedBy { it.time }
		}

		/** 解析 [x, y, z] 数组：数字→Constant，字符串→MoLang 表达式 */
		private fun parseMolangVector(arr: JsonArray): MolangVector3 {
			if (arr.size() < 3) return MolangVector3()
			return MolangVector3(
				x = parseAxisExpr(arr[0]),
				y = parseAxisExpr(arr[1]),
				z = parseAxisExpr(arr[2])
			)
		}

		/** 解析单轴值：数字→Constant，字符串→MoLang */
		private fun parseAxisExpr(el: JsonElement): MolangValue {
			return if (el.isJsonPrimitive && el.asJsonPrimitive.isString) {
				try {
					MathParser.compileMolang(el.asString)
				} catch (_: Exception) {
					Constant(0.0)
				}
			} else {
				Constant(el.asFloat.toDouble())
			}
		}
	}
}