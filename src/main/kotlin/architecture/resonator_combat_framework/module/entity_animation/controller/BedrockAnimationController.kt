// Bedrock 动画控制器实现。加载 BedrockAnimation，每帧通过 MolangQueries 设置查询值后求值 animTimeUpdate 表达式，然后将骨骼变换写入 ProxyModel
package architecture.resonator_combat_framework.module.entity_animation.controller

import architecture.resonator_combat_framework.module.entity_animation.data.AnimationPlayData
import architecture.resonator_combat_framework.module.entity_animation.engine.BedrockAnimation
import architecture.resonator_combat_framework.module.entity_animation.engine.BedrockAnimator
import architecture.resonator_combat_framework.module.entity_animation.engine.molang.MolangQueries
import architecture.resonator_combat_framework.module.entity_animation.registry.BedrockAnimationRegistry
import net.minecraft.resources.ResourceLocation

class BedrockAnimationController(
	id: ResourceLocation,
	isClient: Boolean
) : BaseAnimationController(id, isClient) {

	/** 当前加载的动画数据 */
	private var currentAnim: BedrockAnimation? = null

	/** 当前动画播放位置（秒） */
	private var animTime = 0f

	/** 上一帧的 gameTime，-1 表示首帧 */
	private var lastRawGameTime = -1f

	override fun loadAnimation(animId: String): Boolean {
		currentAnim = BedrockAnimationRegistry.getInstance(isClient).get(animId)
		return currentAnim != null
	}

	override fun syncToBackend(animIds: List<String>, multipliers: List<Float>) {}

	override fun freezeAllAtFrameZero() {
		animTime = 0f
		lastRawGameTime = -1f
	}

	override fun setAnimStartTime(animId: String, timeSec: Float) {
		animTime = timeSec
	}

	override fun getPlaybackInfo(animId: String): PlaybackInfo? {
		val anim = currentAnim ?: return null
		return PlaybackInfo(animTime, anim.length, anim.loop.toBaseLoopType())
	}

	override fun tickBackend(gameTime: Float) {
		val anim = currentAnim ?: return

		val scaledDelta: Float
		if (lastRawGameTime < 0f) {
			lastRawGameTime = gameTime
			scaledDelta = 0f
			animTime = 0f
		} else {
			val delta = gameTime - lastRawGameTime
			lastRawGameTime = gameTime
			scaledDelta = delta * speedMultiplier
		}

		// 使用 MoLang 表达式推进时间（或默认 time += delta）
		val expr = anim.animTimeUpdate
		if (expr != null) {
			MolangQueries.setVariable("query.anim_time") { animTime.toDouble() }
			MolangQueries.setVariable("query.delta_time") { scaledDelta.toDouble() }
			animTime = expr.get().toFloat()
		} else {
			animTime += scaledDelta
		}

		affectedBones = BedrockAnimator.computeAndWrite(anim, animTime, proxyModel)
	}

	override fun resetAnimAndRestart(config: AnimationPlayData) {
		animTime = 0f
	}
}

fun BedrockAnimation.LoopType.toBaseLoopType() = when (this) {
	BedrockAnimation.LoopType.ONCE -> BaseAnimationController.LoopType.ONCE
	BedrockAnimation.LoopType.LOOP -> BaseAnimationController.LoopType.LOOP
	BedrockAnimation.LoopType.HOLD_ON_LAST -> BaseAnimationController.LoopType.HOLD_ON_LAST
}
