package architecture.resonator_combat_framework.module.entity_animation.animation

import architecture.goldenboughs_lib.api.AllOpe
import architecture.resonator_combat_framework.animation.AttackPhase
import architecture.resonator_combat_framework.module.entity_animation.animation.baking_animation.KeyframeAnimation
import architecture.resonator_combat_framework.module.entity_animation.animation.baking_animation.ParticleEvent
import architecture.resonator_combat_framework.module.entity_animation.animation.baking_animation.SoundEvent
import architecture.resonator_combat_framework.module.entity_animation.animation.baking_animation.TimelineEvent
import architecture.resonator_combat_framework.module.entity_animation.animation.controller.IEntityAnimationController
import architecture.resonator_combat_framework.module.entity_animation.animation.model.GeometryModel
import architecture.resonator_combat_framework.module.entity_animation.animation.model.PoseData
import architecture.resonator_combat_framework.module.entity_animation.animation.molang.MolangData
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.Entity
import java.util.*

/**
 * 动画定义——纯生命周期定义，不持有动画数据。
 *
 * 动画数据（[KeyframeAnimation]、[ProxyBoneConfigData]）由 [BedrockAnimationRegistry] 管理，
 * 调用时从对应端的 Registry 获取后传入方法。
 */
@AllOpe
class AnimationDef(
	val id: ResourceLocation,
	val animationId: ResourceLocation,
) {
	private val properties = mutableMapOf<AnimationProperty<*>, Any>()
	private val timedEvents = mutableListOf<TimedEvent>() // TODO

	constructor(id: ResourceLocation) : this(id, id)

	// ===== 动画数据处理（数据由调用方传入） =====

	fun computeAndWrite(
		anim: KeyframeAnimation,
		time: Float,
		poseData: PoseData,
		context: MolangData? = null,
	): Set<String> {
		return anim.computeAndWrite(time, poseData, context)
	}

	fun collectEvents(
		anim: KeyframeAnimation,
		time: Float,
		prevTime: Float,
		alreadyFired: MutableSet<String>,
	): AnimationEventsToFire {
		val sounds = mutableListOf<SoundEvent>()
		val particles = mutableListOf<ParticleEvent>()
		val timelines = mutableListOf<TimelineEvent>()
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
				is SoundEvent -> event.time
				is ParticleEvent -> event.time
				is TimelineEvent -> event.time
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

	@Suppress("UNCHECKED_CAST")
	fun <T : Any> addProperty(key: AnimationProperty<T>, value: T): AnimationDef {
		properties[key] = value as Any
		return this
	}

	@Suppress("UNCHECKED_CAST")
	fun <T : Any> getProperty(key: AnimationProperty<T>): Optional<T> =
		Optional.ofNullable((properties[key] as? T))

	fun <T : Any> getProperty(key: AnimationProperty<T>, phase: AttackPhase): Optional<T> {
		val property = phase.getProperty(key)
		return if (property.isPresent) property else getProperty(key)
	}

	fun addEvent(event: TimedEvent): AnimationDef {
		timedEvents.add(event)
		return this
	}

	fun getTimedEvents(): List<TimedEvent> = timedEvents

	// ===== 生命周期钩子 =====

	/** trigger 早期钩子。在骨骼数据写入前、过渡/镜像设置前调用。 */
	fun onBegin(entity: Entity, animTime: Float, f: Float, poseData: PoseData, brModel: GeometryModel) {}

	/** 动画启动钩子。在 [trigger] 全部初始化完成后调用（含第一帧、extraModel、骨骼重建之后）。 */
	fun onStart(entity: Entity, animTime: Float, f: Float, poseData: PoseData, brModel: GeometryModel) {}

	/** 每 tick 回调（合并前）。在 [tickAnimTime] 后、[remerge] 前调用。 */
	fun tick(entity: Entity, animTime: Float, deltaTime: Float, poseData: PoseData, brModel: GeometryModel) {}

	fun onEnd(entity: Entity) {}

	/** 合并后钩子。在 [remerge] 后调用。[mergedProxy] 为最终合并骨骼。 */
	fun tickAdvance(
		entity: Entity,
		animTime: Float,
		poseData: PoseData,
		brModel: GeometryModel,
		mergedProxy: PoseData,
		controller: IEntityAnimationController<*>,
	) {
	}

	override fun toString(): String {
		return "AnimationDef(" +
			"animationId=$animationId, " +
			"id=$id, " +
			"properties=$properties, " +
			"timedEvents=$timedEvents)"
	}
}
