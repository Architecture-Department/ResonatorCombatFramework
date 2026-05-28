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
import io.github.tt432.eyelib.client.model.GlobalBoneIdHandler
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

	/// 原始游戏时间跟踪（用于速度缩放）
	private var lastRawGameTime = -1f
	private var scaledGameTime = 0f

	// ═══════════ 后端实现 ═══════════

	override fun loadAnimation(animId: String): Boolean {
		EyeLibUtil.getAnimation(isClient, animId) ?: return false
		activeAnimations.clear()
		activeAnimations[animId] = MolangValue.getConstant(speedMultiplier)
		return true
	}

	override fun syncToBackend(animIds: List<String>, multipliers: List<Float>) {
		val names = mutableMapOf<String, String>()
		val mults = mutableMapOf<String, MolangValue>()
		for ((i, id) in animIds.withIndex()) {
			val anim = EyeLibUtil.getAnimation(isClient, id) ?: continue
			names[id] = anim.name()
			// 速度由时间线缩放控制，animate multiplier 固定为 1（仅用作混合权重）
			mults[id] = MolangValue.getConstant(1f)
		}
		EyeLibUtil.animateSetup(renderData, names, mults)
	}

	override fun freezeAllAtFrameZero() {
		for (animId in activeAnimations.keys) EyeLibUtil.resetAnimData(renderData, animId)
		lastRawGameTime = -1f
	}

	override fun setAnimStartTime(animId: String, timeSec: Float) {
		EyeLibUtil.setAnimTime(renderData, animId, timeSec)
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

		// 用缩放后的时间线驱动 EyeLib 动画，实现速度控制
		val scaledTicks = if (lastRawGameTime < 0f) {
			lastRawGameTime = gameTime
			scaledGameTime = gameTime
			gameTime
		} else {
			val delta = gameTime - lastRawGameTime
			scaledGameTime += delta * speedMultiplier
			lastRawGameTime = gameTime
			scaledGameTime
		}

		val infos = if (ac.getSerializableInfo() != null) {
			BrAnimator.tickAnimation(ac, renderData.scope, effects, scaledTicks) {
				val ce = cec.clientEntity ?: return@tickAnimation
				ce.scripts().ifPresent { it.pre_animation().eval(renderData.scope) }
			}
		} else BoneRenderInfos.EMPTY

		// 自动检测当前动画实际控制的骨骼
		affectedBones = infos.infos.keys.mapNotNull { GlobalBoneIdHandler.get(it) }.toSet()

		boneController.writeToProxy(infos, proxyModel)
	}

	override fun resetAnimAndRestart(config: AnimationPlayConfig) {
		EyeLibUtil.resetAnimData(renderData, config.animId)
		lastRawGameTime = -1f
	}
}