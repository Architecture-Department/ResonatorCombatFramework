package architecture.resonator_combat_framework.module.entity_animation.animation.mapper

// 人形实体动画映射器

import architecture.resonator_combat_framework.module.entity_animation.animation.data.ProxyBoneFlags
import architecture.resonator_combat_framework.module.entity_animation.animation.data.lockPos
import architecture.resonator_combat_framework.module.entity_animation.animation.data.lockRotation
import architecture.resonator_combat_framework.module.entity_animation.animation.data.lockScale
import architecture.resonator_combat_framework.module.entity_animation.animation.model.ProxyModel
import architecture.resonator_combat_framework.module.entity_animation.util.BoneTransformUtil
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import net.minecraft.client.model.HumanoidModel
import net.minecraft.client.model.geom.ModelPart
import net.minecraft.world.entity.LivingEntity

/** 人形实体动画映射器 — ProxyModel→HumanoidModel 转换, 处理 6 个人形骨骼 + 物品定位器 */
abstract class HumanoidEntityAnimationMapper<T : LivingEntity, M : HumanoidModel<T>>(
	livingEntity: T
) : LivingEntityAnimationMapper<T, M>(livingEntity) {

	override fun applyProxyToModel(
		proxyModel: ProxyModel, model: M, flags: Map<String, ProxyBoneFlags>
	) {
		if (!isClient) return
		applyProxyBone(proxyModel, "head", flags, model.head, model.hat)
		applyProxyBone(proxyModel, "body", flags, model.body)
		applyProxyBone(proxyModel, "left_arm", flags, model.leftArm)
		applyProxyBone(proxyModel, "right_arm", flags, model.rightArm)
		applyProxyBone(proxyModel, "left_leg", flags, model.leftLeg)
		applyProxyBone(proxyModel, "right_leg", flags, model.rightLeg)
	}

	/** 将单骨骼数据通过 BoneTransformUtil 计算出 Transform 并应用到 ModelPart */
	protected fun applyProxyBone(
		proxy: ProxyModel, name: String, flags: Map<String, ProxyBoneFlags>, vararg parts: ModelPart
	) {
		val bone = proxy.getBone(name) ?: return
		val boneFlags = flags[name]
		val t = BoneTransformUtil.computeForModelPart(bone, boneFlags, 1f)
		val lockPos = boneFlags.lockPos()
		val lockRot = boneFlags.lockRotation()
		val lockScale = boneFlags.lockScale()
		for (part in parts) BoneTransformUtil.applyTo(part, t, lockPos, lockRot, lockScale, 1f)
	}

	/** 将物品定位器骨骼（left_item/right_item）变换应用到 PoseStack */
	fun applyProxyToItem(
		proxyModel: ProxyModel,
		isLeft: Boolean,
		poseStack: PoseStack,
		flags: Map<String, ProxyBoneFlags>
	) {
		val name = if (isLeft) "left_item" else "right_item"
		val bone = proxyModel.getBone(name) ?: return
		val t = BoneTransformUtil.computeForPoseStack(bone, flags[name], 1f)
		poseStack.mulPose(Axis.XP.rotationDegrees(90.0f))
		BoneTransformUtil.applyTo(poseStack, t)
		poseStack.mulPose(Axis.XP.rotationDegrees(-90.0f))
	}

	/** ItemInHandLayerMixin 调用：物品定位器 → PoseStack */
	fun applyItemTransform(isLeft: Boolean, poseStack: PoseStack) {
		if (!isClient) return
		applyProxyToItem(
			animationControllerManager.getInterpolatedProxy(currentPartialTick),
			isLeft, poseStack, animationControllerManager.mergedFlags
		)
	}
}

