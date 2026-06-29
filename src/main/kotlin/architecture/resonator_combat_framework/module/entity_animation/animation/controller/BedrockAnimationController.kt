package architecture.resonator_combat_framework.module.entity_animation.animation.controller

import architecture.resonator_combat_framework.core.RcfEventHooks
import architecture.resonator_combat_framework.module.entity_animation.animation.LoopType
import architecture.resonator_combat_framework.module.entity_animation.animation.StaticAnimation
import architecture.resonator_combat_framework.module.entity_animation.animation.baking_animation.BakingBrAnimation
import architecture.resonator_combat_framework.module.entity_animation.animation.controller.IEntityAnimationController.State
import architecture.resonator_combat_framework.module.entity_animation.animation.data.AnimType
import architecture.resonator_combat_framework.module.entity_animation.animation.data.AnimationPlayData
import architecture.resonator_combat_framework.module.entity_animation.animation.data.ProxyBoneConfigData
import architecture.resonator_combat_framework.module.entity_animation.animation.data.shouldBlend
import architecture.resonator_combat_framework.module.entity_animation.animation.mapper.AnimationControllerManager
import architecture.resonator_combat_framework.module.entity_animation.animation.mapper.IEntityAnimationMapperProvider
import architecture.resonator_combat_framework.module.entity_animation.animation.model.BakingBrModel
import architecture.resonator_combat_framework.module.entity_animation.animation.model.ProxyBone
import architecture.resonator_combat_framework.module.entity_animation.animation.model.ProxyModel
import architecture.resonator_combat_framework.module.entity_animation.registry.BedrockAnimationDataRegistry
import architecture.resonator_combat_framework.module.entity_animation.registry.BedrockAnimationRegistry
import architecture.resonator_combat_framework.module.entity_animation.registry.StaticAnimationRegistry
import architecture.resonator_combat_framework.util.RcfUtil
import architecture.resonator_combat_framework.util.TimeUtil
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.Entity

class BedrockAnimationController<T : Entity> @JvmOverloads constructor(
	override val manager: AnimationControllerManager<T>,
	override val id: ResourceLocation,
	protected val isClient: Boolean,
	override val isOverriding: Boolean = true
) : IEntityAnimationController<T> {

	val proxyModel = ProxyModel("base")

	final override var state = State.IDLE; private set
	final override var transitionSource: ProxyModel? = null; private set
	final override var currentConfig: AnimationPlayData = AnimationPlayData.EMPTY; private set

	/** 当前 tick 的动画数据（来自 [animationLoader]，随 [currentAnim] 切换） */
	final override var currentBakingAnim: BakingBrAnimation? = null; private set

	override var localBoneConfig: ProxyBoneConfigData = ProxyBoneConfigData.EMPTY
		set(value) {
			if (currentConfig.mirror) {
				value.mirror()
			}
			field = value
			_activeBoneConfig = null
		}

	/** 当前的骨骼配置 */
	final override var currentBoneConfig: ProxyBoneConfigData = ProxyBoneConfigData.EMPTY
		private set(value) {
			field = value
			_activeBoneConfig = value
		}

	/** 当前生效的骨骼配置缓存 */
	private var _activeBoneConfig: ProxyBoneConfigData? = null

	final override val activeBoneConfig: ProxyBoneConfigData
		get() {
			if (_activeBoneConfig == null) {
				_activeBoneConfig = currentBoneConfig.merge(localBoneConfig)
			}
			return _activeBoneConfig!!
		}

	final override var blendFactor = 0f; private set
	final override var blendTarget = 0f; private set
	final override var currentTransitionTicks = ProxyBoneConfigData.DEFAULT_TRANSITION_TICKS; private set
	final override var speedMultiplier = 1f
	final override var affectedBones = emptySet<String>(); private set

	protected final var advanceTickCount = 0L; private set
	final override var currentAnim: StaticAnimation? = null; private set
	private final val firedEvents = mutableSetOf<String>()
	final override var extraModel: BakingBrModel? = null; private set
	protected final var animTime = 0f; private set
	protected final var lastRawGameTime = -1f; private set

	override val currentAnimTime: Float get() = currentAnim?.let { animTime } ?: 0f
	override val effectiveWeight: Float get() = if (transitionSource != null) 1f else blendFactor
	override val isFadingOut: Boolean get() = state == State.TRANSITIONING
	override val isFadingIn: Boolean get() = state == State.ANIMATION_TRANSITIONING

	// ===== 动画数据查询 =====

	private fun currentLength(): Float = currentBakingAnim?.length ?: 0f
	private fun currentLoopType(): LoopType = currentBakingAnim?.loop ?: LoopType.ONCE

	override fun isActive(): Boolean = state != State.IDLE

	// ===== 触发 =====

	override fun trigger(animId: String, config: AnimationPlayData) {
		val anim = StaticAnimationRegistry.find(animId)
		if (anim == null || anim.get() == null) {
			RcfUtil.LOGGER.warn("[AnimDebug] Animation not found: $animId")
			return
		}
		trigger(anim.get()!!, config)
	}

	override fun trigger(anim: StaticAnimation, config: AnimationPlayData) {
		RcfEventHooks.AnimationTriggerPre(this, anim, config)

		val oldActionAnim = currentAnim
		oldActionAnim?.onEnd(manager.holder)

		currentAnim = anim
		val baseAnim = BedrockAnimationRegistry.find(anim.animationId) ?: BakingBrAnimation.EMPTY
		currentBakingAnim = if (config.mirror) baseAnim.mirror() else baseAnim
		val baseConfig = BedrockAnimationDataRegistry.find(anim.animationId) ?: ProxyBoneConfigData.EMPTY
		currentBoneConfig = if (config.mirror) baseConfig.mirror() else baseConfig
		manager.clearEmittersFor(id)
		proxyModel.bones.clear()
		firedEvents.clear()
		speedMultiplier = config.resolveSpeedMultiplier()
		currentConfig = config

		snapshotTransitionSource()
		currentTransitionTicks = config.resolveFadeInTicks(activeBoneConfig.getFadeInTicks())
		state = State.ANIMATION_TRANSITIONING
		blendFactor = 0f
		blendTarget = 1f

		// 根据播放方向设置初始动画时间
		animTime = if (speedMultiplier >= 0) 0f else calcEndSecond()
		if (config.startTime > 0) animTime = config.startTime / 20f
		lastRawGameTime = -1f
		affectedBones = anim.computeAndWrite(currentBakingAnim!!, animTime, proxyModel, currentData)
		if (transitionSource != null) crossfadeStep()

		anim.onBegin(manager.holder)
		anim.tick(manager.holder, animTime, 0f, proxyModel, manager.brModel)
		extraModel = activeBoneConfig.extraModel
		manager.rebuildBones()

		RcfEventHooks.AnimationTriggerPost(this, anim, config)
	}

	// ===== 停止 =====

	override fun stop(fadeOutTicks: Int) {
		if (state == State.IDLE || state == State.TRANSITIONING) return
		manager.clearEmittersFor(id)
		state = State.TRANSITIONING
		blendTarget = 0f
		transitionSource = null
		currentTransitionTicks = if (fadeOutTicks >= 0) fadeOutTicks
		else currentConfig.resolveFadeOutTicks(activeBoneConfig.getFadeOutTicks())
		if (currentTransitionTicks <= 0) forceClear()
	}

	override fun pause() {
		if (state == State.PLAYING || state == State.ANIMATION_TRANSITIONING) {
			state = State.PAUSED; transitionSource = null
		}
	}

	override fun resume() {
		if (state != State.PAUSED) return
		if (currentAnim == null) {
			state = if (isInFadeIn()) State.ANIMATION_TRANSITIONING else State.PLAYING
			return
		}

		if (speedMultiplier >= 0) {
			if (animTime * speedMultiplier >= currentLength() && currentLoopType() == LoopType.HOLD_ON_LAST) return
		} else {
			if (animTime <= 0f && currentLoopType() == LoopType.HOLD_ON_LAST) return
		}
		state = if (isInFadeIn()) State.ANIMATION_TRANSITIONING else State.PLAYING
	}

	fun tickHandler(manager: IEntityAnimationMapperProvider<T, *>) {}

	// ===== crossfade =====

	private fun snapshotTransitionSource() {
		if (state == State.IDLE || proxyModel.bones.isEmpty()) return
		transitionSource = ProxyModel("src")
		for ((name, bone) in proxyModel.bones) {
			val copy = ProxyBone(name)
			copy.pos.set(bone.pos); copy.rotation.set(bone.rotation); copy.scale.set(bone.scale)
			if (bone.hasPos()) copy.setPosEmpty(false)
			if (bone.hasRot()) copy.setRotEmpty(false)
			if (bone.hasScale()) copy.setScaleEmpty(false)
			transitionSource!!.addBone(copy)
		}
	}

	private fun crossfadeStep() {
		val src = transitionSource ?: return
		val boneFlags = activeBoneConfig.resolveBoneFlags(currentAnimTime)
		for ((name, bone) in proxyModel.bones) {
			if (name !in affectedBones) bone.pos.set(0f); bone.rotation.set(0f); bone.scale.set(1f)
		}

		for ((name, _) in proxyModel.bones) {
			if (src.getBone(name) == null) src.addBone(ProxyBone(name))
		}

		for ((name, _) in src.bones) {
			if (proxyModel.getBone(name) == null) proxyModel.addBone(ProxyBone(name))
		}

		for ((name, fromBone) in src.bones) {
			val toBone = proxyModel.getBone(name) ?: continue
			if (!boneFlags[name].shouldBlend()) {
				fromBone.pos.set(toBone.pos); fromBone.rotation.set(toBone.rotation); fromBone.scale.set(toBone.scale)
				continue
			}

			fromBone.pos.lerp(toBone.pos, blendFactor, toBone.pos)
			fromBone.rotation.lerp(toBone.rotation, blendFactor, toBone.rotation)
			fromBone.scale.lerp(toBone.scale, blendFactor, toBone.scale)
			if (fromBone.hasPos() || toBone.hasPos()) toBone.setPosEmpty(false)
			if (fromBone.hasRot() || toBone.hasRot()) toBone.setRotEmpty(false)
			if (fromBone.hasScale() || toBone.hasScale()) toBone.setScaleEmpty(false)
		}
	}

	// ===== 播放边界 =====

	private fun checkPlaybackBounds() {
		if (state != State.PLAYING) return
		val anim = currentAnim ?: return

		if (speedMultiplier < 0) {
			if (animTime > 0f) return
		} else {
			val endSec = calcEndSecond()
			if (animTime < endSec || endSec <= 0f) return
		}

		when (currentConfig.animType) {
			AnimType.PLAY_ONCE, AnimType.DEFAULT -> when {
				currentLoopType() == LoopType.ONCE -> startFadeOut()
				currentLoopType() == LoopType.LOOP -> resetAnimAndRestart()
				else -> pause()
			}

			AnimType.LOOP -> {
				resetAnimAndRestart(); if (currentConfig.startTime > 0) setAnimStartTime(currentConfig.startTime / 20f)
			}

			AnimType.STOP_AT_LAST -> pause()
		}
	}

	private fun startFadeOut() {
		pause()
		state = State.TRANSITIONING
		blendTarget = 0f
		currentTransitionTicks = currentConfig.resolveFadeOutTicks(activeBoneConfig.getFadeOutTicks())
		RcfEventHooks.AnimationComplete(this)
	}

	private fun calcEndSecond(): Float {
		val config = currentConfig
		val animLength = currentLength()
		if (animLength <= 0f) return 0f
		return when {
			config.endTime < 0 -> animLength + config.endTime / 20f
			config.endTime > 0 -> config.endTime / 20f
			else -> animLength
		}
	}

	fun resetAnimAndRestart() {
		animTime = if (speedMultiplier >= 0) 0f else calcEndSecond()
		firedEvents.clear()
	}

	// ===== 游戏刻推进 =====

	final override fun tick() {
		if (RcfEventHooks.AnimationControllerTickPre(id, this, manager.mapperProvider)) return
		tickHandlerCall()
		if (state == State.IDLE || state == State.PAUSED) {
			RcfEventHooks.AnimationControllerTickPost(id, this, manager.mapperProvider)
			return
		}
		checkPlaybackBounds()
		tickBlend()
		if (state == State.PLAYING) {
			advanceTickCount++; tickBackend(advanceTickCount / 20f)
		} else tickBackend(0f, freezeTime = true)
		handleStateTransition()
		RcfEventHooks.AnimationControllerTickPost(id, this, manager.mapperProvider)
	}

	override fun tickAdvance() {
		val anim = currentAnim ?: return
		anim.tickAdvance(manager.holder, currentAnimTime, proxyModel, manager.brModel, manager.mergedProxy, this)
	}

	private fun tickHandlerCall() {
		if (RcfEventHooks.AnimationControllerTickHandlerPre(id, this, manager.mapperProvider)) return
		tickHandler(manager.mapperProvider)
		RcfEventHooks.AnimationControllerTickHandlerPost(id, this, manager.mapperProvider)
	}

	private fun handleStateTransition() {
		if (state == State.ANIMATION_TRANSITIONING && transitionSource != null) crossfadeStep()
		if (state == State.ANIMATION_TRANSITIONING && blendFactor >= 1f) {
			state = State.PLAYING; transitionSource = null; lastRawGameTime = advanceTickCount / 20f
		}
		if (state == State.TRANSITIONING && blendFactor <= 0f) forceClear()
	}

	override fun tickRender(deltaSec: Float) {
		if (state != State.PLAYING) return
		if (affectedBones.isNotEmpty()) proxyModel.bones.keys.filter { it !in affectedBones }
			.forEach { proxyModel.bones.remove(it) }
	}

	// ===== 动画后端 =====

	fun freezeAllAtFrameZero() {
		animTime = 0f; lastRawGameTime = -1f
	}

	fun setAnimStartTime(timeSec: Float) {
		animTime = timeSec
	}

	fun tickBackend(gameTime: Float, freezeTime: Boolean = false) {
		val anim = currentAnim ?: return
		val data = currentData
		val bakingAnim = currentBakingAnim ?: return
		if (freezeTime) {
			affectedBones = anim.computeAndWrite(bakingAnim, animTime, proxyModel, data)
			manager.queueEvents(this, anim.collectEvents(bakingAnim, animTime, animTime, firedEvents))
			return
		}
		val scaledDelta = calcScaledDelta(gameTime)
		val prevAnimTime = animTime
		animTime = anim.tickAnimTime(animTime, scaledDelta)
		data.updateAnimQueries(animTime, scaledDelta)
		affectedBones = anim.computeAndWrite(bakingAnim, animTime, proxyModel, data)
		anim.tick(manager.holder, animTime, scaledDelta, proxyModel, manager.brModel)
		manager.queueEvents(this, anim.collectEvents(bakingAnim, animTime, prevAnimTime, firedEvents))
	}

	private fun calcScaledDelta(gameTime: Float): Float {
		if (lastRawGameTime < 0f) {
			lastRawGameTime = gameTime; return 0f
		}
		val delta = gameTime - lastRawGameTime; lastRawGameTime = gameTime; return TimeUtil.calcScaledDelta(
			delta,
			speedMultiplier
		)
	}

	// ===== 内部 =====

	private fun tickBlend() {
		if (blendFactor == blendTarget) return
		if (currentTransitionTicks <= 0) {
			blendFactor = blendTarget; return
		}
		val step = 1f / currentTransitionTicks
		blendFactor = if (blendFactor < blendTarget) (blendFactor + step).coerceAtMost(blendTarget)
		else (blendFactor - step).coerceAtLeast(blendTarget)
	}

	private fun forceClear() {
		currentAnim?.onEnd(manager.holder)
		manager.clearEmittersFor(id)
		state = State.IDLE
		blendFactor = 0f
		blendTarget = 0f
		proxyModel.bones.clear()
		transitionSource = null
		currentConfig = AnimationPlayData.EMPTY
		affectedBones = emptySet()
		currentTransitionTicks = ProxyBoneConfigData.DEFAULT_TRANSITION_TICKS
		speedMultiplier = 1f
		extraModel = null
		currentBakingAnim = null
		currentBoneConfig = ProxyBoneConfigData.EMPTY
		_activeBoneConfig = null
		manager.rebuildBones()
	}

	private fun isInFadeIn(): Boolean = isFadingIn
	override fun equalsCurrentAnimId(id: String): Boolean = currentAnim?.animationId == id
}
