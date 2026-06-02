package architecture.resonator_combat_framework.module.entity_animation.mapper

// 人形实体动画映射器

import architecture.resonator_combat_framework.module.entity_animation.api.ProxyModel
import architecture.resonator_combat_framework.module.entity_animation.data.*
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
		proxyModel: ProxyModel, model: M, flags: Map<String, ProxyBoneFlags>, weight: Float
	) {
		if (!isClient) return
		applyProxyBone(proxyModel, "head", flags, weight, model.head, model.hat)
		applyProxyBone(proxyModel, "body", flags, weight, model.body)
		applyProxyBone(proxyModel, "left_arm", flags, weight, model.leftArm)
		applyProxyBone(proxyModel, "right_arm", flags, weight, model.rightArm)
		applyProxyBone(proxyModel, "left_leg", flags, weight, model.leftLeg)
		applyProxyBone(proxyModel, "right_leg", flags, weight, model.rightLeg)
	}

	protected fun applyProxyBone(
		proxy: ProxyModel, name: String, flags: Map<String, ProxyBoneFlags>, weight: Float, vararg parts: ModelPart
	) {
		val bone = proxy.getBone(name) ?: return
		val boneFlags = flags[name]
		val useWeight = if (boneFlags.shouldTransition()) weight else 1f
		val t = BoneTransformUtil.computeForModelPart(bone, boneFlags, useWeight)
		val lockPos = boneFlags.lockPos()
		val lockRot = boneFlags.lockRotation()
		val lockScale = boneFlags.lockScale()
		for (part in parts) BoneTransformUtil.applyTo(part, t, lockPos, lockRot, lockScale, useWeight)
	}

	fun applyProxyToItem(
		proxyModel: ProxyModel,
		isLeft: Boolean,
		poseStack: PoseStack,
		flags: Map<String, ProxyBoneFlags>,
		weight: Float = 1f
	) {
		if (weight <= 0f) return
		val name = if (isLeft) "left_item" else "right_item"
		val bone = proxyModel.getBone(name) ?: return
		val t = BoneTransformUtil.computeForPoseStack(bone, flags[name], weight)
		poseStack.mulPose(Axis.XP.rotationDegrees(90.0f))
		BoneTransformUtil.applyTo(poseStack, t)
		poseStack.mulPose(Axis.XP.rotationDegrees(-90.0f))
	}
}

