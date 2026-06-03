package architecture.resonator_combat_framework.module.entity_animation.registry

import architecture.resonator_combat_framework.core.RcfConstants
import architecture.resonator_combat_framework.module.entity_animation.engine.BedrockAnimation
import architecture.resonator_combat_framework.module.entity_animation.engine.BrBoneAnimation
import architecture.resonator_combat_framework.module.entity_animation.engine.BrBoneKeyFrame
import architecture.resonator_combat_framework.module.entity_animation.engine.MolangVector3
import architecture.resonator_combat_framework.module.entity_animation.engine.molang.MathParser
import architecture.resonator_combat_framework.module.entity_animation.engine.molang.MathValue
import architecture.resonator_combat_framework.module.entity_animation.engine.molang.value.Constant
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.server.packs.resources.SimplePreparableReloadListener
import net.minecraft.util.profiling.ProfilerFiller

/**
 * Bedrock 动画注册器。
 * 从 `assets/<namespace>/rcf/animations/` 加载动画 JSON 文件。
 * 每个 JSON 文件可包含多个动画（animations 对象下的所有条目均独立注册）。
 */
class BedrockAnimationRegistry(private val side: String = "?") :
	SimplePreparableReloadListener<Map<String, BedrockAnimation>>() {

	companion object {
		private val CLIENT = BedrockAnimationRegistry("CLIENT")
		private val SERVER = BedrockAnimationRegistry("SERVER")

		@JvmStatic
		fun getInstance(isClient: Boolean): BedrockAnimationRegistry {
			return if (isClient) CLIENT else SERVER
		}
	}

	private val animations = mutableMapOf<String, BedrockAnimation>()
	private val exprCache = mutableMapOf<String, MathValue>()

	fun get(animId: String): BedrockAnimation? = animations[animId]

	fun getAllAnimIds(): Set<String> = animations.keys

	override fun prepare(manager: ResourceManager, profiler: ProfilerFiller): Map<String, BedrockAnimation> {
		val result = mutableMapOf<String, BedrockAnimation>()
		var totalFiles = 0;
		var totalAnims = 0
		for (entry in manager.listResources("rcf/animations") { it.path.endsWith(".json") }) {
			val before = result.size
			try {
				val json = JsonParser.parseReader(entry.value.openAsReader()).asJsonObject
				parseAnimations(json, result)
			} catch (e: Exception) {
				RcfConstants.LOGGER.error("[ANIMATION/{}] Failed to load: {} - {}", side, entry.key, e.message)
			}
			val loaded = result.size - before
			if (loaded > 0) {
				totalFiles += 1; totalAnims += loaded
				RcfConstants.LOGGER.info("[ANIMATION/{}] Loaded {} animations from {}", side, loaded, entry.key)
			}
		}
		RcfConstants.LOGGER.info("[ANIMATION/{}] Prepare complete: {} files, {} animations", side, totalFiles, totalAnims)
		return result
	}

	override fun apply(loaded: Map<String, BedrockAnimation>, manager: ResourceManager, profiler: ProfilerFiller) {
		animations.clear()
		animations.putAll(loaded)
		RcfConstants.LOGGER.info(
			"[ANIMATION/{}] Applied {} bedrock animations: {}",
			side,
			loaded.size,
			loaded.keys.sorted().joinToString(", ")
		)
	}

	/** 解析 JSON 根对象的 "animations" 段，每个条目注册为一个 BedrockAnimation */
	private fun parseAnimations(root: JsonObject, out: MutableMap<String, BedrockAnimation>) {
		val animations = root.getAsJsonObject("animations") ?: return
		for ((animKey, animEl) in animations.entrySet()) {
			val animId = animKey
			val animDef = animEl.asJsonObject ?: continue


			val loopEl = animDef.get("loop")
			val loop = when {
				loopEl?.isJsonPrimitive == true && loopEl.asBoolean -> BedrockAnimation.LoopType.LOOP
				loopEl?.asString == "loop" -> BedrockAnimation.LoopType.LOOP
				loopEl?.asString == "hold_on_last_frame" -> BedrockAnimation.LoopType.HOLD_ON_LAST
				else -> BedrockAnimation.LoopType.ONCE
			}
			val bonesJson = animDef.getAsJsonObject("bones")
			val bones = if (bonesJson != null) parseBones(bonesJson) else emptyMap()
			val length = animDef.get("animation_length")?.asFloat ?: calcAnimLength(bones)
			// 解析 Molang anim_time_update
			val exprStr = animDef.get("anim_time_update")?.asString ?: "query.anim_time + query.delta_time"
			try {
				val mathValue = MathParser.compileMolang(exprStr)
				val expr = exprCache.getOrPut(exprStr) { mathValue }
				out[animId] = BedrockAnimation(animId, loop, length, bones, animTimeUpdate = expr)
			} catch (e: Exception) {
				RcfConstants.LOGGER.warn("[ANIMATION/{}] Failed to parse anim_time_update: '{}' for {}", side, exprStr, animId)
				out[animId] = BedrockAnimation(animId, loop, length, bones, animTimeUpdate = null)
			}
		}
	}

	/** 解析骨骼字典：{ 骨骼名 → { position/rotation/scale } } */
	private fun parseBones(bonesJson: JsonObject): Map<String, BrBoneAnimation> {
		val bones = mutableMapOf<String, BrBoneAnimation>()
		for ((name, bj) in bonesJson.entrySet()) {
			val bObj = bj.asJsonObject
			bones[name] = BrBoneAnimation(
				parseBrBoneKeyFrames(bObj.get("position")),
				parseBrBoneKeyFrames(bObj.get("rotation")),
				parseBrBoneKeyFrames(bObj.get("scale"))
			)
		}
		return bones
	}

	/** 解析单组关键帧数据：简单数组 [x,y,z] 或对象 {pre, post, lerp_mode} */
	private fun parseBrBoneKeyFrames(element: JsonElement?): List<BrBoneKeyFrame> {
		element ?: return emptyList()

		// 单组值：[x, y, z]（数字→Constant，字符串→MoLang）
		if (element.isJsonArray) {
			return listOf(BrBoneKeyFrame(time = 0f, value = parseMolangVector(element.asJsonArray)))
		}

		if (!element.isJsonObject) return emptyList()
		val obj = element.asJsonObject

		val frames = mutableListOf<BrBoneKeyFrame>()
		for ((timeStr, valEl) in obj.entrySet()) {
			val time = timeStr.toFloatOrNull() ?: continue

			when {
				// 值数组：[x, y, z]
				valEl.isJsonArray -> {
					frames.add(BrBoneKeyFrame(time = time, value = parseMolangVector(valEl.asJsonArray)))
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
							"catmullrom" -> BrBoneKeyFrame.LerpMode.CATMULLROM
							else -> {
								RcfConstants.LOGGER.warn("Unknown lerp_mode: {}", mode)
								BrBoneKeyFrame.LerpMode.LINEAR
							}
						}
					} ?: BrBoneKeyFrame.LerpMode.STEP
					frames.add(BrBoneKeyFrame(time = time, pre = pre, post = post, lerp = lerpMode))
				}

				// 单一 MoLang 字符串：三个轴用同一个表达式
				valEl.isJsonPrimitive && valEl.asJsonPrimitive.isString -> {
					val expr = try {
						MathParser.compileMolang(valEl.asString)
					} catch (_: Exception) {
						null
					}
					if (expr != null) {
						frames.add(BrBoneKeyFrame(time = time, value = MolangVector3(expr, expr, expr)))
					}
				}
			}
		}
		return frames.sortedBy { it.time }
	}

	/** 从所有骨骼关键帧中取最大时间作为动画长度，至少 1 秒 */
	private fun calcAnimLength(bones: Map<String, BrBoneAnimation>): Float {
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


	/** 解析 [x, y, z] 数组：数字→Constant，字符串→MoLang 表达式 */
	private fun parseMolangVector(arr: com.google.gson.JsonArray): MolangVector3 {
		if (arr.size() < 3) return MolangVector3()
		return MolangVector3(
			x = parseAxisExpr(arr[0]),
			y = parseAxisExpr(arr[1]),
			z = parseAxisExpr(arr[2])
		)
	}

	/** 解析单轴值：数字→Constant，字符串→MoLang */
	private fun parseAxisExpr(el: JsonElement): MathValue {
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

