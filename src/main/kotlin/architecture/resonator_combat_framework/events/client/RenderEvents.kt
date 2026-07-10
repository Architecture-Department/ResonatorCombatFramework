package architecture.resonator_combat_framework.events.client

import architecture.goldenboughs_lib.util.client.translate
import architecture.goldenboughs_lib.util.toRadians
import architecture.resonator_combat_framework.combat.AttackAnimationAction
import architecture.resonator_combat_framework.init.RcfAttachmentTypes
import architecture.resonator_combat_framework.module.animation.IAnimationProvider
import architecture.resonator_combat_framework.module.animation.IAnimationProvider.Companion.getMapperProvider
import architecture.resonator_combat_framework.module.animation.mapper.AnimationControllerManager
import architecture.resonator_combat_framework.util.RcfUtil
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import com.mojang.math.Axis
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.LevelRenderer
import net.minecraft.client.renderer.RenderType
import net.minecraft.world.entity.Entity
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.RenderLivingEvent
import org.joml.Matrix4f
import org.joml.Vector3fc

/**
 * 客户端渲染事件 —— 在 F3+B 调试模式下渲染攻击碰撞体的 sweep 路径。
 */
@EventBusSubscriber(modid = RcfUtil.ID, value = [Dist.CLIENT])
object RenderEvents {

	@SubscribeEvent
	fun onRenderLivingPost(event: RenderLivingEvent.Post<*, *>) {
		// 仅 F3+B（显示碰撞箱）时渲染
		if (!Minecraft.getInstance().entityRenderDispatcher.shouldRenderHitBoxes()) return

		val bufferSource = event.multiBufferSource
		val vertexConsumer = bufferSource.getBuffer(RenderType.lines())
		val poseStack = event.poseStack
		val entity = event.entity
		if (entity.isSpectator && !entity.isAlive) return

		val stateHolder = entity.getExistingDataOrNull(RcfAttachmentTypes.STATE_HOLDER) ?: return
		if (entity !is IAnimationProvider) return
		val actionController = stateHolder.actionController
		val action = actionController.action as? AttackAnimationAction ?: return
		val mapper = entity.getMapperProvider()
		val controller = mapper.getController(action.controllerId) ?: return
		val time = actionController.time
		val manager = controller.manager

		// 碰撞体（有 bodyYaw 旋转）
		poseStack.pushPose()
		poseStack.mulPose(Axis.YP.rotation(-entity.getPreciseBodyRotation(1.0f).toRadians()))
		renderColliders(
			manager,
			action, time, poseStack, vertexConsumer
		)
		poseStack.popPose()
	}

	/**
	 * 渲染指定阶段所有碰撞体的 sweep 插值路径。
	 * 当前碰撞体以亮白色显示，sweep 插值样本以渐变绿色显示。
	 */
	private fun renderColliders(
		manager: AnimationControllerManager<out Entity>,
		action: AttackAnimationAction,
		time: Float,
		poseStack: PoseStack,
		vertexConsumer: VertexConsumer,
	) {
		val brModel = manager.brModel
		val (bones, _) = brModel
		for (phase in action.phases) {
			if (time < phase.startTime || time > phase.endTime) continue

			for (pair in phase.colliders) {
				if (bones[pair.boneName] == null) continue
				val c2 = pair.collider
				val half2 = c2.modelVertices[1]
				val hx = half2.x().toDouble()
				val hy = half2.y().toDouble()
				val hz = half2.z().toDouble()

				// 主碰撞体（当前帧，亮白色）
				val currMat = brModel.computeBoneGlobalMatrix(
					pair.boneName, manager.getInterpolatedProxy(0f), true,
				)
				renderBox(
					poseStack, vertexConsumer, currMat, c2.modelCenter,
					hx, hy, hz, 1f, 1f, 1f, 1f
				)

				// sweep 路径（与 MultiCollider 相同的骨骼插值，数量由 AttackActionPhase 指定）
				val colliderCount = phase.colliderCount
				for (step in 0 until colliderCount) {
					val t = if (colliderCount > 1) step / (colliderCount - 1f) else 1f
					val sweepMat = brModel.computeBoneGlobalMatrix(
						pair.boneName, manager.getInterpolatedProxy(t), true,
					)
					val alpha = 0.15f + t * 0.25f
					renderBox(
						poseStack, vertexConsumer, sweepMat, c2.modelCenter,
						hx, hy, hz, 0.3f, 1f, 0.3f, alpha
					)
				}
			}
		}
	}

	/**
	 * 渲染单个 OBB 包围盒线框。
	 */
	private fun renderBox(
		poseStack: PoseStack, vertexConsumer: VertexConsumer,
		matrix: Matrix4f, center: Vector3fc,
		hx: Double, hy: Double, hz: Double,
		r: Float, g: Float, b: Float, a: Float,
	) {
		poseStack.pushPose()
		poseStack.mulPose(matrix)
		poseStack.translate(center)
		LevelRenderer.renderLineBox(poseStack, vertexConsumer, -hx, -hy, -hz, hx, hy, hz, r, g, b, a)
		poseStack.popPose()
	}
}
