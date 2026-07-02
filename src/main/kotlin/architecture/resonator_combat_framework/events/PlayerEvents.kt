package architecture.resonator_combat_framework.events

import architecture.goldenboughs_lib.util.PoseStack
import architecture.goldenboughs_lib.util.toPos
import architecture.goldenboughs_lib.util.toRadians
import architecture.resonator_combat_framework.init.RcfAttachmentTypes
import architecture.resonator_combat_framework.module.entity_animation.IAnimationProvider.Companion.getMapperProvider
import architecture.resonator_combat_framework.module.entity_animation.animation.model.GeometryModel
import architecture.resonator_combat_framework.module.entity_animation.animation.model.PoseData
import architecture.resonator_combat_framework.module.entity_state_machine.holder.EntityStateHolder
import architecture.resonator_combat_framework.util.RcfUtil
import com.mojang.math.Axis
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent
import net.neoforged.neoforge.event.tick.PlayerTickEvent

@EventBusSubscriber(modid = RcfUtil.ID)
object PlayerEvents {
	private var DEBUG = false

	/**
	 * 玩家装备变化事件（主手物品切换/丢弃/交换）。
	 * 若当前禁止切物品（CAN_SWITCH_ITEM = false），由动作系统处理停止。
	 */
	@SubscribeEvent
	fun onEquipmentChange(event: LivingEquipmentChangeEvent) {
		if (event.slot != EquipmentSlot.MAINHAND) return
		val player = event.entity as? Player ?: return
		val stateHolder = player.getData(RcfAttachmentTypes.STATE_HOLDER)
		if (stateHolder.getState(EntityStateHolder.CAN_SWITCH_ITEM)) return
		// 禁止切物品：强制结束动作（无过渡），动作的 onEnd 会自动停止动画
		stateHolder.actionController.onActionForcedEnd()
	}

	/**
	 * 玩家 tick 后处理：在客户端调试模式下于 locator 位置生成粒子标记。
	 */
	@SubscribeEvent
	fun onTickPre(event: PlayerTickEvent.Post) {
		val player = event.entity
		val level = player.level()
		val animationTransformer = player.getMapperProvider()
		val animationControllerManager = animationTransformer.animationControllerManager
		val animationData = animationControllerManager.getInterpolatedProxy(1f)
		val brModel = animationControllerManager.brModel
		if (level.isClientSide) {
			if (DEBUG) {
				test("right_item", brModel, animationData, player, level)
				test("left_item", brModel, animationData, player, level)
			}
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