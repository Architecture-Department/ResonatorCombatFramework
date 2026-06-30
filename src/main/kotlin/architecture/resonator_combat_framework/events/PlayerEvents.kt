package architecture.resonator_combat_framework.events

import architecture.goldenboughs_lib.util.PoseStack
import architecture.goldenboughs_lib.util.toPos
import architecture.goldenboughs_lib.util.toRadians
import architecture.resonator_combat_framework.module.entity_animation.IProxyAnimationProvider.Companion.getMapperProvider
import architecture.resonator_combat_framework.module.entity_animation.animation.model.BrModel
import architecture.resonator_combat_framework.module.entity_animation.animation.model.ProxyModel
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
		brModel: BrModel,
		animationData: ProxyModel,
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
