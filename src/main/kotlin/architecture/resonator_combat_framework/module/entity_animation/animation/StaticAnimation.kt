package architecture.resonator_combat_framework.module.entity_animation.animation

import architecture.goldenboughs_lib.api.AllOpe
import architecture.resonator_combat_framework.module.entity_animation.animation.baking_animation.BakingBrAnimation
import architecture.resonator_combat_framework.module.entity_animation.animation.baking_animation.BakingBrAnimationParticle
import architecture.resonator_combat_framework.module.entity_animation.animation.baking_animation.BakingBrAnimationSound
import architecture.resonator_combat_framework.module.entity_animation.animation.baking_animation.BakingBrAnimationTimeline
import architecture.resonator_combat_framework.module.entity_animation.animation.data.ProxyBoneConfigData
import architecture.resonator_combat_framework.module.entity_animation.animation.model.BrModel
import architecture.resonator_combat_framework.module.entity_animation.animation.model.ProxyModel
import architecture.resonator_combat_framework.module.entity_animation.animation.molang.MolangData
import architecture.resonator_combat_framework.module.entity_animation.registry.BedrockAnimationRegistry
import architecture.resonator_combat_framework.module.entity_animation.registry.ProxyBoneConfigDataRegistry
import architecture.resonator_combat_framework.util.RcfUtil
import architecture.resonator_combat_framework.module.entity_animation.animation.controller.IEntityAnimationController
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.Entity

@AllOpe
class StaticAnimation(
	val id: ResourceLocation,
	val animationId: String,
) {
	private lateinit var bakingAnimation: BakingBrAnimation
	private lateinit var mirroredBakingAnimation: BakingBrAnimation

	val length: Float get() = bakingAnimation.length
	val loopType: LoopType get() = bakingAnimation.loop

	private val properties = mutableMapOf<AnimationPropertyKey<*>, Any>()
	private val timedEvents = mutableListOf<TimedEvent>()

	private var _boneConfig: ProxyBoneConfigData? = null

	val boneConfig: ProxyBoneConfigData
		get() = _boneConfig ?: ProxyBoneConfigData.EMPTY

	private var _mirroredBoneConfig: ProxyBoneConfigData? = null

	val mirroredBoneConfig: ProxyBoneConfigData
		get() {
			val base = _boneConfig ?: return ProxyBoneConfigData.EMPTY
			if (_mirroredBoneConfig == null) {
				_mirroredBoneConfig = base.mirrored()
			}
			return _mirroredBoneConfig!!
		}

	constructor(id: ResourceLocation) :
		this(id, id.namespace + "." + id.path)

	constructor(animationId: String) :
		this(RcfUtil.modRl(animationId), animationId)

	fun init(isClient: Boolean) {
		bakingAnimation = BedrockAnimationRegistry.getInstance(isClient).getBakingAnimation(animationId) ?: BakingBrAnimation.EMPTY
		_boneConfig = ProxyBoneConfigDataRegistry.getInstance(isClient).getConfig(animationId)
	}

	fun getBakingAnimation(mirrored: Boolean = false): BakingBrAnimation {
		if (!mirrored) return bakingAnimation
		if (!::mirroredBakingAnimation.isInitialized) mirroredBakingAnimation = bakingAnimation.mirrored()
		return mirroredBakingAnimation
	}

	fun computeAndWrite(time: Float, proxyModel: ProxyModel, context: MolangData? = null, mirrored: Boolean = false): Set<String> {
		return getBakingAnimation(mirrored).computeAndWrite(time, proxyModel, context)
	}

	fun collectEvents(time: Float, alreadyFired: MutableSet<String>, mirrored: Boolean = false): AnimationEventsToFire {
		val src = getBakingAnimation(mirrored)
		val sounds = mutableListOf<BakingBrAnimationSound>()
		val particles = mutableListOf<BakingBrAnimationParticle>()
		val timelines = mutableListOf<BakingBrAnimationTimeline>()
		collectTyped(src.sounds, "sound_", alreadyFired, time, sounds)
		collectTyped(src.particles, "particle_", alreadyFired, time, particles)
		collectTyped(src.timelines, "timeline_", alreadyFired, time, timelines)
		return AnimationEventsToFire(sounds, particles, timelines)
	}

	final fun tickAnimTime(currentTime: Float, deltaTime: Float, context: MolangData? = null, mirrored: Boolean = false): Float {
		val src = getBakingAnimation(mirrored)
		val expr = src.animTimeUpdate
		if (expr != null && context != null) {
			context.updateAnimQueries(currentTime, deltaTime)
			return expr.eval(context).toFloat()
		}
		return currentTime + deltaTime
	}

	private inline fun <reified T : Any> collectTyped(events: List<T>, prefix: String, alreadyFired: MutableSet<String>, time: Float, out: MutableList<T>) {
		events.forEachIndexed { i, event ->
			val key = "$prefix$i"
			if (key in alreadyFired) return@forEachIndexed
			val eventTime = when (event) {
				is BakingBrAnimationSound -> event.time
				is BakingBrAnimationParticle -> event.time
				is BakingBrAnimationTimeline -> event.time
				else -> return@forEachIndexed
			}
			if (time >= eventTime) {
				alreadyFired.add(key)
				out.add(event)
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
	fun tickAdvance(entity: Entity, animTime: Float, proxyModel: ProxyModel, brModel: BrModel, mergedProxy: ProxyModel, controller: IEntityAnimationController<*>? = null) {}
}
