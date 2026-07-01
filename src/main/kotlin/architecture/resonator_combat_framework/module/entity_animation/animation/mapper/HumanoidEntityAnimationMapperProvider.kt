package architecture.resonator_combat_framework.module.entity_animation.animation.mapper

// 人形实体动画映射器

import architecture.resonator_combat_framework.module.entity_animation.animation.data.ProxyBoneFlags
import architecture.resonator_combat_framework.module.entity_animation.animation.model.PoseData
import architecture.resonator_combat_framework.module.entity_animation.util.ModelPartApplier
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import net.minecraft.client.model.HumanoidModel
import net.minecraft.client.model.geom.ModelPart
import net.minecraft.world.entity.LivingEntity

/** 人形实体动画映射器 — ProxyModel→HumanoidModel 转换, 处理 6 个人形骨骼 + 物品定位器 */
abstract class HumanoidEntityAnimationMapperProvider<T : LivingEntity, M : HumanoidModel<T>>(
	livingEntity: T,
	isClient: Boolean,
	animationControllerManager: AnimationControllerManager<T>
) : LivingEntityAnimationMapperProvider<T, M>(livingEntity, isClient, animationControllerManager) {
	constructor(holder: T) : this(holder, holder.level().isClientSide, AnimationControllerManager(holder))

	override fun applyProxyToModel(
		poseData: PoseData, model: M, flags: Map<String, ProxyBoneFlags>
	) {
		if (!isClient) return
		applyProxyBone(poseData, "head", flags, model.head, model.hat)
		applyProxyBone(poseData, "body", flags, model.body)
		applyProxyBone(poseData, "left_arm", flags, model.leftArm)
		applyProxyBone(poseData, "right_arm", flags, model.rightArm)
		applyProxyBone(poseData, "left_leg", flags, model.leftLeg)
		applyProxyBone(poseData, "right_leg", flags, model.rightLeg)
	}

	/** 将单骨骼数据通过 BoneTransformUtil 计算出 Transform 并应用到 ModelPart */
	protected fun applyProxyBone(
		proxy: PoseData, name: String, flags: Map<String, ProxyBoneFlags>, vararg parts: ModelPart
	) {
		val bone = proxy.getBone(name) ?: return
		val boneFlags = flags[name]
		val t = ModelPartApplier.computeFor(bone, boneFlags, flipPY = true, except = false)
		for (part in parts) ModelPartApplier.applyTo(part, t, boneFlags, 1f)
	}

	/** 将物品定位器骨骼（left_item/right_item）变换应用到 PoseStack */
	fun applyProxyToItem(
		poseData: PoseData,
		isLeft: Boolean,
		poseStack: PoseStack,
		flags: Map<String, ProxyBoneFlags>
	) {
		val name = if (isLeft) "left_item" else "right_item"
		val bone = poseData.getBone(name) ?: return
		val boneFlags = flags[name]
		val t = ModelPartApplier.computeFor(bone, boneFlags, except = true, flipPX = true, flipRY = true, flipRX = true)
		poseStack.mulPose(Axis.XP.rotationDegrees(90.0f))
		ModelPartApplier.applyTo(poseStack, t, boneFlags, 1f)
		poseStack.mulPose(Axis.XP.rotationDegrees(-90.0f))
	}

	/** ItemInHandLayerMixin 调用：物品定位器 → PoseStack */
	fun applyItemTransform(isLeft: Boolean, poseStack: PoseStack) {
		if (!isClient) return
		applyProxyToItem(
			animationControllerManager.getInterpolatedProxy(currentPartialTick),
			isLeft, poseStack, animationControllerManager.mergedBoneFlags
		)
	}
}

