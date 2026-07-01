package architecture.resonator_combat_framework.module.entity_animation.animation.baking_animation

import architecture.resonator_combat_framework.module.entity_animation.animation.molang.MoLangParser
import architecture.resonator_combat_framework.module.entity_animation.animation.molang.MolangData
import architecture.resonator_combat_framework.module.entity_animation.animation.molang.MolangValue
import architecture.resonator_combat_framework.module.entity_animation.animation.molang.MolangVector3
import architecture.resonator_combat_framework.module.entity_animation.animation.molang.value.Constant
import architecture.resonator_combat_framework.util.RcfUtil
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import org.joml.Vector3f

data class Keyframe
@JvmOverloads constructor(
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

	/** 镜像位置关键帧：X 取反 */
	fun mirroredPos(): Keyframe = copy(
		value = value.mirroredPos(),
		pre = pre.mirroredPos(),
		post = post.mirroredPos()
	)

	/** 镜像旋转关键帧：Y/Z 取反 */
	fun mirroredRot(): Keyframe = copy(
		value = value.mirroredRot(),
		pre = pre.mirroredRot(),
		post = post.mirroredRot()
	)

	enum class LerpMode { LINEAR, CATMULLROM, STEP }

	companion object {
		/** 解析单组关键帧数据：简单数组 [x,y,z] 或对象 {pre, post, lerp_mode} */
		@JvmStatic
		fun parse(element: JsonElement?): List<Keyframe> {
			element ?: return emptyList()

			// 单组值：[x, y, z]（数字→Constant，字符串→MoLang）
			if (element.isJsonArray) {
				return listOf(Keyframe(time = 0f, value = parseMolangVector(element.asJsonArray)))
			}

			if (!element.isJsonObject) return emptyList()
			val obj = element.asJsonObject

			val frames = mutableListOf<Keyframe>()
			for ((timeStr, valEl) in obj.entrySet()) {
				val time = timeStr.toFloatOrNull() ?: continue

				when {
					// 值数组：[x, y, z]
					valEl.isJsonArray -> {
						frames.add(Keyframe(time = time, value = parseMolangVector(valEl.asJsonArray)))
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
									RcfUtil.LOGGER.warn("Unknown lerp_mode: {}", mode)
									LerpMode.LINEAR
								}
							}
						} ?: LerpMode.STEP
						frames.add(Keyframe(time = time, pre = pre, post = post, lerp = lerpMode))
					}

					// 单一 MoLang 字符串：三个轴用同一个表达式
					valEl.isJsonPrimitive && valEl.asJsonPrimitive.isString -> {
						val expr = try {
							MoLangParser.compileMolang(valEl.asString)
						} catch (_: Exception) {
							null
						}
						if (expr != null) {
							frames.add(Keyframe(time = time, value = MolangVector3(expr, expr, expr)))
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
				x = parseAxisExpr(arr[0]), y = parseAxisExpr(arr[1]), z = parseAxisExpr(arr[2])
			)
		}

		/** 解析单轴值：数字→Constant，字符串→MoLang */
		private fun parseAxisExpr(el: JsonElement): MolangValue {
			return if (el.isJsonPrimitive && el.asJsonPrimitive.isString) {
				try {
					MoLangParser.compileMolang(el.asString)
				} catch (e: Exception) {
					RcfUtil.LOGGER.warn("[KEYFRAME] Failed to parse axis expr: {}", e.message)
					Constant(0.0)
				}
			} else {
				Constant(el.asFloat.toDouble())
			}
		}
	}
}