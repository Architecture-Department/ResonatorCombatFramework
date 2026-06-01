package architecture.resonator_combat_framework.module.player_animation.mapper

import architecture.resonator_combat_framework.module.player_animation.api.IAnimationMapper
import architecture.resonator_combat_framework.module.player_animation.api.ProxyModel
import architecture.resonator_combat_framework.module.player_animation.controller.AnimationControllerManager
import architecture.resonator_combat_framework.module.player_animation.flags.ProxyBoneFlags
import architecture.resonator_combat_framework.module.player_animation.util.BoneTransformUtil
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.model.EntityModel
import net.minecraft.world.entity.Entity

abstract class EntityAnimationMapper<T : Entity, M : EntityModel<T>>
@JvmOverloads
constructor(
	val entity: T,
	override val isClient: Boolean = entity.level().isClientSide
) : IAnimationMapper {

	/**
	 * 控制器管理器。
	 * 主控制器由子类在 init 中添加，保证 getDefault() 可用。
	 */
	override val animationControllerManager = AnimationControllerManager()

	abstract fun applyProxyToModel(
		proxyModels: List<ProxyModel>, model: M,
		flags: Map<String, ProxyBoneFlags>, weight: Float
	)

	fun applyRootTransform(
		proxyModels: List<ProxyModel>, poseStack: PoseStack,
		flags: Map<String, ProxyBoneFlags>, weight: Float
	) {
		if (!isClient) return
		if (weight <= 0f) return
		for (proxy in proxyModels) {
			val bone = proxy.getBone("root") ?: continue
			val t = BoneTransformUtil.computeForPoseStack(bone, flags["root"], weight, flipY = true)
			BoneTransformUtil.applyTo(poseStack, t)
			return
		}
	}
}
