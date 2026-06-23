package architecture.resonator_combat_framework.module.entity_animation.util

import architecture.goldenboughs_lib.util.toRadians
import architecture.resonator_combat_framework.module.entity_animation.animation.data.*
import architecture.resonator_combat_framework.module.entity_animation.animation.model.ProxyBone
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.model.geom.ModelPart
import org.joml.Quaternionf
import org.joml.Vector3f

object BoneTransformUtil {

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
		val signPX = if (flipPX) -1f else 1f
		val signPY = if (flipPY) -1f else 1f
		val signPZ = if (flipPZ) -1f else 1f
		val signRX = if (flipRX) -1f else 1f
		val signRY = if (flipRY) -1f else 1f
		val signRZ = if (flipRZ) -1f else 1f
		val except = if (except) 16f else 1f

		if (flags.isEnabled("pos.x") && np) {
			mask = Transform.addPosXMask(mask)
			p.x = bone.pos.x / except * signPX
		}
		if (flags.isEnabled("pos.y") && np) {
			mask = Transform.addPosYMask(mask)
			p.y = bone.pos.y / except * signPY
		}
		if (flags.isEnabled("pos.z") && np) {
			mask = Transform.addPosZMask(mask)
			p.z = bone.pos.z / except * signPZ
		}
		if (flags.isEnabled("rot.x") && nr) {
			mask = Transform.addRotXMask(mask)
			r.x = bone.rotation.x * signRX
		}
		if (flags.isEnabled("rot.y") && nr) {
			mask = Transform.addRotYMask(mask)
			r.y = bone.rotation.y * signRY
		}
		if (flags.isEnabled("rot.z") && nr) {
			mask = Transform.addRotZMask(mask)
			r.z = bone.rotation.z * signRZ
		}
		if (flags.isEnabled("scale.x") && ns) {
			mask = Transform.addScaleXMask(mask)
			s.x = bone.scale.x - 1f
		}
		if (flags.isEnabled("scale.y") && ns) {
			mask = Transform.addScaleYMask(mask)
			s.y = bone.scale.y - 1f
		}
		if (flags.isEnabled("scale.z") && ns) {
			mask = Transform.addScaleZMask(mask)
			s.z = bone.scale.z - 1f
		}

		return Transform(mask, p, r, s)
	}

	fun applyTo(poseStack: PoseStack, t: Transform, flags: ProxyBoneFlags?, weight: Float) {
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

	fun applyTo(part: ModelPart, t: Transform, flags: ProxyBoneFlags?, weight: Float) {
		val useWeight = if (flags.shouldBlend()) weight else 1f
		val lockPos = flags.lockPos()
		val lockRot = flags.lockRotation()
		val lockScale = flags.lockScale()
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
			if (t.hasRotX) part.xRot += (ip.xRot + t.rot.x.toRadians() - part.xRot) * useWeight
			if (t.hasRotY) part.yRot += (ip.yRot + t.rot.y.toRadians() - part.yRot) * useWeight
			if (t.hasRotZ) part.zRot += (ip.zRot + t.rot.z.toRadians() - part.zRot) * useWeight
		} else {
			if (t.hasRotX) part.xRot += t.rot.x.toRadians() * useWeight
			if (t.hasRotY) part.yRot += t.rot.y.toRadians() * useWeight
			if (t.hasRotZ) part.zRot += t.rot.z.toRadians() * useWeight
		}
		if (lockScale) {
			if (t.hasScaleX) part.xScale += (1f + t.scale.x - part.xScale) * useWeight
			if (t.hasScaleY) part.yScale += (1f + t.scale.y - part.yScale) * useWeight
			if (t.hasScaleZ) part.zScale += (1f + t.scale.z - part.zScale) * useWeight
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

