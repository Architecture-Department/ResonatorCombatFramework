package architecture.resonator_combat_framework.module.player_animation.mapper

import architecture.resonator_combat_framework.module.player_animation.api.ProxyModel
import architecture.resonator_combat_framework.module.player_animation.config.ProxyBoneFlags
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import net.minecraft.client.model.HumanoidModel
import net.minecraft.client.model.geom.ModelPart
import net.minecraft.world.entity.LivingEntity
import org.joml.Quaternionf

/** 人形实体动画映射器 — ProxyModel→HumanoidModel 转换, 处理 6 个人形骨骼 + 物品定位器 */
abstract class HumanoidEntityAnimationMapper<T : LivingEntity, M : HumanoidModel<T>>(
	livingEntity: T
) : LivingEntityAnimationMapper<T, M>(livingEntity) {

	/** ProxyModel 骨骼 → HumanoidModel（支持 lock 模式）*/
	override fun applyProxyToModel(
		proxyModels: List<ProxyModel>, model: M, flags: Map<String, ProxyBoneFlags>, weight: Float
	) {
		if (!isClient) return
		for (proxyModel in proxyModels) {
			applyProxyBone(proxyModel, "head", flags, weight, model.head, model.hat)
			applyProxyBone(proxyModel, "body", flags, weight, model.body)
			applyProxyBone(proxyModel, "left_arm", flags, weight, model.leftArm)
			applyProxyBone(proxyModel, "right_arm", flags, weight, model.rightArm)
			applyProxyBone(proxyModel, "left_leg", flags, weight, model.leftLeg)
			applyProxyBone(proxyModel, "right_leg", flags, weight, model.rightLeg)
		}
	}

	/** 单个骨骼 ProxyBone → ModelPart:
	 *  lock 模式: 替换原版 (lerp to eyelib)
	 *  非 lock: 叠加原版 (add eyelib delta) */
	protected fun applyProxyBone(
		proxy: ProxyModel, name: String, flags: Map<String, ProxyBoneFlags>, weight: Float, vararg parts: ModelPart
	) {
		val bone = proxy.getBone(name) ?: return
		val rp = bone.pos
		val rr = bone.rotation
		val rs = bone.scale
		val boneFlags = flags[name]
		val lock = boneFlags?.hasAnyLockState() ?: true // TODO 后续改成通用常量
		// 不参与过渡时 weight 强制为 1.0，骨骼直接跟随动画
		val useWeight = if (boneFlags?.shouldTransition() != false) weight else 1f

		val isPos = rp.x != 0f || rp.y != 0f || rp.z != 0f
		val isRotation = rr.x != 0f || rr.y != 0f || rr.z != 0f
		val isScale = rs.x != 1f || rs.y != 1f || rs.z != 1f
		if (!isPos && !isRotation && !isScale) return

		val posX = -rp.x * 16f
		val posY = -rp.y * 16f
		val posZ = rp.z * 16f
		val rotationX = -rr.x
		val rotationY = -rr.y
		val rotationZ = rr.z
		val scaleX = rs.x
		val scaleY = rs.y
		val scaleZ = rs.z

		for (part in parts) {
			val ip = part.initialPose
			if (lock) {
				if (isPos) {
					part.x += (ip.x + posX - part.x) * weight
					part.y += (ip.y + posY - part.y) * weight
					part.z += (ip.z + posZ - part.z) * weight
				}
				if (isRotation) {
					part.xRot += (ip.xRot + rotationX - part.xRot) * weight
					part.yRot += (ip.yRot + rotationY - part.yRot) * weight
					part.zRot += (ip.zRot + rotationZ - part.zRot) * weight
				}
				if (isScale) {
					part.xScale += (1 + scaleX - part.xScale) * weight
					part.yScale += (1 + scaleY - part.yScale) * weight
					part.zScale += (1 + scaleZ - part.zScale) * weight
				}
			} else {
				if (isPos) {
					part.x += posX * useWeight
					part.y += posY * useWeight
					part.z += posZ * useWeight
				}
				if (isRotation) {
					part.xRot += rotationX * useWeight
					part.yRot += rotationY * useWeight
					part.zRot += rotationZ * useWeight
				}
				if (isScale) {
					part.xScale += scaleX * useWeight
					part.yScale += scaleY * useWeight
					part.zScale += scaleZ * useWeight
				}
			}
		}
	}

	/**
	 * 将 ProxyLocator 应用到 PoseStack, 用于物品渲染.
	 * @param weight blendFactor, 用于过渡淡入淡出 (0=原版, 1=完全动画)
	 */
	@Suppress("DuplicatedCode")
	fun applyProxyToItem(proxyModels: List<ProxyModel>, isLeft: Boolean, poseStack: PoseStack, weight: Float = 1f) {
		if (weight <= 0f) return
		val itemName = if (isLeft) "left_item" else "right_item"
		for (proxy in proxyModels) {
			val bone = proxy.getBone(itemName) ?: continue
			val px = bone.pos.x * weight;
			val py = bone.pos.y * weight;
			val pz = bone.pos.z * weight
			val rx = bone.rotation.x * weight;
			val ry = bone.rotation.y * weight;
			val rz = bone.rotation.z * weight
			val sx = 1f + (bone.scale.x - 1f) * weight
			val sy = 1f + (bone.scale.y - 1f) * weight
			val sz = 1f + (bone.scale.z - 1f) * weight
			poseStack.mulPose(Axis.XP.rotationDegrees(90.0f))
			if (px != 0f || py != 0f || pz != 0f) poseStack.translate(px.toDouble(), py.toDouble(), pz.toDouble())
			if (rz != 0f || ry != 0f || rx != 0f) poseStack.mulPose(Quaternionf().rotationZYX(rz, ry, rx))
			if (sx != 1f || sy != 1f || sz != 1f) poseStack.scale(sx, sy, sz)
			poseStack.mulPose(Axis.XP.rotationDegrees(-90.0f))
			return
		}
	}
}
