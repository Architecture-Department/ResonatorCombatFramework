package architecture.resonator_combat_framework.events.entity

import architecture.goldenboughs_lib.event.PlayerDropItemEvent
import architecture.goldenboughs_lib.util.PoseStack
import architecture.goldenboughs_lib.util.toPos
import architecture.goldenboughs_lib.util.toRadians
import architecture.resonator_combat_framework.init.RcfAttachmentTypes
import architecture.resonator_combat_framework.module.entity_animation.IAnimationProvider.Companion.getMapperProvider
import architecture.resonator_combat_framework.module.entity_animation.animation.model.GeometryModel
import architecture.resonator_combat_framework.module.entity_animation.animation.model.PoseData
import architecture.resonator_combat_framework.util.RcfUtil
import com.mojang.math.Axis
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.tick.PlayerTickEvent

@EventBusSubscriber(modid = RcfUtil.ID)
object PlayerEvents {
	private var DEBUG = false

	@SubscribeEvent
	fun onDropItem(event: PlayerDropItemEvent) {
		val livingEntity = event.player
		val interactionHand = event.hand

		val stateHolderOptional = livingEntity.getExistingData(RcfAttachmentTypes.STATE_HOLDER)
		if (stateHolderOptional.isPresent) {
			val stateHolder = stateHolderOptional.get()
//			if (!stateHolder.getState(EntityStateHolder.CAN_SWITCH_ITEM)) {
//				if (interactionHand != InteractionHand.MAIN_HAND && interactionHand != InteractionHand.OFF_HAND) {
//					event.isCanceled = true
//				}
//				return
//			}
		}
	}

	/**
	 * 客户端 tick：根据状态限制移动和视角，并在调试模式下于 locator 位置生成粒子标记。
	 */
	@SubscribeEvent
	fun onTickPre(event: PlayerTickEvent.Post) {
		val player = event.entity
		val level = player.level()
		if (!level.isClientSide) return

		// 调试粒子
		if (DEBUG) {
			val animationTransformer = player.getMapperProvider()
			val animationControllerManager = animationTransformer.animationControllerManager
			val animationData = animationControllerManager.getInterpolatedProxy(1f)
			val brModel = animationControllerManager.brModel
			test("right_item", brModel, animationData, player, level)
			test("left_item", brModel, animationData, player, level)
		}
	}

	private fun test(
		name: String,
		brModel: GeometryModel,
		animationData: PoseData,
		player: Player,
		level: Level
	) {
		val pos1 = player.position().toVector3f()
		val poseStack = PoseStack()
		val locatorMatrix = brModel.computeLocatorGlobalMatrix(name, animationData, isWorld = true)
		poseStack.pushPose()
		poseStack.translate(pos1.x, pos1.y, pos1.z)
		poseStack.mulPose(Axis.YP.rotation(-player.getPreciseBodyRotation(1.0f).toRadians()))
		poseStack.pushPose()
		poseStack.mulPose(locatorMatrix)

		for (x in 0..3) {
			poseStack.pushPose()
			poseStack.translate(x.toFloat() / 16, 0f, 0f)
			val pos = poseStack.last().pose.toPos()
			level.addParticle(ParticleTypes.ELECTRIC_SPARK, pos.x, pos.y, pos.z, 0.0, 0.0, 0.0)
			poseStack.popPose()
		}
		for (y in 0..5) {
			poseStack.pushPose()
			poseStack.translate(0f, y.toFloat() / 16, 0f)
			val pos = poseStack.last().pose.toPos()
			level.addParticle(ParticleTypes.ELECTRIC_SPARK, pos.x, pos.y, pos.z, 0.0, 0.0, 0.0)
			poseStack.popPose()
		}
		for (z in 0..10) {
			poseStack.pushPose()
			poseStack.translate(0f, 0f, z.toFloat() / 16)
			val pos = poseStack.last().pose.toPos()
			level.addParticle(ParticleTypes.ELECTRIC_SPARK, pos.x, pos.y, pos.z, 0.0, 0.0, 0.0)
			poseStack.popPose()
		}
		poseStack.popPose()
		poseStack.popPose()
	}
}
