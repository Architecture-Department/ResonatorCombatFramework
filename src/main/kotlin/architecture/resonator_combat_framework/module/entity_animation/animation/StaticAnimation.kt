package architecture.resonator_combat_framework.module.entity_animation.animation

import architecture.goldenboughs_lib.api.AllOpe
import architecture.resonator_combat_framework.module.entity_animation.animation.model.ProxyModel
import architecture.resonator_combat_framework.module.entity_animation.animation.molang.MolangData
import architecture.resonator_combat_framework.module.entity_animation.registry.BedrockAnimationRegistry
import architecture.resonator_combat_framework.util.RcfUtil
import net.minecraft.resources.ResourceLocation

enum class LoopType { ONCE, LOOP, HOLD_ON_LAST }

@AllOpe
class StaticAnimation(
	val id: ResourceLocation,
	val animationId: String,
) {
	private lateinit var bakingAnimation: BakingBrAnimation
	private lateinit var mirroredBakingAnimation: BakingBrAnimation

	val length: Float get() = bakingAnimation.length
	val loopType: LoopType get() = bakingAnimation.loop

	constructor(id: ResourceLocation) :
		this(id, id.namespace + "." + id.path)

	constructor(animationId: String) :
		this(RcfUtil.modRl(animationId), animationId)

	fun init(isClient: Boolean) {
		bakingAnimation =
			BedrockAnimationRegistry.getInstance(isClient).getBakingAnimation(animationId) ?: BakingBrAnimation.EMPTY
	}

	/** 获取当前生效的 BakingBrAnimation */
	fun getBakingAnimation(mirrored: Boolean = false): BakingBrAnimation {
		if (!mirrored) {
			return bakingAnimation
		}
		if (!::mirroredBakingAnimation.isInitialized) mirroredBakingAnimation = bakingAnimation.mirrored()
		return mirroredBakingAnimation
	}

	fun computeAndWrite(
		time: Float,
		proxyModel: ProxyModel,
		context: MolangData? = null,
		mirrored: Boolean = false
	): Set<String> {
		return getBakingAnimation(mirrored).computeAndWrite(time, proxyModel, context)
	}

	fun tickAnimTime(
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

	fun collectEvents(
		time: Float,
		alreadyFired: MutableSet<String>,
		mirrored: Boolean = false
	): AnimationEventsToFire {
		val src = getBakingAnimation(mirrored)
		val sounds = mutableListOf<BakingBrAnimationSound>()
		val particles = mutableListOf<BakingBrAnimationParticle>()
		val timelines = mutableListOf<BakingBrAnimationTimeline>()

		collectTyped(src.sounds, "sound_", alreadyFired, time, sounds)
		collectTyped(src.particles, "particle_", alreadyFired, time, particles)
		collectTyped(src.timelines, "timeline_", alreadyFired, time, timelines)

		return AnimationEventsToFire(sounds, particles, timelines)
	}

	private inline fun <reified T : Any> collectTyped(
		events: List<T>,
		prefix: String,
		alreadyFired: MutableSet<String>,
		time: Float,
		out: MutableList<T>
	) {
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
}
