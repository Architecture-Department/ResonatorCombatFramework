package architecture.resonator_combat_framework.animation

import architecture.goldenboughs_lib.api.AllOpen
import architecture.resonator_combat_framework.animation.controller.IEntityAnimationController
import architecture.resonator_combat_framework.animation.keyframe_animation.KeyframeAnimation
import architecture.resonator_combat_framework.animation.keyframe_animation.ParticleEvent
import architecture.resonator_combat_framework.animation.keyframe_animation.SoundEvent
import architecture.resonator_combat_framework.animation.keyframe_animation.TimelineEvent
import architecture.resonator_combat_framework.model.DynamicGeometryModel
import architecture.resonator_combat_framework.model.PoseData
import architecture.resonator_combat_framework.molang.MolangDataHolder
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.Entity

/**
 * 动画定义——纯生命周期定义，不持有动画数据。
 *
 * 动画数据（[KeyframeAnimation]、[BoneConfig]）由注册表管理，
 * 调用时从对应端的 Registry 获取后传入方法。
 */
@AllOpen
class AnimationDef(
	val id: ResourceLocation,
	val animationId: ResourceLocation,
) {

	constructor(id: ResourceLocation) : this(id, id)

	// ===== 动画数据处理（数据由调用方传入） =====

	/**
	 * 计算指定时间点的骨骼姿态并写入 [poseData]。
	 *
	 * @param anim 关键帧动画数据
	 * @param time 当前动画时间（秒）
	 * @param poseData 输出目标姿态数据
	 * @param context MoLang 表达式上下文
	 * @return 受影响的骨骼名称集合
	 */
	fun computeAndWrite(
		anim: KeyframeAnimation,
		time: Float,
		poseData: PoseData,
		context: MolangDataHolder? = null,
	): Set<String> {
		return anim.computeAndWrite(time, poseData, context)
	}

	/**
	 * 收集当前时间片内跨越事件边界的音效、粒子和时间线事件。
	 * 支持正放和倒放两种方向的时间推进。
	 *
	 * @param anim 关键帧动画数据
	 * @param time 当前动画时间（秒）
	 * @param prevTime 上一帧动画时间（秒）
	 * @param alreadyFired 已触发事件 key 集合，防止重复触发
	 * @return 待触发的事件容器
	 */
	fun collectEvents(
		anim: KeyframeAnimation,
		time: Float,
		prevTime: Float,
		alreadyFired: MutableSet<String>,
	): EventsToFire {
		val sounds = mutableListOf<SoundEvent>()
		val particles = mutableListOf<ParticleEvent>()
		val timelines = mutableListOf<TimelineEvent>()
		collectTyped(anim.sounds, "sound_", alreadyFired, time, prevTime, sounds)
		collectTyped(anim.particles, "particle_", alreadyFired, time, prevTime, particles)
		collectTyped(anim.timelines, "timeline_", alreadyFired, time, prevTime, timelines)
		return EventsToFire(sounds, particles, timelines)
	}

	/**
	 * 推进动画时间，返回更新后的动画时间（秒）。以固定步进累加。
	 */
	fun tickAnimTime(currentTime: Float, deltaTime: Float): Float = currentTime + deltaTime

	/**
	 * 泛型事件收集——遍历事件列表，检查是否穿越了事件时间边界。
	 * 支持正放（prevTime < time）和倒放（prevTime > time）。
	 */
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

	// ===== 生命周期钩子 =====

	/** trigger 早期钩子。在骨骼数据写入前、过渡/镜像设置前调用。 */
	fun onBegin(entity: Entity, animTime: Float, f: Float, poseData: PoseData, brModel: DynamicGeometryModel) {}

	/** 动画启动钩子。在 [trigger] 全部初始化完成后调用（含第一帧、extraModel、骨骼重建之后）。 */
	fun onStart(entity: Entity, animTime: Float, f: Float, poseData: PoseData, brModel: DynamicGeometryModel) {}

	/** 每 tick 回调（合并前）。在 [tickAnimTime] 后、[remerge] 前调用。 */
	fun tick(entity: Entity, animTime: Float, deltaTime: Float, poseData: PoseData, brModel: DynamicGeometryModel) {}

	/** 动画结束回调。在控制器清理动画状态时调用。 */
	fun onEnd(entity: Entity) {}

	/** 合并后钩子。在 [remerge] 后调用。[mergedProxy] 为最终合并骨骼。 */
	fun tickAdvance(
		entity: Entity,
		animTime: Float,
		poseData: PoseData,
		brModel: DynamicGeometryModel,
		mergedProxy: PoseData,
		controller: IEntityAnimationController<*>,
	) {
	}

	override fun toString(): String {
		return "AnimationDef(" +
			"animationId=$animationId, " +
			"id=$id" +
			")"
	}
}
