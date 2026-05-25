package architecture.resonator_combat_framework.module.player_animation.controller.eyelib

import architecture.resonator_combat_framework.module.player_animation.api.ProxyBone
import architecture.resonator_combat_framework.module.player_animation.api.ProxyModel
import architecture.resonator_combat_framework.module.player_animation.config.AnimationPlayConfig
import architecture.resonator_combat_framework.module.player_animation.config.AnimType
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
 * eyelib 动画控制器。
 *
 * 跨动画 crossfade：切换时捕获当前姿态，在 proxyModel 层交叉过渡到新动画首帧。
 * 过渡期间 effectiveWeight 恒为 1.0，applyProxyBone 直接套用混合结果，无二次混合。
 *
 * 状态机：IDLE → TRANSITIONING → PLAYING → FADING_OUT → IDLE（任意非 IDLE 可 → PAUSED）
 */
class EyeLibAnimationController(
	private val renderData: RenderData<*>,
	private val isClient: Boolean
) : IAnimationController {

	private enum class State { IDLE, TRANSITIONING, PLAYING, PAUSED, FADING_OUT }

	val proxyModel = ProxyModel("eyelib")
	private var state = State.IDLE
	private var transitionSource: ProxyModel? = null
	private var currentConfig: AnimationPlayConfig = AnimationPlayConfig.of("")


	override var blendFactor = 0f
	override var blendTarget = 0f
	override var currentTransitionTicks = ProxyBoneConfigData.DEFAULT_TRANSITION_TICKS
	override var speedMultiplier = 1f
	override var priority = 0
	override var isOverriding = true
	override var currentAnimId: String? = null
	override var affectedBones = emptySet<String>()


	private val activeAnimations = linkedMapOf<String, Animation<*>>()
	private val activeMultipliers = mutableMapOf<String, MolangValue>()
	private val configLoader = ProxyBoneConfigLoader.getInstance(isClient)
	private val boneController = EyelibBoneController()
	private val itemController = EyelibItemController()
	private var lastTickSec = 0f

	/** 跨动画过渡时恒为 1.0；否则等于 blendFactor */
	val effectiveWeight: Float get() = if (transitionSource != null) 1f else blendFactor

	override fun isActive(): Boolean = state != State.IDLE

	// 触发

	override fun trigger(config: AnimationPlayConfig) {
		val anim = EyeLibUtil.getAnimation(isClient, config.animId) ?: return
		val multiplier = MolangValue.getConstant(config.resolveSpeedMultiplier())
		val cfg = configLoader.getConfig(config.animId)
		val finalConfig = config.boneConfig ?: cfg

		// 如果同一动画，使用 restart；否则走完整触发流程
		if (activeAnimations.containsKey(config.animId)) {
			restartInternal(config.animId, finalConfig, multiplier)
			return
		}

		// 跨动画过渡源
		if (activeAnimations.isNotEmpty() && proxyModel.bones.isNotEmpty()) {
			transitionSource = ProxyModel("src")
			for ((name, bone) in proxyModel.bones) {
				val copy = ProxyBone(name)
				copy.pos.set(bone.pos); copy.rotation.set(bone.rotation); copy.scale.set(bone.scale)
				transitionSource!!.addBone(copy)
			}
		}

		activeAnimations.clear(); activeMultipliers.clear()
		activeAnimations[config.animId] = anim
		activeMultipliers[config.animId] = multiplier
		currentConfig = config
		speedMultiplier = config.resolveSpeedMultiplier()
		currentAnimId = config.animId
		affectedBones = finalConfig.resolveCurrentBoneNames()

		// 设置起始时间
		if (config.startTime > 0 && isClient) {
			EyeLibUtil.setAnimTime(renderData, config.animId, config.startTime / 20f)
		}

		// 处理动画类型
		applyAnimType(config.animType, anim)

		// 淡入
		currentTransitionTicks = config.resolveFadeInTicks(cfg.transitionTicks)
		state = State.TRANSITIONING
		blendFactor = 0f; blendTarget = 1f
		lastTickSec = 0f
		freezeAllAtFrameZero()
		rebuildAnimate()
	}

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

		// 跨动画过渡源：快照当前姿态
		if (activeAnimations.isNotEmpty() && proxyModel.bones.isNotEmpty()) {
			transitionSource = ProxyModel("src")

			for ((name, bone) in proxyModel.bones) {
				val copy = ProxyBone(name)
				copy.pos.set(bone.pos); copy.rotation.set(bone.rotation); copy.scale.set(bone.scale)
				transitionSource!!.addBone(copy)
			}
		}


		activeAnimations.clear(); activeMultipliers.clear()

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

	// 停止

	override fun stop() {
		if (state == State.IDLE || state == State.FADING_OUT) return
		state = State.FADING_OUT
		blendTarget = 0f
		currentTransitionTicks = currentConfig.resolveFadeOutTicks(currentTransitionTicks)
		if (currentTransitionTicks <= 0) forceClear()
	}

	override fun stopImmediate() {
		forceClear()
	}

	// 暂停 / 恢复

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

	// 每帧

	override fun tick(partialTick: Float, deltaSec: Float) {
		if (!isClient) return
		val ticks = (ClientTickHandler.getTick() + partialTick) / 20
		val dSec = if (lastTickSec == 0f) 0f else ticks - lastTickSec
		lastTickSec = ticks

		tickBlend(dSec)

		val shouldTickEyelib = state != State.IDLE && state != State.PAUSED

		// 清除不属于当前动画的骨骼
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

		// 冻结首帧
		if (state == State.TRANSITIONING) freezeAllAtFrameZero()

		checkOnceAnimations()

		boneController.writeToProxy(infos, proxyModel)
		val la = proxyModel.getBone("left_arm")
		val ra = proxyModel.getBone("right_arm")
		if (la != null && ra != null) itemController.writeToProxy(infos, la, ra)

		// 交叉过渡：旧姿态 → 新动画首帧
		if (state == State.TRANSITIONING && transitionSource != null) {
			// 旧动画独有骨骼：每帧重置为 identity
			for ((name, bone) in proxyModel.bones) {
				if (name !in affectedBones) {
					bone.pos.set(0f); bone.rotation.set(0f); bone.scale.set(1f)
				}
			}
			// 新动画骨骼：source 补 identity
			for ((name, _) in proxyModel.bones) {
				if (transitionSource!!.getBone(name) == null)
					transitionSource!!.addBone(ProxyBone(name))
			}
			// 旧动画骨骼：target 补 identity
			for ((name, _) in transitionSource!!.bones) {
				if (proxyModel.getBone(name) == null)
					proxyModel.addBone(ProxyBone(name))
			}
			// lerp 混合
			for ((name, fromBone) in transitionSource!!.bones) {
				val toBone = proxyModel.getBone(name) ?: continue
				fromBone.pos.lerp(toBone.pos, blendFactor, toBone.pos)
				fromBone.rotation.lerp(toBone.rotation, blendFactor, toBone.rotation)
				fromBone.scale.lerp(toBone.scale, blendFactor, toBone.scale)
			}
		}

		// 状态转换
		if (state == State.TRANSITIONING && blendFactor >= 1f) {
			state = State.PLAYING
			transitionSource = null
		}
		if (state == State.FADING_OUT && blendFactor <= 0f) forceClear()

		ac.tickedInfos = infos; ac.effects = effects
	}

	// 内部

	private fun restartInternal(animId: String, config: ProxyBoneConfigData, multiplier: MolangValue) {
		activeMultipliers[animId] = multiplier
		currentAnimId = animId; affectedBones = config.resolveCurrentBoneNames()
		state = State.PLAYING; blendTarget = 1f; blendFactor = 1f
		transitionSource = null
		if (isClient) EyeLibUtil.resetAnimData(renderData, animId)
		lastTickSec = 0f
	}

	private fun applyAnimType(type: AnimType, anim: Animation<*>) {
		if (anim !is BrAnimationEntry) return
		when (type) {
			AnimType.DEFAULT -> {} // 不修改
			AnimType.PLAY_ONCE -> anim.setLoop(BrLoopType.ONCE)
			AnimType.STOP_AT_LAST -> anim.setLoop(BrLoopType.HOLD_ON_LAST_FRAME)
			AnimType.LOOP -> anim.setLoop(BrLoopType.LOOP)
		}
	}

	private fun checkOnceAnimations() {
		for ((animId, anim) in activeAnimations.toList()) {
			if (anim !is BrAnimationEntry) continue
			val data = EyeLibUtil.getEntryData(renderData, animId) ?: continue
			val effectiveTime = data.animTime * speedMultiplier
			val animLength = anim.animationLength()

			// 检查 endTime
			if (currentConfig.endTime != 0) {
				val endSec = if (currentConfig.endTime < 0)
					animLength + currentConfig.endTime / 20f
				else
					currentConfig.endTime / 20f
				if (effectiveTime >= endSec) {
					when (currentConfig.animType) {
						AnimType.STOP_AT_LAST -> pause()
						AnimType.LOOP -> EyeLibUtil.resetAnimData(renderData, animId)
						else -> stop()
					}
					return
				}
			}

			// 默认行为
			when {
				anim.loop() == BrLoopType.ONCE && effectiveTime > animLength -> {
					if (currentConfig.animType == AnimType.STOP_AT_LAST) pause()
					else stop()
				}
				anim.loop() == BrLoopType.HOLD_ON_LAST_FRAME && effectiveTime > animLength -> {
					pause()
				}
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
		transitionSource = null
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
