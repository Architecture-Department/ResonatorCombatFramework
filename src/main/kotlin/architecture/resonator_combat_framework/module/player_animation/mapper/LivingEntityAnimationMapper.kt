package architecture.resonator_combat_framework.module.player_animation.mapper

// 生物实体动画映射器

import architecture.resonator_combat_framework.module.player_animation.config.ProxyBoneFlags
import net.minecraft.client.model.EntityModel
import net.minecraft.world.entity.LivingEntity

/** + 骨骼标志解析 + 动画时间追踪 */
abstract class LivingEntityAnimationMapper<T : LivingEntity, M : EntityModel<T>>(
	livingEntity: T
) : EntityAnimationMapper<T, M>(livingEntity) {

	/** 动画已运行时间，用于 timeline 求值 */
	protected var animTimeTracker = 0f

	/** 合并所有活跃动画的骨骼标志 */
	fun resolveBoneFlags(animTime: Float): Map<String, ProxyBoneFlags> {
		val flags = mutableMapOf<String, ProxyBoneFlags>()
		for ((_, config) in boneConfigs) flags.putAll(config.resolveBoneFlags(animTime))
		return flags
	}
}
