package architecture.resonator_combat_framework.module.player_animation.controller.eyelib

import architecture.resonator_combat_framework.module.player_animation.api.ProxyModel
import architecture.resonator_combat_framework.module.player_animation.config.ProxyBoneConfigData
import architecture.resonator_combat_framework.module.player_animation.config.ProxyBoneConfigLoader
import architecture.resonator_combat_framework.module.player_animation.controller.IAnimationController
import architecture.resonator_combat_framework.module.player_animation.util.EyeLibUtil
import io.github.tt432.eyelib.capability.RenderData
import io.github.tt432.eyelib.capability.component.AnimationComponent
import io.github.tt432.eyelib.capability.component.ClientEntityComponent
import io.github.tt432.eyelib.client.ClientTickHandler
import io.github.tt432.eyelib.client.animation.Animation
import io.github.tt432.eyelib.client.animation.AnimationEffects
import io.github.tt432.eyelib.client.animation.BrAnimator
import io.github.tt432.eyelib.client.animation.bedrock.BrAnimationEntry
import io.github.tt432.eyelib.client.animation.bedrock.BrLoopType
import io.github.tt432.eyelib.client.render.bone.BoneRenderInfos
import io.github.tt432.eyelib.molang.MolangValue

/**
 * eyelib animation controller | simplified state machine.
 *
 * Design:
 * - applyProxyBone IS the transition; no internal crossfade needed
 * - trigger: clear old anim, start new at frame 0 (frozen), fade in via blendFactor 0->1
 * - stop: set blendTarget=0, animation keeps ticking for fade-out, then clear
 * - stopImmediate: instant clear, no transition
 * - pause: freeze eyelib tick + pose
 * - resume: unfreeze
 *
 * States: IDLE -> TRANSITIONING -> PLAYING
 *         PLAYING -> FADING_OUT -> IDLE
 *         any -> PAUSED -> previous
 */
class EyeLibAnimationController(
	private val renderData: RenderData<*>,
	private val isClient: Boolean
) : IAnimationController {

	private enum class State { IDLE, TRANSITIONING, PLAYING, PAUSED, FADING_OUT }

	val proxyModel = ProxyModel("eyelib")
	private var state = State.IDLE

	// === params ===
	override var blendFactor = 0f
	override var blendTarget = 0f
	override var currentTransitionTicks = ProxyBoneConfigData.DEFAULT_TRANSITION_TICKS
	override var speedMultiplier = 1f
	override var priority = 0
	override var isOverriding = true
	override var currentAnimId: String? = null
	override var affectedBones = emptySet<String>()

	// === eyelib ===
	private val activeAnimations = linkedMapOf<String, Animation<*>>()
	private val activeMultipliers = mutableMapOf<String, MolangValue>()
	private val configLoader = ProxyBoneConfigLoader.getInstance(isClient)
	private val boneController = EyelibBoneController()
	private val itemController = EyelibItemController()
	private var lastTickSec = 0f

	override fun isActive(): Boolean = state != State.IDLE

	// ===================== trigger =====================

	override fun trigger(animId: String, transitionTicks: Int) {
		trigger(animId, transitionTicks, speedMultiplier)
	}

	override fun trigger(animId: String, transitionTicks: Int, speedMultiplier: Float) {
		val anim = EyeLibUtil.getAnimation(isClient, animId) ?: return
		currentTransitionTicks = transitionTicks
		this.speedMultiplier = speedMultiplier
		val config = configLoader.getConfig(animId)
		val multiplier = MolangValue.getConstant(speedMultiplier)

		if (activeAnimations.containsKey(animId)) {
			restartInternal(animId, config, multiplier)
			return
		}

		// clear previous animation unconditionally
		activeAnimations.clear(); activeMultipliers.clear()
		// clear proxyModel: old bones must not leak into new animation
		proxyModel.bones.clear()

		activeAnimations[animId] = anim
		activeMultipliers[animId] = multiplier
		currentAnimId = animId
		affectedBones = config.resolveCurrentBoneNames()

		state = State.TRANSITIONING
		blendFactor = 0f; blendTarget = 1f
		lastTickSec = 0f
		freezeAllAtFrameZero()
		rebuildAnimate()
	}

	override fun triggerForDuration(
		animId: String, transitionTicks: Int, durationTicks: Int, originalAnimLengthSec: Float
	) {
		val desiredSec = durationTicks / 20f
		val m = if (desiredSec > 0f) originalAnimLengthSec / desiredSec else 1f
		trigger(animId, transitionTicks, m)
	}

	// ===================== stop =====================

	override fun stop() {
		if (state == State.IDLE || state == State.FADING_OUT) return
		state = State.FADING_OUT
		blendTarget = 0f
		if (currentTransitionTicks <= 0) forceClear()
	}

	override fun stopImmediate() {
		forceClear()
	}

	// ===================== pause / resume =====================

	override fun pause() {
		if (state == State.PLAYING || state == State.TRANSITIONING) state = State.PAUSED
	}

	override fun resume() {
		if (state == State.PAUSED) state = if (isInFadeIn()) State.TRANSITIONING else State.PLAYING
	}

	override fun stopAnimation(animId: String) {
		activeAnimations.remove(animId); activeMultipliers.remove(animId)
		if (activeAnimations.isEmpty()) forceClear()
	}

	override fun restartAnimation(animId: String) {
		restartInternal(animId, configLoader.getConfig(animId), MolangValue.getConstant(speedMultiplier))
	}

	// ===================== tick =====================

	override fun tick(partialTick: Float, deltaSec: Float) {
		if (!isClient) return
		val ticks = (ClientTickHandler.getTick() + partialTick) / 20
		val dSec = if (lastTickSec == 0f) 0f else ticks - lastTickSec
		lastTickSec = ticks

		tickBlend(dSec)

		val shouldTickEyelib = state != State.IDLE && state != State.PAUSED

		// stale-bone removal: only keep bones that current animation declares
		if (affectedBones.isNotEmpty()) {
			val toRemove = proxyModel.bones.keys.filter { it !in affectedBones }.toList()
			for (name in toRemove) proxyModel.bones.remove(name)
		}

		if (!shouldTickEyelib) return

		val ac: AnimationComponent = renderData.animationComponent
		val cec: ClientEntityComponent = renderData.clientEntityComponent
		val effects = AnimationEffects()

		val infos = if (ac.getSerializableInfo() != null) {
			BrAnimator.tickAnimation(ac, renderData.scope, effects, ticks) {
				val ce = cec.clientEntity ?: return@tickAnimation
				ce.scripts().ifPresent { it.pre_animation().eval(renderData.scope) }
			}
		} else BoneRenderInfos.EMPTY

		// freeze at frame 0 during transition
		if (state == State.TRANSITIONING) freezeAllAtFrameZero()

		checkOnceAnimations()

		boneController.writeToProxy(infos, proxyModel)
		val la = proxyModel.getBone("left_arm")
		val ra = proxyModel.getBone("right_arm")
		if (la != null && ra != null) itemController.writeToProxy(infos, la, ra)

		// state transitions from blendFactor
		if (state == State.TRANSITIONING && blendFactor >= 1f) state = State.PLAYING
		if (state == State.FADING_OUT && blendFactor <= 0f) forceClear()

		ac.tickedInfos = infos; ac.effects = effects
	}

	// ===================== internal =====================

	private fun restartInternal(animId: String, config: ProxyBoneConfigData, multiplier: MolangValue) {
		activeMultipliers[animId] = multiplier
		currentAnimId = animId; affectedBones = config.resolveCurrentBoneNames()
		state = State.PLAYING; blendTarget = 1f; blendFactor = 1f
		if (isClient) EyeLibUtil.resetAnimData(renderData, animId)
		lastTickSec = 0f
	}

	private fun checkOnceAnimations() {
		for ((animId, anim) in activeAnimations.toList()) {
			if (anim is BrAnimationEntry && anim.loop() == BrLoopType.ONCE) {
				val data = EyeLibUtil.getEntryData(renderData, animId) ?: continue
				val effectiveTime = data.animTime * speedMultiplier
				if (effectiveTime > anim.animationLength()) stop()
			}
		}
	}

	private fun tickBlend(ds: Float) {
		if (blendFactor == blendTarget) return
		if (currentTransitionTicks <= 0) {
			blendFactor = blendTarget; return
		}
		val step = (ds * 20f) / currentTransitionTicks
		blendFactor = if (blendFactor < blendTarget)
			(blendFactor + step).coerceAtMost(blendTarget)
		else
			(blendFactor - step).coerceAtLeast(blendTarget)
	}

	private fun forceClear() {
		state = State.IDLE
		blendFactor = 0f; blendTarget = 0f
		activeAnimations.clear(); activeMultipliers.clear()
		proxyModel.bones.clear()
		currentAnimId = null; affectedBones = emptySet()
		lastTickSec = 0f
		rebuildAnimate()
	}

	private fun isInFadeIn(): Boolean = blendTarget > 0f && blendFactor < 1f

	private fun freezeAllAtFrameZero() {
		if (!isClient) return
		for (animId in activeAnimations.keys) EyeLibUtil.resetAnimData(renderData, animId)
	}

	private fun rebuildAnimate() {
		if (!isClient) return
		EyeLibUtil.animateSetup(
			renderData,
			activeAnimations.mapValues { it.value.name() },
			activeMultipliers.toMap()
		)
	}
}
