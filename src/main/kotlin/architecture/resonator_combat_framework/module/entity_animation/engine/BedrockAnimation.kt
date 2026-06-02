package architecture.resonator_combat_framework.module.entity_animation.engine

import architecture.resonator_combat_framework.module.entity_animation.engine.molang.MathValue
import org.joml.Vector3f

data class BedrockAnimation(
	val animId: String,
	/** 播放模式：ONCE / LOOP / HOLD_ON_LAST */
	val loop: LoopType,
	/** 动画总时长（秒） */
	val length: Float,
	/** 骨骼名 → 骨骼动画数据 */
	val bones: Map<String, BrBoneAnimation>,
	/** MoLang 时间推进表达式，默认 query.anim_time + query.delta_time */
	val animTimeUpdate: MathValue? = null
) {
	enum class LoopType { ONCE, LOOP, HOLD_ON_LAST }
}

data class BrBoneAnimation(
	val pos: List<BrBoneKeyFrame> = emptyList(),
	val rot: List<BrBoneKeyFrame> = emptyList(),
	val scale: List<BrBoneKeyFrame> = emptyList()
)

data class BrBoneKeyFrame(
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
	fun evaluateValue(out: Vector3f = Vector3f()): Vector3f {
		if (!value.allNull()) return value.evaluate(out)
		if (!post.allNull()) return post.evaluate(out)
		if (!pre.allNull()) return pre.evaluate(out)
		return out.set(0f, 0f, 0f)
	}

	/** 取入切线：pre → value → post → (0,0,0) */
	fun evaluatePre(out: Vector3f = Vector3f()): Vector3f {
		if (!pre.allNull()) return pre.evaluate(out)
		if (!value.allNull()) return value.evaluate(out)
		if (!post.allNull()) return post.evaluate(out)
		return out.set(0f, 0f, 0f)
	}

	/** 取出切线：post → value → pre → (0,0,0) */
	fun evaluatePost(out: Vector3f = Vector3f()): Vector3f {
		if (!post.allNull()) return post.evaluate(out)
		if (!value.allNull()) return value.evaluate(out)
		if (!pre.allNull()) return pre.evaluate(out)
		return out.set(0f, 0f, 0f)
	}

	fun hasPreData(): Boolean = pre.allNull().not()
	fun hasPostData(): Boolean = post.allNull().not()

	enum class LerpMode { LINEAR, CATMULLROM, STEP }
}

data class MolangVector3(
	val x: MathValue? = null,
	/** 每个轴可独立为数字或 MoLang 表达式，null 表示"无数据" */
	val y: MathValue? = null,
	val z: MathValue? = null
) {
	fun evaluate(out: Vector3f = Vector3f()): Vector3f {
		return out.set(
			x?.get()?.toFloat() ?: 0f,
			y?.get()?.toFloat() ?: 0f,
			z?.get()?.toFloat() ?: 0f
		)
	}

	fun allNull(): Boolean = x == null && y == null && z == null
}