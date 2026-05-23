package architecture.resonator_combat_framework.module.player_animation.client

import architecture.resonator_combat_framework.core.Rcf
import architecture.resonator_combat_framework.module.player_animation.config.RcfBoneConfig
import architecture.resonator_combat_framework.module.player_animation.util.EyeLibUtil
import io.github.tt432.eyelib.client.animation.Animation
import io.github.tt432.eyelib.client.animation.AnimationEffects
import io.github.tt432.eyelib.client.animation.bedrock.BrAnimationEntry
import io.github.tt432.eyelib.client.manager.AnimationManager
import io.github.tt432.eyelib.client.render.bone.BoneRenderInfos
import io.github.tt432.eyelib.molang.MolangScope

class RcfPlayerAnimationBridge private constructor() : Animation<RcfPlayerAnimationBridge.BridgeData> {

	class BridgeData {
		var activeAnimationId: String? = null
		var activeBoneConfig: RcfBoneConfig = RcfBoneConfig.EMPTY
		var previousAnimationId: String? = null
		var crossFadeProgress: Float = 1f

		private val entryDataCache = mutableMapOf<String, BrAnimationEntry.Data>()

		fun getEntryData(animId: String): BrAnimationEntry.Data {
			return entryDataCache.getOrPut(animId) {
				val entry = getAnimationManager().get(animId) as BrAnimationEntry
				entry.createData()
			}
		}
	}

	override fun name(): String = NAME

	override fun createData(): BridgeData = BridgeData()

	override fun onFinish(data: BridgeData) {
		data.activeAnimationId?.let { finishEntry(it, data) }
		data.previousAnimationId?.let { finishEntry(it, data) }
	}

	private fun finishEntry(animId: String, data: BridgeData) {
		val entry = getAnimationManager().get(animId) as? BrAnimationEntry ?: return
		entry.onFinish(data.getEntryData(animId))
	}

	override fun anyAnimationFinished(data: BridgeData): Boolean {
		val animId = data.activeAnimationId ?: return true
		val entry = getAnimationManager().get(animId) as? BrAnimationEntry ?: return true
		return entry.anyAnimationFinished(data.getEntryData(animId))
	}

	override fun allAnimationFinished(data: BridgeData): Boolean = anyAnimationFinished(data)

	override fun tickAnimation(
		data: BridgeData,
		animations: MutableMap<String, String>,
		scope: MolangScope,
		ticks: Float,
		multiplier: Float,
		renderInfos: BoneRenderInfos,
		effects: AnimationEffects,
		animationStartFeedback: Runnable
	) {
		val currId = data.activeAnimationId
		val prevId = data.previousAnimationId
		val progress = data.crossFadeProgress.coerceIn(0f, 1f)

		if (currId != null) {
			val entry = getAnimationManager().get(currId) as? BrAnimationEntry
			entry?.tickAnimation(
				data.getEntryData(currId), animations, scope, ticks,
				multiplier * progress, renderInfos, effects, animationStartFeedback
			)
		}

		if (prevId != null && progress < 1f) {
			val entry = getAnimationManager().get(prevId) as? BrAnimationEntry
			entry?.tickAnimation(
				data.getEntryData(prevId), animations, scope, ticks,
				multiplier * (1f - progress), renderInfos, effects, animationStartFeedback
			)
		}

		if (prevId != null && progress >= 1f) {
			data.previousAnimationId = null
		}
	}

	companion object {
		val NAME = Rcf.modRlText("player_animation_bridge")
		val INSTANCE = RcfPlayerAnimationBridge()

		fun register() {
			getAnimationManager().put(NAME, INSTANCE)
		}
	}
}

private fun getAnimationManager(): AnimationManager =
	EyeLibUtil.getAnimationManager(true)