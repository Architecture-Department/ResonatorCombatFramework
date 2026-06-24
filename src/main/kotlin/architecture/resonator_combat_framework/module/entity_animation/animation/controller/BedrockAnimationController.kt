package architecture.resonator_combat_framework.module.entity_animation.animation.controller

import architecture.resonator_combat_framework.animation.ActionAnimation
import architecture.resonator_combat_framework.module.entity_animation.animation.LoopType
import architecture.resonator_combat_framework.module.entity_animation.animation.StaticAnimation
import architecture.resonator_combat_framework.module.entity_animation.animation.controller.IEntityAnimationController.State
import architecture.resonator_combat_framework.module.entity_animation.animation.data.*
import architecture.resonator_combat_framework.module.entity_animation.animation.mapper.AnimationControllerManager
import architecture.resonator_combat_framework.module.entity_animation.animation.mapper.IEntityAnimationMapper
import architecture.resonator_combat_framework.module.entity_animation.animation.model.BakingBrModel
import architecture.resonator_combat_framework.module.entity_animation.animation.model.ProxyBone
import architecture.resonator_combat_framework.module.entity_animation.animation.model.ProxyModel
import architecture.resonator_combat_framework.module.entity_animation.animation.molang.MolangData
import architecture.resonator_combat_framework.module.entity_animation.event.AnimationControllerEvent
import architecture.resonator_combat_framework.module.entity_animation.registry.BedrockAnimationRegistry
import architecture.resonator_combat_framework.module.entity_animation.registry.ProxyBoneConfigDataRegistry
import architecture.resonator_combat_framework.util.RcfUtil
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.neoforged.neoforge.common.NeoForge

/** 动画控制器——状态机 + crossfade 过渡 + 权重混合 + Bedrock 后端插值 */
class BedrockAnimationController<T : Entity> @JvmOverloads constructor(
	override val manager: AnimationControllerManager<T>,
	override val id: ResourceLocation,
	protected val isClient: Boolean,
	override val isOverriding: Boolean = true
) : IEntityAnimationController<T> {
	/** 动画数据加载器 */
	protected val animationLoader = BedrockAnimationRegistry.getInstance(isClient)

	/** 骨骼配置加载器 */
	protected val configLoader: ProxyBoneConfigDataRegistry = ProxyBoneConfigDataRegistry.getInstance(isClient)

	/** 当前游戏刻的骨骼状态 */
	val proxyModel = ProxyModel("base")

	override var state = State.IDLE
	override var transitionSource: ProxyModel? = null
	override var currentConfig: AnimationPlayData = AnimationPlayData.EMPTY
	override var resolvedBoneConfig: ProxyBoneConfigData? = null
	override var activeBoneConfig: ProxyBoneConfigData = ProxyBoneConfigData.EMPTY
	override var boneConfigs: ProxyBoneConfigData? = null
	override var blendFactor = 0f
	override var blendTarget = 0f
	override var currentTransitionTicks = ProxyBoneConfigData.DEFAULT_TRANSITION_TICKS
	override var speedMultiplier = 1f
	override var currentAnimId: String? = null
	override var affectedBones = emptySet<String>()

	/** 游戏刻推进计数器，用于 tickBackend 计算 delta time */
	protected var advanceTickCount = 0L

	/** 当前加载的 Animation 单例 */
	private var currentAnim: StaticAnimation? = null

	/** 已触发的事件索引集合，避免重复触发 */
	private val firedEvents = mutableSetOf<String>()

	/** 当前动画的额外（骨骼）模型定义 */
	var extraModel: BakingBrModel? = null

	/** 当前 ActionAnimation 的状态修改器 */
	override val activeStateModifiers: Map<ResourceLocation, Boolean>
		get() = (currentAnim as? ActionAnimation)?.stateModifiers ?: emptyMap()

	/** 当前动画播放位置（秒） */
	private var animTime = 0f

	/** 上一帧的 gameTime，-1 表示首帧 */
	private var lastRawGameTime = -1f

	override val currentAnimTime: Float
		get() {
			val info = currentAnimId?.let { getPlaybackInfo() } ?: return 0f
			return info.animTime
		}

	override val effectiveWeight: Float get() = if (transitionSource != null) 1f else blendFactor
	override val isFadingOut: Boolean get() = state == State.TRANSITIONING
	override val isFadingIn: Boolean get() = state == State.ANIMATION_TRANSITIONING

	// ===== 骨骼标志 =====

	/** 合并控制器骨骼标志与配置覆盖标志 */
	fun resolveBoneFlags(animTime: Float): Map<String, ProxyBoneFlags> {
		val flags = activeBoneConfig.resolveBoneFlags(animTime).toMutableMap()
		val overrideFlags = boneConfigs?.resolveBoneFlags(animTime) ?: return flags
		overrideFlags.forEach { (boneName, boneFlag) -> flags[boneName] = boneFlag }
		return flags
	}

	override fun isActive(): Boolean = state != State.IDLE

	/** 播放信息 */
	data class PlaybackInfo(val animTime: Float, val animLength: Float, val loopType: LoopType)

	// ===== 触发 =====

	override fun trigger(animId: String, config: AnimationPlayData) {
		val anim = animationLoader.getStaticAnimation(animId)
		if (anim == null) {
			RcfUtil.LOGGER.warn("[AnimDebug] Animation not found: $animId")
			return
		}
		// 如果外部没有通过 resolvedBoneConfig 注入配置，则在此处加载
		if (resolvedBoneConfig == null) {
			resolvedBoneConfig = config.boneConfig ?: configLoader.getConfig(animId)
		}
		triggerWithAnimation(anim, animId, config)
	}

	/** 使用指定的 [StaticAnimation] 实例触发播放 */
	override fun triggerWithAnimation(anim: StaticAnimation, animId: String, config: AnimationPlayData) {
		// 0. 通知旧动画结束
		val oldActionAnim = currentAnim as? ActionAnimation
		if (oldActionAnim != null) {
			val livingEntity = manager.mapper.holder as? LivingEntity
			if (livingEntity != null) oldActionAnim.onEnd(livingEntity)
		}

		// 1. 设置动画引用
		currentAnim = anim

		// 2. 清除旧状态
		manager.clearEmittersFor(id)
		proxyModel.bones.clear()
		firedEvents.clear()

		// 3. 解析配置
		speedMultiplier = config.resolveSpeedMultiplier()
		val finalConfig = resolvedBoneConfig ?: config.boneConfig ?: configLoader.getConfig(animId)
		resolvedBoneConfig = null
		activeBoneConfig = finalConfig
		currentConfig = config
		currentAnimId = animId

		// 4. 设置过渡状态
		snapshotTransitionSource()
		if (config.startTime > 0) setAnimStartTime(config.startTime / 20f)
		currentTransitionTicks = config.resolveFadeInTicks(finalConfig.getFadeInTicks())
		state = State.ANIMATION_TRANSITIONING
		blendFactor = 0f
		blendTarget = 1f

		// 5. 写入第0帧
		freezeAllAtFrameZero()
		affectedBones = anim.computeAndWrite(animTime, proxyModel, currentData, config.mirror)

		// 6. 跨动画混合
		if (transitionSource != null) crossfadeStep()

		// 7. 收尾
		// ActionAnimation 钩子
		val newActionAnim = anim as? ActionAnimation
		if (newActionAnim != null) {
			val livingEntity = manager.mapper.holder as? LivingEntity
			if (livingEntity != null) newActionAnim.onBegin(livingEntity)
			newActionAnim.modifyPose(proxyModel, animTime)
		}

		extraModel = finalConfig.extraModel
		manager.rebuildBones()
	}

	// ===== 停止 =====

	override fun stop(fadeOutTicks: Int) {
		if (state == State.IDLE || state == State.TRANSITIONING) return
		manager.clearEmittersFor(id)
		state = State.TRANSITIONING
		blendTarget = 0f
		transitionSource = null // 清除 crossfade 源，使 effectiveWeight = blendFactor
		currentTransitionTicks = if (fadeOutTicks >= 0) fadeOutTicks
		else currentConfig.resolveFadeOutTicks(activeBoneConfig.getFadeOutTicks())
		if (currentTransitionTicks <= 0) forceClear()
	}

	// ===== 暂停 / 恢复 =====

	override fun pause() {
		if (state == State.PLAYING || state == State.ANIMATION_TRANSITIONING) {
			state = State.PAUSED
			transitionSource = null
		}
	}

	override fun resume() {
		if (state != State.PAUSED) return
		// HOLD_ON_LAST 且播完则不允许恢复
		val info = currentAnimId?.let { getPlaybackInfo() }
		if (info != null && info.animTime * speedMultiplier >= info.animLength
			&& info.loopType == LoopType.HOLD_ON_LAST
		) return
		state = if (isInFadeIn()) State.ANIMATION_TRANSITIONING else State.PLAYING
	}

	/** tick 处理钩子，子类可重写（如 ActionAnimationController 检测物品切换） */
	fun tickHandler(manager: IEntityAnimationMapper<T, *>) {}

	// ===== crossfade 过渡 =====

	/** 快照当前骨骼为过渡源 */
	private fun snapshotTransitionSource() {
		if (state == State.IDLE || proxyModel.bones.isEmpty()) return
		transitionSource = ProxyModel("src")
		for ((name, bone) in proxyModel.bones) {
			val copy = ProxyBone(name)
			copy.pos.set(bone.pos)
			copy.rotation.set(bone.rotation)
			copy.scale.set(bone.scale)
			if (bone.hasPos()) copy.setPosEmpty(false)
			if (bone.hasRot()) copy.setRotEmpty(false)
			if (bone.hasScale()) copy.setScaleEmpty(false)
			transitionSource!!.addBone(copy)
		}
	}

	/** 跨动画过渡混合：按 blendFactor 在 transitionSource 与 proxyModel 之间 lerp */
	private fun crossfadeStep() {
		val src = transitionSource ?: return
		val boneFlags = activeBoneConfig.resolveBoneFlags(currentAnimTime)
		// 旧动画独有骨骼：每帧重置为 identity
		for ((name, bone) in proxyModel.bones) {
			if (name !in affectedBones) {
				bone.pos.set(0f); bone.rotation.set(0f); bone.scale.set(1f)
			}
		}
		// 补全缺失骨骼（确保新旧集合一致）
		for ((name, _) in proxyModel.bones) {
			if (src.getBone(name) == null) src.addBone(ProxyBone(name))
		}
		for ((name, _) in src.bones) {
			if (proxyModel.getBone(name) == null) proxyModel.addBone(ProxyBone(name))
		}
		for ((name, fromBone) in src.bones) {
			val toBone = proxyModel.getBone(name) ?: continue
			// 不参与混合的骨骼直接使用目标值
			if (!boneFlags[name].shouldBlend()) {
				fromBone.pos.set(toBone.pos); fromBone.rotation.set(toBone.rotation); fromBone.scale.set(toBone.scale)
				continue
			}
			fromBone.pos.lerp(toBone.pos, blendFactor, toBone.pos)
			fromBone.rotation.lerp(toBone.rotation, blendFactor, toBone.rotation)
			fromBone.scale.lerp(toBone.scale, blendFactor, toBone.scale)
			// 根据源/目标数据状态同步 emptyMask
			if (fromBone.hasPos() || toBone.hasPos()) toBone.setPosEmpty(false)
			if (fromBone.hasRot() || toBone.hasRot()) toBone.setRotEmpty(false)
			if (fromBone.hasScale() || toBone.hasScale()) toBone.setScaleEmpty(false)
		}
	}

	// ===== 播放边界检查 =====

	/** 检查动画是否播放完毕，根据 AnimType/LoopType 决定下一状态 */
	private fun checkPlaybackBounds() {
		if (state != State.PLAYING) return
		val info = currentAnimId?.let { getPlaybackInfo() } ?: return
		val animLength = info.animLength
		val endSec = calcEndSecond()
		if (info.animTime < endSec || endSec <= 0f) return

		when (currentConfig.animType) {
			AnimType.PLAY_ONCE, AnimType.DEFAULT -> when {
				isOnceType(info.loopType) -> startFadeOut()
				isLoopType(info.loopType) -> resetAnimAndRestart()
				else -> pause()
			}

			AnimType.LOOP -> {
				resetAnimAndRestart()
				if (currentConfig.startTime > 0) setAnimStartTime(currentConfig.startTime / 20f)
			}

			AnimType.STOP_AT_LAST -> pause()
		}
	}

	private fun isOnceType(loopType: LoopType): Boolean = loopType == LoopType.ONCE
	private fun isLoopType(loopType: LoopType): Boolean = loopType == LoopType.LOOP

	/** 进入淡出状态 */
	private fun startFadeOut() {
		pause()
		state = State.TRANSITIONING
		blendTarget = 0f
		currentTransitionTicks = currentConfig.resolveFadeOutTicks(activeBoneConfig.getFadeOutTicks())
	}

	/** 计算动画结束秒数 */
	private fun calcEndSecond(): Float {
		val config = currentConfig
		val info = currentAnimId?.let { getPlaybackInfo() } ?: return 0f
		return when {
			config.endTime < 0 -> info.animLength + config.endTime / 20f
			config.endTime > 0 -> config.endTime / 20f
			else -> info.animLength
		}
	}

	/** 重置动画时间并从开头重新播放 */
	fun resetAnimAndRestart() {
		animTime = 0f
		firedEvents.clear()
	}

	// ===== 游戏刻推进 =====

	final override fun tickAdvance() {
		// 事件钩子：Pre
		if (NeoForge.EVENT_BUS.post(AnimationControllerEvent.TickPre(id, this, manager.mapper)).isCanceled) return

		tickHandlerCall()
		if (state == State.IDLE || state == State.PAUSED) {
			NeoForge.EVENT_BUS.post(AnimationControllerEvent.TickPost(id, this, manager.mapper))
			return
		}

		checkPlaybackBounds()
		tickBlend()
		if (state == State.PLAYING) {
			advanceTickCount++
			tickBackend(advanceTickCount / 20f, manager.mapper.holder, manager.mapper.molangData)
		} else {
			// TRANSITIONING / TRANSITIONING：冻结时间，用当前 animTime 重算骨骼
			tickBackend(0f, manager.mapper.holder, manager.mapper.molangData, freezeTime = true)
		}
		handleStateTransition()
		NeoForge.EVENT_BUS.post(AnimationControllerEvent.TickPost(id, this, manager.mapper))
	}

	/** 调用 tickHandler 并触发事件钩子 */
	private fun tickHandlerCall() {
		if (NeoForge.EVENT_BUS.post(AnimationControllerEvent.TickHandlerPre(id, this, manager.mapper)).isCanceled) return
		tickHandler(manager.mapper)
		NeoForge.EVENT_BUS.post(AnimationControllerEvent.TickHandlerPost(id, this, manager.mapper))
	}

	/** 处理状态转移：TRANSITIONING→PLAYING / TRANSITIONING→IDLE */
	private fun handleStateTransition() {
		if (state == State.ANIMATION_TRANSITIONING && transitionSource != null) {
			crossfadeStep()
		}
		if (state == State.ANIMATION_TRANSITIONING && blendFactor >= 1f) {
			state = State.PLAYING
			transitionSource = null
			lastRawGameTime = advanceTickCount / 20f
		}
		if (state == State.TRANSITIONING && blendFactor <= 0f) {
			forceClear()
		}
	}

	override fun tickRender(deltaSec: Float) {
		// 骨骼清理仅在 PLAYING 时执行（TRANSITIONING 中旧骨骼由 crossfadeStep 维护）
		if (state != State.PLAYING) return
		if (affectedBones.isNotEmpty()) {
			proxyModel.bones.keys.filter { it !in affectedBones }.forEach { proxyModel.bones.remove(it) }
		}
	}

	// ===== 动画后端 =====


	/** 重置动画时间到 0 */
	fun freezeAllAtFrameZero() {
		animTime = 0f
		lastRawGameTime = -1f
	}

	/** 设置动画起始时间 */
	fun setAnimStartTime(timeSec: Float) {
		animTime = timeSec
	}

	/** 获取当前播放信息 */
	fun getPlaybackInfo(): PlaybackInfo? {
		val anim = currentAnim ?: return null
		return PlaybackInfo(animTime, anim.length, anim.loopType)
	}

	/** 驱动 MoLang anim_time_update 并写骨骼到 proxyModel */
	fun tickBackend(gameTime: Float, entity: Entity, data: MolangData, freezeTime: Boolean = false) {
		val anim = currentAnim ?: return
		if (freezeTime) {
			affectedBones = anim.computeAndWrite(animTime, proxyModel, data, currentConfig.mirror)
			manager.queueEvents(this, anim.collectEvents(animTime, firedEvents, currentConfig.mirror))
			return
		}
		val scaledDelta = calcScaledDelta(gameTime)
		animTime = anim.tickAnimTime(animTime, scaledDelta, currentData, currentConfig.mirror)
		affectedBones = anim.computeAndWrite(animTime, proxyModel, data, currentConfig.mirror)

		// ActionAnimation 每 tick 钩子
		if (anim is ActionAnimation) {
			if (entity is LivingEntity) anim.onTick(entity, animTime, scaledDelta)
			anim.modifyPose(proxyModel, animTime)
		}

		manager.queueEvents(this, anim.collectEvents(animTime, firedEvents, currentConfig.mirror))
	}

	fun StaticAnimation.tickAnimTime(
		currentTime: Float,
		deltaTime: Float,
		context: MolangData? = null,
		mirrored: Boolean = false
	): Float {
		val src = getBakingAnimation(mirrored)
		val expr = src.animTimeUpdate
		if (expr != null && context != null) {
			context.updateAnimQueries(currentTime, deltaTime)
			return expr.eval(context).toFloat()
		}
		return currentTime + deltaTime
	}

	/** 计算经过 delta 时间，处理首帧 */
	private fun calcScaledDelta(gameTime: Float): Float {
		if (lastRawGameTime < 0f) {
			lastRawGameTime = gameTime
			animTime = 0f
			return 0f
		}
		val delta = gameTime - lastRawGameTime
		lastRawGameTime = gameTime
		return delta * speedMultiplier
	}

	// ===== 事件收集 =====


	// ===== 内部 =====

	/** 基于 tick 推进 blendFactor */
	private fun tickBlend() {
		if (blendFactor == blendTarget) return
		if (currentTransitionTicks <= 0) {
			blendFactor = blendTarget
			return
		}
		val step = 1f / currentTransitionTicks
		blendFactor = if (blendFactor < blendTarget) {
			(blendFactor + step).coerceAtMost(blendTarget)
		} else {
			(blendFactor - step).coerceAtLeast(blendTarget)
		}
	}

	/** 强制清除并回到 IDLE 状态 */
	private fun forceClear() {
		// ActionAnimation 结束钩子
		val actionAnim = currentAnim as? ActionAnimation
		if (actionAnim != null) {
			val livingEntity = manager.mapper.holder as? LivingEntity
			if (livingEntity != null) actionAnim.onEnd(livingEntity)
		}

		manager.clearEmittersFor(id)
		state = State.IDLE
		blendFactor = 0f; blendTarget = 0f
		proxyModel.bones.clear()
		transitionSource = null
		currentConfig = AnimationPlayData.EMPTY
		currentAnimId = null
		affectedBones = emptySet()
		currentTransitionTicks = ProxyBoneConfigData.DEFAULT_TRANSITION_TICKS
		speedMultiplier = 1f
		activeBoneConfig = ProxyBoneConfigData.EMPTY
		extraModel = null
		manager.rebuildBones()
	}

	private fun isInFadeIn(): Boolean = isFadingIn
}
