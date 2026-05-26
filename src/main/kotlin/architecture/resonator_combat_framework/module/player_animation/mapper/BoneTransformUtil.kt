package architecture.resonator_combat_framework.module.player_animation.mapper

import architecture.resonator_combat_framework.module.player_animation.api.ProxyBone
import architecture.resonator_combat_framework.module.player_animation.config.ProxyBoneFlags
import architecture.resonator_combat_framework.module.player_animation.config.isEnabled
import architecture.resonator_combat_framework.module.player_animation.config.shouldTransition
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.model.geom.ModelPart
import org.joml.Quaternionf

object BoneTransformUtil {

	@Suppress("DuplicatedCode")
	fun computeForPoseStack(
		bone: ProxyBone, flags: ProxyBoneFlags?, weight: Float, flipXY: Boolean = false
	): Transform {
		val useWeight = if (flags.shouldTransition()) weight else 1f
		val noP = !bone.posEmpty;
		val noR = !bone.rotationEmpty;
		val noS = !bone.scalaEmpty
		val signX = if (flipXY) -1f else 1f;
		val signY = if (flipXY) -1f else 1f

		val ePX = flags.isEnabled("pos.x") && noP;
		val ePY = flags.isEnabled("pos.y") && noP;
		val ePZ = flags.isEnabled("pos.z") && noP
		val eRX = flags.isEnabled("rot.x") && noR;
		val eRY = flags.isEnabled("rot.y") && noR;
		val eRZ = flags.isEnabled("rot.z") && noR
		val eSX = flags.isEnabled("scale.x") && noS;
		val eSY = flags.isEnabled("scale.y") && noS;
		val eSZ = flags.isEnabled("scale.z") && noS

		return Transform(
			ePX,
			ePY,
			ePZ,
			eRX,
			eRY,
			eRZ,
			eSX,
			eSY,
			eSZ,
			if (ePX) bone.pos.x * signX * useWeight else 0f,
			if (ePY) bone.pos.y * signY * useWeight else 0f,
			if (ePZ) bone.pos.z * useWeight else 0f,
			if (eRX) bone.rotation.x * signX * useWeight else 0f,
			if (eRY) bone.rotation.y * signY * useWeight else 0f,
			if (eRZ) bone.rotation.z * useWeight else 0f,
			if (eSX) 1f + (bone.scale.x - 1f) * useWeight else 1f,
			if (eSY) 1f + (bone.scale.y - 1f) * useWeight else 1f,
			if (eSZ) 1f + (bone.scale.z - 1f) * useWeight else 1f
		)
	}

	@Suppress("DuplicatedCode")
	fun computeForModelPart(
		bone: ProxyBone, flags: ProxyBoneFlags?, useWeight: Float
	): Transform {
		val noP = !bone.posEmpty;
		val noR = !bone.rotationEmpty;
		val noS = !bone.scalaEmpty

		val ePX = flags.isEnabled("pos.x") && noP;
		val ePY = flags.isEnabled("pos.y") && noP;
		val ePZ = flags.isEnabled("pos.z") && noP
		val eRX = flags.isEnabled("rot.x") && noR;
		val eRY = flags.isEnabled("rot.y") && noR;
		val eRZ = flags.isEnabled("rot.z") && noR
		val eSX = flags.isEnabled("scale.x") && noS;
		val eSY = flags.isEnabled("scale.y") && noS;
		val eSZ = flags.isEnabled("scale.z") && noS

		return Transform(
			ePX,
			ePY,
			ePZ,
			eRX,
			eRY,
			eRZ,
			eSX,
			eSY,
			eSZ,
			-bone.pos.x * 16f,
			-bone.pos.y * 16f,
			bone.pos.z * 16f,
			-bone.rotation.x,
			-bone.rotation.y,
			bone.rotation.z,
			bone.scale.x,
			bone.scale.y,
			bone.scale.z
		)
	}

	fun applyTo(poseStack: PoseStack, t: Transform) {
		poseStack.translate(t.posX, t.posY, t.posZ)
		poseStack.mulPose(Quaternionf().rotationZYX(t.rotZ, t.rotY, t.rotX))
		poseStack.scale(t.scaleX, t.scaleY, t.scaleZ)
	}

	@Suppress("DuplicatedCode")
	fun applyTo(
		part: ModelPart, t: Transform,
		lockPos: Boolean, lockRot: Boolean, lockScale: Boolean, useWeight: Float
	) {
		val ip = part.initialPose
		if (lockPos) {
			if (t.enPosX) part.x += (ip.x + t.posX - part.x) * useWeight
			if (t.enPosY) part.y += (ip.y + t.posY - part.y) * useWeight
			if (t.enPosZ) part.z += (ip.z + t.posZ - part.z) * useWeight
		} else {
			if (t.enPosX) part.x += t.posX * useWeight
			if (t.enPosY) part.y += t.posY * useWeight
			if (t.enPosZ) part.z += t.posZ * useWeight
		}
		if (lockRot) {
			if (t.enRotX) part.xRot += (ip.xRot + t.rotX - part.xRot) * useWeight
			if (t.enRotY) part.yRot += (ip.yRot + t.rotY - part.yRot) * useWeight
			if (t.enRotZ) part.zRot += (ip.zRot + t.rotZ - part.zRot) * useWeight
		} else {
			if (t.enRotX) part.xRot += t.rotX * useWeight
			if (t.enRotY) part.yRot += t.rotY * useWeight
			if (t.enRotZ) part.zRot += t.rotZ * useWeight
		}
		if (lockScale) {
			if (t.enSclX) part.xScale += (1f + (t.scaleX - 1f) - part.xScale) * useWeight
			if (t.enSclY) part.yScale += (1f + (t.scaleY - 1f) - part.yScale) * useWeight
			if (t.enSclZ) part.zScale += (1f + (t.scaleZ - 1f) - part.zScale) * useWeight
		} else {
			if (t.enSclX) part.xScale += (t.scaleX) * useWeight
			if (t.enSclY) part.yScale += (t.scaleY) * useWeight
			if (t.enSclZ) part.zScale += (t.scaleZ) * useWeight
		}
	}

	data class Transform(
		val enPosX: Boolean,
		val enPosY: Boolean,
		val enPosZ: Boolean,
		val enRotX: Boolean,
		val enRotY: Boolean,
		val enRotZ: Boolean,
		val enSclX: Boolean,
		val enSclY: Boolean,
		val enSclZ: Boolean,
		val posX: Float,
		val posY: Float,
		val posZ: Float,
		val rotX: Float,
		val rotY: Float,
		val rotZ: Float,
		val scaleX: Float,
		val scaleY: Float,
		val scaleZ: Float
	)
}
