package architecture.resonator_combat_framework.module.player_animation.bedrock

import architecture.resonator_combat_framework.module.player_animation.bedrock.molang.MathValue
import org.joml.Vector3f

data class BedrockAnimation(
	val animId: String,
	val loop: LoopType,
	val length: Float,
	val bones: Map<String, BrBoneAnimation>,
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
	val time: Float,
	val lerp: LerpMode = LerpMode.LINEAR,
	val value: MolangVector3 = MolangVector3(),
	val pre: MolangVector3 = MolangVector3(),
	val post: MolangVector3 = MolangVector3()
) {
	fun evaluateValue(out: Vector3f = Vector3f()): Vector3f {
		if (!value.allNull()) return value.evaluate(out)
		if (!post.allNull()) return post.evaluate(out)
		if (!pre.allNull()) return pre.evaluate(out)
		return out.set(0f, 0f, 0f)
	}

	fun evaluatePre(out: Vector3f = Vector3f()): Vector3f {
		if (!pre.allNull()) return pre.evaluate(out)
		if (!value.allNull()) return value.evaluate(out)
		if (!post.allNull()) return post.evaluate(out)
		return out.set(0f, 0f, 0f)
	}

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