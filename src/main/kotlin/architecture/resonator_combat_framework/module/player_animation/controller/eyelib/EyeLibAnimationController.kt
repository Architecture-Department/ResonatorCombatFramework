package architecture.resonator_combat_framework.module.player_animation.controller.eyelib

import architecture.resonator_combat_framework.module.player_animation.config.AnimationPlayConfig
import architecture.resonator_combat_framework.module.player_animation.config.ProxyBoneConfigLoader
import architecture.resonator_combat_framework.module.player_animation.controller.BaseAnimationController
import architecture.resonator_combat_framework.module.player_animation.util.EyeLibUtil
import io.github.tt432.eyelib.capability.RenderData
import io.github.tt432.eyelib.capability.component.AnimationComponent
import io.github.tt432.eyelib.capability.component.ClientEntityComponent
import io.github.tt432.eyelib.client.animation.AnimationEffects
import io.github.tt432.eyelib.client.animation.BrAnimator
import io.github.tt432.eyelib.client.animation.bedrock.BrAnimationEntry
import io.github.tt432.eyelib.client.animation.bedrock.BrLoopType
import io.github.tt432.eyelib.client.render.bone.BoneRenderInfos
import io.github.tt432.eyelib.molang.MolangValue

/**
 * eyelib 动画控制器。
 *
 * 继承 BaseAnimationController 的状态机和过渡系统，
 * 只实现 eyelib 特有的后端操作。
 */
class EyeLibAnimationController(
	private val renderData: RenderData<*>,
	isClient: Boolean
) : BaseAnimationController(isClient, ProxyBoneConfigLoader.getInstance(isClient)) {

	private val activeAnimations = linkedMapOf<String, MolangValue>()
	private val boneController = EyelibBoneController()
	private val itemController = EyelibItemController()

	// ═══════════ 后端实现 ═══════════

	override fun loadAnimation(animId: String): Boolean {
		val anim = EyeLibUtil.getAnimation(isClient, animId) ?: return false
		activeAnimations.clear()
		activeAnimations[animId] = MolangValue.getConstant(speedMultiplier)
		return true
	}

	override fun syncToBackend(animIds: List<String>, multipliers: List<Float>) {
		if (!isClient) return
		val names = mutableMapOf<String, String>()
		val mults = mutableMapOf<String, MolangValue>()
		for ((i, id) in animIds.withIndex()) {
			val anim = EyeLibUtil.getAnimation(true, id) ?: continue
			names[id] = anim.name()
			mults[id] = MolangValue.getConstant(multipliers.getOrElse(i) { 1f })
		}
		EyeLibUtil.animateSetup(renderData, names, mults)
	}

	override fun freezeAllAtFrameZero() {
		if (!isClient) return
		for (animId in activeAnimations.keys) EyeLibUtil.resetAnimData(renderData, animId)
	}

	override fun setAnimStartTime(animId: String, timeSec: Float) {
		if (isClient) EyeLibUtil.setAnimTime(renderData, animId, timeSec)
	}

	override fun getPlaybackInfo(animId: String): PlaybackInfo? {
		val anim = EyeLibUtil.getAnimation(isClient, animId) as? BrAnimationEntry ?: return null
		val data = EyeLibUtil.getEntryData(renderData, animId) ?: return null
		val loopType = when (anim.loop()) {
			BrLoopType.ONCE -> LoopType.ONCE
			BrLoopType.LOOP -> LoopType.LOOP
			BrLoopType.HOLD_ON_LAST_FRAME -> LoopType.HOLD_ON_LAST
			else -> LoopType.ONCE
		}
		return PlaybackInfo(data.animTime, anim.animationLength(), loopType)
	}

	override fun tickBackend(gameTime: Float) {
		val ac: AnimationComponent = renderData.animationComponent
		val cec: ClientEntityComponent = renderData.clientEntityComponent
		val effects = AnimationEffects()

		val infos = if (ac.getSerializableInfo() != null) {
			BrAnimator.tickAnimation(ac, renderData.scope, effects, gameTime) {
				val ce = cec.clientEntity ?: return@tickAnimation
				ce.scripts().ifPresent { it.pre_animation().eval(renderData.scope) }
			}
		} else BoneRenderInfos.EMPTY

		boneController.writeToProxy(infos, proxyModel)
		val la = proxyModel.getBone("left_arm")
		val ra = proxyModel.getBone("right_arm")
		if (la != null && ra != null) itemController.writeToProxy(infos, la, ra)
	}

	override fun resetAnimAndRestart(config: AnimationPlayConfig) {
		if (!isClient) return
		EyeLibUtil.resetAnimData(renderData, config.animId)
	}
}
