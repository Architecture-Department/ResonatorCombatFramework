package architecture.resonator_combat_framework.module.entity_animation.util

import architecture.resonator_combat_framework.module.entity_animation.ProxyBone
import architecture.resonator_combat_framework.module.entity_animation.data.ProxyBoneFlags
import architecture.resonator_combat_framework.module.entity_animation.data.isEnabled
import architecture.resonator_combat_framework.module.entity_animation.data.shouldTransition
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.model.geom.ModelPart
import org.joml.Quaternionf
import org.joml.Vector3f
import java.lang.Math.toRadians

object BoneTransformUtil {

	fun computeForPoseStack(
		bone: ProxyBone, flags: ProxyBoneFlags?, weight: Float, flipY: Boolean = false
	): Transform {
		val useWeight = if (flags.shouldTransition()) weight else 1f
		val np = bone.hasPos()
		val nr = bone.hasRot()
		val ns = bone.hasScale()
		var mask = 0
		val p = Vector3f()
		val r = Vector3f()
		val s = Vector3f(1f, 1f, 1f)
		val signY = if (flipY) -1f else 1f

		if (flags.isEnabled("pos.x") && np) {
			mask = Transform.addPosXMask(mask)
			p.x = bone.pos.x / 16f * useWeight
		}
		if (flags.isEnabled("pos.y") && np) {
			mask = Transform.addPosYMask(mask)
			p.y = bone.pos.y * signY / 16f * useWeight
		}
		if (flags.isEnabled("pos.z") && np) {
			mask = Transform.addPosZMask(mask)
			p.z = bone.pos.z / 16f * useWeight
		}
		if (flags.isEnabled("rot.x") && nr) {
			mask = Transform.addRotXMask(mask)
			r.x = toRadians(bone.rotation.x.toDouble()).toFloat() * useWeight
		}
		if (flags.isEnabled("rot.y") && nr) {
			mask = Transform.addRotYMask(mask)
			r.y = toRadians(bone.rotation.y.toDouble()).toFloat() * useWeight
		}
		if (flags.isEnabled("rot.z") && nr) {
			mask = Transform.addRotZMask(mask)
			r.z = toRadians(bone.rotation.z.toDouble()).toFloat() * useWeight
		}
		if (flags.isEnabled("scale.x") && ns) {
			mask = Transform.addScaleXMask(mask)
			s.x = 1f + (bone.scale.x - 1f) * useWeight
		}
		if (flags.isEnabled("scale.y") && ns) {
			mask = Transform.addScaleYMask(mask)
			s.y = 1f + (bone.scale.y - 1f) * useWeight
		}
		if (flags.isEnabled("scale.z") && ns) {
			mask = Transform.addScaleZMask(mask)
			s.z = 1f + (bone.scale.z - 1f) * useWeight
		}

		return Transform(mask, p, r, s)
	}

	fun computeForModelPart(bone: ProxyBone, flags: ProxyBoneFlags?, useWeight: Float): Transform {
		val np = bone.hasPos()
		val nr = bone.hasRot()
		val ns = bone.hasScale()
		var mask = 0
		val p = Vector3f()
		val r = Vector3f()
		val s = Vector3f(1f, 1f, 1f)

		if (flags.isEnabled("pos.x") && np) {
			mask = Transform.addPosXMask(mask)
			p.x = bone.pos.x
		}
		if (flags.isEnabled("pos.y") && np) {
			mask = Transform.addPosYMask(mask)
			p.y = -bone.pos.y
		}
		if (flags.isEnabled("pos.z") && np) {
			mask = Transform.addPosZMask(mask)
			p.z = bone.pos.z
		}
		if (flags.isEnabled("rot.x") && nr) {
			mask = Transform.addRotXMask(mask)
			r.x = toRadians(bone.rotation.x.toDouble()).toFloat()
		}
		if (flags.isEnabled("rot.y") && nr) {
			mask = Transform.addRotYMask(mask)
			r.y = toRadians(bone.rotation.y.toDouble()).toFloat()
		}
		if (flags.isEnabled("rot.z") && nr) {
			mask = Transform.addRotZMask(mask)
			r.z = toRadians(bone.rotation.z.toDouble()).toFloat()
		}
		if (flags.isEnabled("scale.x") && ns) {
			mask = Transform.addScaleXMask(mask)
			s.x = bone.scale.x
		}
		if (flags.isEnabled("scale.y") && ns) {
			mask = Transform.addScaleYMask(mask)
			s.y = bone.scale.y
		}
		if (flags.isEnabled("scale.z") && ns) {
			mask = Transform.addScaleZMask(mask)
			s.z = bone.scale.z
		}

		return Transform(mask, p, r, s)
	}

	fun applyTo(poseStack: PoseStack, t: Transform) {
		if (t.hasAnyPos) poseStack.translate(t.pos.x, t.pos.y, t.pos.z)
		if (t.hasAnyRot) poseStack.mulPose(
			Quaternionf().rotationZYX(
				t.rot.z,
				t.rot.y,
				t.rot.x
			)
		)
		if (t.hasAnyScale) poseStack.scale(
			if (t.hasScaleX) t.scale.x else 1f,
			if (t.hasScaleY) t.scale.y else 1f,
			if (t.hasScaleZ) t.scale.z else 1f
		)
	}

	fun applyTo(part: ModelPart, t: Transform, lockPos: Boolean, lockRot: Boolean, lockScale: Boolean, useWeight: Float) {
		val ip = part.initialPose
		if (lockPos) {
			if (t.hasPosX) part.x += (ip.x + t.pos.x - part.x) * useWeight
			if (t.hasPosY) part.y += (ip.y + t.pos.y - part.y) * useWeight
			if (t.hasPosZ) part.z += (ip.z + t.pos.z - part.z) * useWeight
		} else {
			if (t.hasPosX) part.x += t.pos.x * useWeight
			if (t.hasPosY) part.y += t.pos.y * useWeight
			if (t.hasPosZ) part.z += t.pos.z * useWeight
		}
		if (lockRot) {
			if (t.hasRotX) part.xRot += (ip.xRot + t.rot.x - part.xRot) * useWeight
			if (t.hasRotY) part.yRot += (ip.yRot + t.rot.y - part.yRot) * useWeight
			if (t.hasRotZ) part.zRot += (ip.zRot + t.rot.z - part.zRot) * useWeight
		} else {
			if (t.hasRotX) part.xRot += t.rot.x * useWeight
			if (t.hasRotY) part.yRot += t.rot.y * useWeight
			if (t.hasRotZ) part.zRot += t.rot.z * useWeight
		}
		if (lockScale) {
			if (t.hasScaleX) part.xScale += (1f + (t.scale.x - 1f) - part.xScale) * useWeight
			if (t.hasScaleY) part.yScale += (1f + (t.scale.y - 1f) - part.yScale) * useWeight
			if (t.hasScaleZ) part.zScale += (1f + (t.scale.z - 1f) - part.zScale) * useWeight
		} else {
			if (t.hasScaleX) part.xScale += t.scale.x * useWeight
			if (t.hasScaleY) part.yScale += t.scale.y * useWeight
			if (t.hasScaleZ) part.zScale += t.scale.z * useWeight
		}
	}

	data class Transform(
		private val mask: Int,
		val pos: Vector3f,
		val rot: Vector3f,
		val scale: Vector3f
	) {
		companion object {
			private const val POS_X = 0x001
			private const val POS_Y = 0x002
			private const val POS_Z = 0x004
			private const val ROT_X = 0x008
			private const val ROT_Y = 0x010
			private const val ROT_Z = 0x020
			private const val SCL_X = 0x040
			private const val SCL_Y = 0x080
			private const val SCL_Z = 0x100

			fun addPosXMask(mask: Int): Int = mask or POS_X
			fun addPosYMask(mask: Int): Int = mask or POS_Y
			fun addPosZMask(mask: Int): Int = mask or POS_Z
			fun addRotXMask(mask: Int): Int = mask or ROT_X
			fun addRotYMask(mask: Int): Int = mask or ROT_Y
			fun addRotZMask(mask: Int): Int = mask or ROT_Z
			fun addScaleXMask(mask: Int): Int = mask or SCL_X
			fun addScaleYMask(mask: Int): Int = mask or SCL_Y
			fun addScaleZMask(mask: Int): Int = mask or SCL_Z
		}

		// ---- 组合检查 ----
		val hasAnyPos: Boolean get() = mask and (POS_X or POS_Y or POS_Z) != 0
		val hasAnyRot: Boolean get() = mask and (ROT_X or ROT_Y or ROT_Z) != 0
		val hasAnyScale: Boolean get() = mask and (SCL_X or SCL_Y or SCL_Z) != 0

		// ---- 单轴检查 ----
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

