package architecture.resonator_combat_framework.module.entity_animation.util

import architecture.goldenboughs_lib.util.toRadians
import architecture.resonator_combat_framework.module.entity_animation.animation.data.*
import architecture.resonator_combat_framework.module.entity_animation.animation.model.ProxyBone
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.model.geom.ModelPart
import net.minecraft.client.model.geom.PartPose
import org.joml.Quaternionf
import org.joml.Vector3f

object BoneTransformUtil {

	/**
	 * 将 ProxyBone 变换数据计算为 Transform，应用轴翻转和单位缩放。
	 *
	 * @param bone 代理骨骼数据
	 * @param flags 骨骼标志（控制各轴是否启用）
	 * @param flipPX/YP/ZP 位置轴取反
	 * @param flipRX/RY/RZ 旋转轴取反
	 * @param except 缩放分母（16=世界坐标，1=模型坐标）
	 */
	fun computeFor(
		bone: ProxyBone, flags: ProxyBoneFlags?,
		flipPX: Boolean = false, flipPY: Boolean = false, flipPZ: Boolean = false,
		flipRX: Boolean = false, flipRY: Boolean = false, flipRZ: Boolean = false,
		except: Boolean = false,
	): Transform {
		val np = bone.hasPos()
		val nr = bone.hasRot()
		val ns = bone.hasScale()
		var mask = 0
		val p = Vector3f()
		val r = Vector3f()
		val s = Vector3f(1f, 1f, 1f)
		val exceptDiv = if (except) 16f else 1f

		// 逐轴检查标志并设置值
		mask = setAxisIf(mask, Transform.POS_X, np, flags.isEnabled("pos.x")) {
			p.x = bone.pos.x / exceptDiv * sign(flipPX)
		}
		mask = setAxisIf(mask, Transform.POS_Y, np, flags.isEnabled("pos.y")) {
			p.y = bone.pos.y / exceptDiv * sign(flipPY)
		}
		mask = setAxisIf(mask, Transform.POS_Z, np, flags.isEnabled("pos.z")) {
			p.z = bone.pos.z / exceptDiv * sign(flipPZ)
		}
		mask = setAxisIf(mask, Transform.ROT_X, nr, flags.isEnabled("rot.x")) {
			r.x = bone.rotation.x * sign(flipRX)
		}
		mask = setAxisIf(mask, Transform.ROT_Y, nr, flags.isEnabled("rot.y")) {
			r.y = bone.rotation.y * sign(flipRY)
		}
		mask = setAxisIf(mask, Transform.ROT_Z, nr, flags.isEnabled("rot.z")) {
			r.z = bone.rotation.z * sign(flipRZ)
		}
		mask = setAxisIf(mask, Transform.SCL_X, ns, flags.isEnabled("scale.x")) {
			s.x = bone.scale.x - 1f
		}
		mask = setAxisIf(mask, Transform.SCL_Y, ns, flags.isEnabled("scale.y")) {
			s.y = bone.scale.y - 1f
		}
		mask = setAxisIf(mask, Transform.SCL_Z, ns, flags.isEnabled("scale.z")) {
			s.z = bone.scale.z - 1f
		}

		return Transform(mask, p, r, s)
	}

	/**
	 * 将 Transform 应用到 PoseStack（用于 root/物品变换）。
	 * 使用 Quaternionf.rotationZYX 避免万向锁。
	 */
	fun applyTo(poseStack: PoseStack, t: Transform, flags: ProxyBoneFlags?, weight: Float) {
		if (!t.hasAnyPos && !t.hasAnyRot && !t.hasAnyScale) return
		val useWeight = if (flags.shouldBlend()) weight else 1f
		if (t.hasAnyPos) poseStack.translate(
			t.pos.x * useWeight,
			t.pos.y * useWeight,
			t.pos.z * useWeight
		)
		if (t.hasAnyRot) poseStack.mulPose(
			Quaternionf().rotationZYX(
				t.rot.z.toRadians() * useWeight,
				t.rot.y.toRadians() * useWeight,
				t.rot.x.toRadians() * useWeight
			)
		)
		if (t.hasAnyScale) poseStack.scale(
			1 + t.scale.x * useWeight,
			1 + t.scale.y * useWeight,
			1 + t.scale.z * useWeight
		)
	}

	/**
	 * 将 Transform 应用到 ModelPart（用于 HumanoidModel 骨骼）。
	 * 支持 lock/normal 两种模式：lock 模式以 initialPose 为基准增量。
	 */
	fun applyTo(part: ModelPart, t: Transform, flags: ProxyBoneFlags?, weight: Float) {
		val useWeight = if (flags.shouldBlend()) weight else 1f
		val lockPos = flags.lockPos()
		val lockRot = flags.lockRotation()
		val lockScale = flags.lockScale()
		val ip = part.initialPose

		if (lockPos) {
			applyLockedPos(part, t, ip, useWeight)
		} else {
			applyDirectPos(part, t, useWeight)
		}
		if (lockRot) {
			applyLockedRot(part, t, ip, useWeight)
		} else {
			applyDirectRot(part, t, useWeight)
		}
		if (lockScale) {
			applyLockedScale(part, t, ip, useWeight)
		} else {
			applyDirectScale(part, t, useWeight)
		}
	}

	// ===== 内部辅助方法 =====

	/** 布尔值转 ±1 */
	private fun sign(flip: Boolean): Float = if (flip) -1f else 1f

	/**
	 * 当条件满足时设置位掩码并执行操作。
	 * @param mask 当前位掩码
	 * @param bit 目标位
	 * @param hasData 骨骼是否有该轴数据
	 * @param enabled 标志是否启用该轴
	 * @param action 设置值的操作
	 * @return 更新后的位掩码
	 */
	private inline fun setAxisIf(mask: Int, bit: Int, hasData: Boolean, enabled: Boolean, action: () -> Unit): Int {
		return if (hasData && enabled) {
			action()
			mask or bit
		} else {
			mask
		}
	}

	/** lock 模式：以 initialPose 为基准累加到目标值 */
	private fun applyLockedPos(part: ModelPart, t: Transform, ip: PartPose, w: Float) {
		if (t.hasPosX) part.x += (ip.x + t.pos.x - part.x) * w
		if (t.hasPosY) part.y += (ip.y + t.pos.y - part.y) * w
		if (t.hasPosZ) part.z += (ip.z + t.pos.z - part.z) * w
	}

	/** normal 模式：直接累加 */
	private fun applyDirectPos(part: ModelPart, t: Transform, w: Float) {
		if (t.hasPosX) part.x += t.pos.x * w
		if (t.hasPosY) part.y += t.pos.y * w
		if (t.hasPosZ) part.z += t.pos.z * w
	}

	/** lock 模式：旋转以 initialPose 为基准 */
	private fun applyLockedRot(part: ModelPart, t: Transform, ip: PartPose, w: Float) {
		if (t.hasRotX) part.xRot += (ip.xRot + t.rot.x.toRadians() - part.xRot) * w
		if (t.hasRotY) part.yRot += (ip.yRot + t.rot.y.toRadians() - part.yRot) * w
		if (t.hasRotZ) part.zRot += (ip.zRot + t.rot.z.toRadians() - part.zRot) * w
	}

	private fun applyDirectRot(part: ModelPart, t: Transform, w: Float) {
		if (t.hasRotX) part.xRot += t.rot.x.toRadians() * w
		if (t.hasRotY) part.yRot += t.rot.y.toRadians() * w
		if (t.hasRotZ) part.zRot += t.rot.z.toRadians() * w
	}

	/** lock 模式：缩放以 1 为基准 */
	private fun applyLockedScale(part: ModelPart, t: Transform, ip: PartPose, w: Float) {
		if (t.hasScaleX) part.xScale += (1f + t.scale.x - part.xScale) * w
		if (t.hasScaleY) part.yScale += (1f + t.scale.y - part.yScale) * w
		if (t.hasScaleZ) part.zScale += (1f + t.scale.z - part.zScale) * w
	}

	private fun applyDirectScale(part: ModelPart, t: Transform, w: Float) {
		if (t.hasScaleX) part.xScale += t.scale.x * w
		if (t.hasScaleY) part.yScale += t.scale.y * w
		if (t.hasScaleZ) part.zScale += t.scale.z * w
	}

	/** 变换数据：位掩码标记哪些轴有值，避免 JOML Vec 默认值歧义 */
	data class Transform(
		private val mask: Int,
		val pos: Vector3f,
		val rot: Vector3f,
		val scale: Vector3f
	) {
		companion object {
			const val POS_X = 0x001
			const val POS_Y = 0x002
			const val POS_Z = 0x004
			const val ROT_X = 0x008
			const val ROT_Y = 0x010
			const val ROT_Z = 0x020
			const val SCL_X = 0x040
			const val SCL_Y = 0x080
			const val SCL_Z = 0x100
		}

		/** 是否有任何位置数据 */
		val hasAnyPos: Boolean get() = mask and (POS_X or POS_Y or POS_Z) != 0

		/** 是否有任何旋转数据 */
		val hasAnyRot: Boolean get() = mask and (ROT_X or ROT_Y or ROT_Z) != 0

		/** 是否有任何缩放数据 */
		val hasAnyScale: Boolean get() = mask and (SCL_X or SCL_Y or SCL_Z) != 0

		val hasPosX: Boolean get() = mask and POS_X != 0
		val hasPosY: Boolean get() = mask and POS_Y != 0
		val hasPosZ: Boolean get() = mask and POS_Z != 0
		val hasRotX: Boolean get() = mask and ROT_X != 0
		val hasRotY: Boolean get() = mask and ROT_Y != 0
		val hasRotZ: Boolean get() = mask and ROT_Z != 0
		val hasScaleX: Boolean get() = mask and SCL_X != 0
		val hasScaleY: Boolean get() = mask and SCL_Y != 0
		val hasScaleZ: Boolean get() = mask and SCL_Z != 0
	}
}
