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

	/** 将 ProxyModel 骨骼数据应用到 HumanoidModel, 含 lock 模式和权重 */
	override fun applyProxyToModel(
		proxyModels: List<ProxyModel>, model: M, flags: Map<String, ProxyBoneFlags>, weight: Float
	) {
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
		val rp = bone.pos;
		val rr = bone.rotation
		val rs = bone.scale
		val lock = flags[name]?.hasAnyLockState() ?: false

		val isPos = rp.x != 0f || rp.y != 0f || rp.z != 0f
		val isRotation = rr.x != 0f || rr.y != 0f || rr.z != 0f
		val isScale = rs.x != 1f || rs.y != 1f || rs.z != 1f
		if (!isPos && !isRotation && !isScale) return

		for (part in parts) {
			val ip = part.initialPose
			if (lock) {
				if (isPos) {
					part.x += (ip.x - rp.x * 16f - part.x) * weight
					part.y += (ip.y - rp.y * 16f - part.y) * weight
					part.z += (ip.z + rp.z * 16f - part.z) * weight
				}
				if (isRotation) {
					part.xRot += (ip.xRot - rr.x - part.xRot) * weight
					part.yRot += (ip.yRot - rr.y - part.yRot) * weight
					part.zRot += (ip.zRot + rr.z - part.zRot) * weight
				}
				if (isScale) {
					part.xScale += (1 + rs.x - part.xScale) * weight
					part.yScale += (1 + rs.y - part.yScale) * weight
					part.zScale += (1 + rs.z - part.zScale) * weight
				}
			} else {
				if (isPos) {
					part.x += (-rp.x * 16f) * weight
					part.y += (-rp.y * 16f) * weight
					part.z += (+rp.z * 16f) * weight
				}
				if (isRotation) {
					part.xRot += (-rr.x) * weight
					part.yRot += (-rr.y) * weight
					part.zRot += (+rr.z) * weight
				}
				if (isScale) {
					part.xScale += (rs.x) * weight
					part.yScale += (rs.y) * weight
					part.zScale += (rs.z) * weight
				}
			}
		}
	}

	/** 将 ProxyLocator 数据应用到 PoseStack, 用于物品渲染 */
	fun applyProxyToItem(proxyModels: List<ProxyModel>, isLeft: Boolean, poseStack: PoseStack) {
		val armName = if (isLeft) "left_arm" else "right_arm"
		val itemName = if (isLeft) "left_item" else "right_item"
		for (proxy in proxyModels) {
			val bone = proxy.getBone(armName) ?: continue
			val loc = bone.getLocator(itemName) ?: continue
			val px = loc.pos.x;
			val py = loc.pos.y;
			val pz = loc.pos.z
			val rx = loc.rotation.x;
			val ry = loc.rotation.y;
			val rz = loc.rotation.z
			val sx = loc.scale.x;
			val sy = loc.scale.y;
			val sz = loc.scale.z
			poseStack.mulPose(Axis.XP.rotationDegrees(90.0f))
			if (px != 0f || py != 0f || pz != 0f) poseStack.translate(px.toDouble(), py.toDouble(), pz.toDouble())
			if (rz != 0f || ry != 0f || rx != 0f) poseStack.mulPose(Quaternionf().rotationZYX(rz, ry, rx))
			if (sx != 1f || sy != 1f || sz != 1f) poseStack.scale(sx, sy, sz)
			poseStack.mulPose(Axis.XP.rotationDegrees(-90.0f))
			return
		}
	}
}
