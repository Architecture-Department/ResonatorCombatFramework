// 骨骼变换工具类
package architecture.resonator_combat_framework.module.player_animation.mapper

import architecture.resonator_combat_framework.module.player_animation.api.ProxyBone
import architecture.resonator_combat_framework.module.player_animation.api.hasPos
import architecture.resonator_combat_framework.module.player_animation.api.hasRot
import architecture.resonator_combat_framework.module.player_animation.api.hasScale
import architecture.resonator_combat_framework.module.player_animation.config.ProxyBoneFlags
import architecture.resonator_combat_framework.module.player_animation.config.isEnabled
import architecture.resonator_combat_framework.module.player_animation.config.shouldTransition
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.model.geom.ModelPart
import org.joml.Quaternionf
import org.joml.Vector3f
import java.lang.Math.toRadians

/** 启用位标记 */
const val POS_X = 0x001
const val POS_Y = 0x002
const val POS_Z = 0x004
const val ROT_X = 0x008
const val ROT_Y = 0x010
const val ROT_Z = 0x020
const val SCL_X = 0x040
const val SCL_Y = 0x080
const val SCL_Z = 0x100

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
			mask = mask or POS_X
			p.x = bone.pos.x / 16f * useWeight
		}
		if (flags.isEnabled("pos.y") && np) {
			mask = mask or POS_Y
			p.y = bone.pos.y * signY / 16f * useWeight
		}
		if (flags.isEnabled("pos.z") && np) {
			mask = mask or POS_Z
			p.z = bone.pos.z / 16f * useWeight
		}
		if (flags.isEnabled("rot.x") && nr) {
			mask = mask or ROT_X
			r.x = toRadians(bone.rotation.x.toDouble()).toFloat() * useWeight
		}
		if (flags.isEnabled("rot.y") && nr) {
			mask = mask or ROT_Y
			r.y = toRadians(bone.rotation.y.toDouble()).toFloat() * useWeight
		}
		if (flags.isEnabled("rot.z") && nr) {
			mask = mask or ROT_Z
			r.z = toRadians(bone.rotation.z.toDouble()).toFloat() * useWeight
		}
		if (flags.isEnabled("scale.x") && ns) {
			mask = mask or SCL_X
			s.x = 1f + (bone.scale.x - 1f) * useWeight
		}
		if (flags.isEnabled("scale.y") && ns) {
			mask = mask or SCL_Y
			s.y = 1f + (bone.scale.y - 1f) * useWeight
		}
		if (flags.isEnabled("scale.z") && ns) {
			mask = mask or SCL_Z
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
			mask = mask or POS_X
			p.x = bone.pos.x
		}
		if (flags.isEnabled("pos.y") && np) {
			mask = mask or POS_Y
			p.y = -bone.pos.y
		}
		if (flags.isEnabled("pos.z") && np) {
			mask = mask or POS_Z
			p.z = bone.pos.z
		}
		if (flags.isEnabled("rot.x") && nr) {
			mask = mask or ROT_X
			r.x = toRadians(bone.rotation.x.toDouble()).toFloat()
		}
		if (flags.isEnabled("rot.y") && nr) {
			mask = mask or ROT_Y
			r.y = toRadians(bone.rotation.y.toDouble()).toFloat()
		}
		if (flags.isEnabled("rot.z") && nr) {
			mask = mask or ROT_Z
			r.z = toRadians(bone.rotation.z.toDouble()).toFloat()
		}
		if (flags.isEnabled("scale.x") && ns) {
			mask = mask or SCL_X
			s.x = bone.scale.x
		}
		if (flags.isEnabled("scale.y") && ns) {
			mask = mask or SCL_Y
			s.y = bone.scale.y
		}
		if (flags.isEnabled("scale.z") && ns) {
			mask = mask or SCL_Z
			s.z = bone.scale.z
		}

		return Transform(mask, p, r, s)
	}

	fun applyTo(poseStack: PoseStack, t: Transform) {
		if (t.mask and (POS_X or POS_Y or POS_Z) != 0) poseStack.translate(t.pos.x, t.pos.y, t.pos.z)
		if (t.mask and (ROT_X or ROT_Y or ROT_Z) != 0) poseStack.mulPose(
			Quaternionf().rotationZYX(
				t.rot.z,
				t.rot.y,
				t.rot.x
			)
		)
		if (t.mask and (SCL_X or SCL_Y or SCL_Z) != 0) poseStack.scale(
			if (t.mask and SCL_X != 0) t.scale.x else 1f,
			if (t.mask and SCL_Y != 0) t.scale.y else 1f,
			if (t.mask and SCL_Z != 0) t.scale.z else 1f
		)
	}

	fun applyTo(part: ModelPart, t: Transform, lockPos: Boolean, lockRot: Boolean, lockScale: Boolean, useWeight: Float) {
		val ip = part.initialPose
		if (lockPos) {
			if (t.mask and POS_X != 0) part.x += (ip.x + t.pos.x - part.x) * useWeight
			if (t.mask and POS_Y != 0) part.y += (ip.y + t.pos.y - part.y) * useWeight
			if (t.mask and POS_Z != 0) part.z += (ip.z + t.pos.z - part.z) * useWeight
		} else {
			if (t.mask and POS_X != 0) part.x += t.pos.x * useWeight
			if (t.mask and POS_Y != 0) part.y += t.pos.y * useWeight
			if (t.mask and POS_Z != 0) part.z += t.pos.z * useWeight
		}
		if (lockRot) {
			if (t.mask and ROT_X != 0) part.xRot += (ip.xRot + t.rot.x - part.xRot) * useWeight
			if (t.mask and ROT_Y != 0) part.yRot += (ip.yRot + t.rot.y - part.yRot) * useWeight
			if (t.mask and ROT_Z != 0) part.zRot += (ip.zRot + t.rot.z - part.zRot) * useWeight
		} else {
			if (t.mask and ROT_X != 0) part.xRot += t.rot.x * useWeight
			if (t.mask and ROT_Y != 0) part.yRot += t.rot.y * useWeight
			if (t.mask and ROT_Z != 0) part.zRot += t.rot.z * useWeight
		}
		if (lockScale) {
			if (t.mask and SCL_X != 0) part.xScale += (1f + (t.scale.x - 1f) - part.xScale) * useWeight
			if (t.mask and SCL_Y != 0) part.yScale += (1f + (t.scale.y - 1f) - part.yScale) * useWeight
			if (t.mask and SCL_Z != 0) part.zScale += (1f + (t.scale.z - 1f) - part.zScale) * useWeight
		} else {
			if (t.mask and SCL_X != 0) part.xScale += t.scale.x * useWeight
			if (t.mask and SCL_Y != 0) part.yScale += t.scale.y * useWeight
			if (t.mask and SCL_Z != 0) part.zScale += t.scale.z * useWeight
		}
	}

	/** 骨骼变换，18字段压缩为 4 字段：bitmask + 3×Vector3f */
	data class Transform(
		val mask: Int,
		val pos: Vector3f,
		val rot: Vector3f,
		val scale: Vector3f
	)
}