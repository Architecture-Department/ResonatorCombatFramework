package architecture.resonator_combat_framework.module.player_animation.mapper

import architecture.resonator_combat_framework.module.player_animation.config.ProxyBoneFlags
import net.minecraft.client.model.EntityModel
import net.minecraft.world.entity.LivingEntity

/** 生物动画映射器 — 添加骨骼标志解析和动画时间追踪 */
abstract class LivingEntityAnimationMapper<T : LivingEntity, M : EntityModel<T>>(
	livingEntity: T
) : EntityAnimationMapper<T, M>(livingEntity) {

	/** 动画运行时间 (秒), 用于 timeline 解析 */
	protected var animTimeTracker = 0f

	/** 合并所有活跃动画的骨骼标志, 供 applyProxyToModel 使用 */
	fun resolveBoneFlags(animTime: Float): Map<String, ProxyBoneFlags> {
		val flags = mutableMapOf<String, ProxyBoneFlags>()
		for ((_, config) in boneConfigs) flags.putAll(config.resolveBoneFlags(animTime))
		return flags
	}
}
