package architecture.resonator_combat_framework.module.entity_animation.animation.mapper

import architecture.resonator_combat_framework.module.entity_animation.animation.data.BoneFlags
import architecture.resonator_combat_framework.module.entity_animation.animation.model.PoseData
import architecture.resonator_combat_framework.module.entity_animation.util.ModelPartApplier
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import net.minecraft.client.model.HumanoidModel
import net.minecraft.client.model.geom.ModelPart
import net.minecraft.world.entity.LivingEntity

/**
 * 人形实体动画映射器，实现 ProxyModel→HumanoidModel 的骨骼变换转换。
 * 处理 6 个人形基础骨骼（头、身体、左右臂、左右腿）的映射，
 * 并提供物品定位器骨骼（left_item/right_item）到 PoseStack 的变换。
 *
 * @param T 生物实体类型
 * @param M HumanoidModel 子类型
 */
abstract class HumanoidEntityAnimationMapperProvider<T : LivingEntity, M : HumanoidModel<T>>(
	livingEntity: T,
	isClient: Boolean,
	animationControllerManager: AnimationControllerManager<T>
) : LivingEntityAnimationMapperProvider<T, M>(livingEntity, isClient, animationControllerManager) {
	constructor(holder: T) : this(holder, holder.level().isClientSide, AnimationControllerManager(holder))

	/**
	 * 将代理骨骼数据映射到 HumanoidModel 的 6 个根 ModelPart。
	 *
	 * @param poseData 代理骨骼姿态数据
	 * @param model 目标 HumanoidModel
	 * @param flags 骨骼标志映射
	 */
	override fun applyProxyToModel(
		poseData: PoseData, model: M, flags: Map<String, BoneFlags>
	) {
		if (!isClient) return
		applyProxyBone(poseData, "head", flags, model.head, model.hat)
		applyProxyBone(poseData, "body", flags, model.body)
		applyProxyBone(poseData, "left_arm", flags, model.leftArm)
		applyProxyBone(poseData, "right_arm", flags, model.rightArm)
		applyProxyBone(poseData, "left_leg", flags, model.leftLeg)
		applyProxyBone(poseData, "right_leg", flags, model.rightLeg)
	}

	/**
	 * 将单根骨骼数据通过 ModelPartApplier 计算出 Transform 并应用到 ModelPart。
	 *
	 * @param proxy 代理姿态数据
	 * @param name 骨骼名称
	 * @param flags 骨骼标志映射
	 * @param parts 要应用变换的 ModelPart 列表
	 */
	protected fun applyProxyBone(
		proxy: PoseData, name: String, flags: Map<String, BoneFlags>, vararg parts: ModelPart
	) {
		val bone = proxy.getBone(name) ?: return
		val boneFlags = flags[name]
		val t = ModelPartApplier.computeFor(bone, boneFlags, flipPY = true, except = false)
		for (part in parts) ModelPartApplier.applyTo(part, t, boneFlags, 1f)
	}

	/**
	 * 将物品定位器骨骼（left_item/right_item）变换应用到 PoseStack。
	 * 用于手持物品的渲染变换。
	 *
	 * @param poseData 代理骨骼姿态数据
	 * @param isLeft 是否为左手
	 * @param poseStack 姿态栈
	 * @param flags 骨骼标志映射
	 */
	fun applyProxyToItem(
		poseData: PoseData,
		isLeft: Boolean,
		poseStack: PoseStack,
		flags: Map<String, BoneFlags>
	) {
		val name = if (isLeft) "left_item" else "right_item"
		val bone = poseData.getBone(name) ?: return
		val boneFlags = flags[name]
		val t = ModelPartApplier.computeFor(bone, boneFlags, except = true, flipPX = true, flipRY = true, flipRX = true)
		poseStack.mulPose(Axis.XP.rotationDegrees(90.0f))
		ModelPartApplier.applyTo(poseStack, t, boneFlags, 1f)
		poseStack.mulPose(Axis.XP.rotationDegrees(-90.0f))
	}

	/**
	 * ItemInHandLayer 调用的物品变换入口。
	 *
	 * @param isLeft 是否为左手
	 * @param poseStack 姿态栈
	 */
	fun applyItemTransform(isLeft: Boolean, poseStack: PoseStack) {
		if (!isClient) return
		applyProxyToItem(
			animationControllerManager.getInterpolatedProxy(currentPartialTick),
			isLeft, poseStack, animationControllerManager.mergedBoneFlags
		)
	}
}
