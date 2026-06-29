package architecture.resonator_combat_framework.module.entity_animation.animation

import architecture.goldenboughs_lib.api.AllOpe
import architecture.resonator_combat_framework.module.entity_animation.animation.baking_animation.BakingBrAnimation
import architecture.resonator_combat_framework.module.entity_animation.animation.baking_animation.BakingBrAnimationParticle
import architecture.resonator_combat_framework.module.entity_animation.animation.baking_animation.BakingBrAnimationSound
import architecture.resonator_combat_framework.module.entity_animation.animation.baking_animation.BakingBrAnimationTimeline
import architecture.resonator_combat_framework.module.entity_animation.animation.controller.IEntityAnimationController
import architecture.resonator_combat_framework.module.entity_animation.animation.model.BrModel
import architecture.resonator_combat_framework.module.entity_animation.animation.model.ProxyModel
import architecture.resonator_combat_framework.module.entity_animation.animation.molang.MolangData
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.Entity

/**
 * 动画定义——纯生命周期定义，不持有动画数据。
 *
 * 动画数据（[BakingBrAnimation]、[ProxyBoneConfigData]）由 [BedrockAnimationRegistry] 管理，
 * 调用时从对应端的 Registry 获取后传入方法。
 */
@AllOpe
class StaticAnimation(
	val id: ResourceLocation,
	val animationId: ResourceLocation,
) {
	private val properties = mutableMapOf<AnimationPropertyKey<*>, Any>()
	private val timedEvents = mutableListOf<TimedEvent>()

	constructor(id: ResourceLocation) : this(id, id)

	// ===== 动画数据处理（数据由调用方传入） =====

	fun computeAndWrite(
		anim: BakingBrAnimation,
		time: Float,
		proxyModel: ProxyModel,
		context: MolangData? = null,
	): Set<String> {
		return anim.computeAndWrite(time, proxyModel, context)
	}

	fun collectEvents(
		anim: BakingBrAnimation,
		time: Float,
		prevTime: Float,
		alreadyFired: MutableSet<String>,
	): AnimationEventsToFire {
		val sounds = mutableListOf<BakingBrAnimationSound>()
		val particles = mutableListOf<BakingBrAnimationParticle>()
		val timelines = mutableListOf<BakingBrAnimationTimeline>()
		collectTyped(anim.sounds, "sound_", alreadyFired, time, prevTime, sounds)
		collectTyped(anim.particles, "particle_", alreadyFired, time, prevTime, particles)
		collectTyped(anim.timelines, "timeline_", alreadyFired, time, prevTime, timelines)
		return AnimationEventsToFire(sounds, particles, timelines)
	}

	/**
	 * 推进动画时间，返回更新后的动画时间（秒）。以固定步进累加。
	 */
	fun tickAnimTime(currentTime: Float, deltaTime: Float): Float = currentTime + deltaTime

	private inline fun <reified T : Any> collectTyped(
		events: List<T>,
		prefix: String,
		alreadyFired: MutableSet<String>,
		time: Float,
		prevTime: Float,
		out: MutableList<T>,
	) {
		events.forEachIndexed { i, event ->
			val key = "$prefix$i"
			val eventTime = when (event) {
				is BakingBrAnimationSound -> event.time
				is BakingBrAnimationParticle -> event.time
				is BakingBrAnimationTimeline -> event.time
				else -> return@forEachIndexed
			}
			// 穿越事件时间边界时触发（支持正放和倒放）
			if ((prevTime < eventTime && time >= eventTime) || (eventTime in time..<prevTime)) {
				if (key !in alreadyFired) {
					alreadyFired.add(key)
					out.add(event)
				}
			}
		}
	}

	// ===== 链式属性配置 =====

	fun <T> addProperty(key: AnimationPropertyKey<T>, value: T): StaticAnimation {
		properties[key] = value as Any
		return this
	}

	@Suppress("UNCHECKED_CAST")
	fun <T> getProperty(key: AnimationPropertyKey<T>): T = (properties[key] as? T) ?: key.default

	fun addEvent(event: TimedEvent): StaticAnimation {
		timedEvents.add(event)
		return this
	}

	fun getTimedEvents(): List<TimedEvent> = timedEvents

	// ===== 生命周期钩子 =====

	fun onBegin(entity: Entity) {}

	/** 每 tick 回调（合并前）。在 [tickAnimTime] 后、[remerge] 前调用。 */
	fun tick(entity: Entity, animTime: Float, deltaTime: Float, proxyModel: ProxyModel, brModel: BrModel) {}

	fun onEnd(entity: Entity) {}

	/** 合并后钩子。在 [remerge] 后调用。[mergedProxy] 为最终合并骨骼。 */
	fun tickAdvance(
		entity: Entity,
		animTime: Float,
		proxyModel: ProxyModel,
		brModel: BrModel,
		mergedProxy: ProxyModel,
		controller: IEntityAnimationController<*>,
	) {
	}
}
