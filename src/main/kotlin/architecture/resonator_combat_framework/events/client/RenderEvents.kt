package architecture.resonator_combat_framework.events.client

import architecture.resonator_combat_framework.module.collision.CollisionEntry
import architecture.resonator_combat_framework.module.collision.CollisionSystem
import architecture.resonator_combat_framework.module.collision.collision.BoneCollider
import architecture.resonator_combat_framework.module.collision.collision.OrientedBox
import architecture.resonator_combat_framework.util.RcfUtil
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import com.mojang.math.Axis
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.LevelRenderer
import net.minecraft.client.renderer.RenderType
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.RenderLevelStageEvent
import org.joml.Matrix4f

@EventBusSubscriber(modid = RcfUtil.ID, value = [Dist.CLIENT])
object RenderEvents {
	@SubscribeEvent
	fun onRenderLevelStage(event: RenderLevelStageEvent) {
		if (event.stage !== RenderLevelStageEvent.Stage.AFTER_ENTITIES) return
		val minecraft = Minecraft.getInstance()
		if (!minecraft.entityRenderDispatcher.shouldRenderHitBoxes()) return
		val level = minecraft.level ?: return
		if (level.entityCount <= 0) return
		val entitiesForRendering = level.entitiesForRendering()

		val poseStack = event.poseStack
		val timer = minecraft.timer
		val partialTicks = timer.getGameTimeDeltaPartialTick(false)
		val camera = event.camera
		val cameraPos = camera.position
		val bufferSource = minecraft.renderBuffers().bufferSource()

		poseStack.pushPose()
		poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z)
		val vertexConsumer = bufferSource.getBuffer(RenderType.lines())
		entitiesForRendering.filter {
			it.isAlive && (if (it is Player) !it.isSpectator else true) && CollisionSystem.hasData(it)
		}.map {
			CollisionSystem.getData(it) to it
		}.filter { (k, v) ->
			!k.activeColliders.isEmpty()
		}.forEach { (k, v) ->
			poseStack.pushPose()
			val pos = v.getPosition(partialTicks)
			poseStack.translate(pos.x, pos.y, pos.z)
			k.activeColliders.forEach {
				poseStack.pushPose()
				renderCollider(it, poseStack, vertexConsumer, pos)
				poseStack.popPose()
			}
			poseStack.popPose()
		}
		poseStack.popPose()
	}
}

/** 绘制单个碰撞体的线框 */
private fun renderCollider(
	entry: CollisionEntry,
	poseStack: PoseStack,
	vertexConsumer: VertexConsumer,
	entityPos: Vec3
) {
	val shape = entry.shape
	if (shape is OrientedBox) {
		val h = shape.halfExtents
		val c = shape.center

		if (entry.worldMatrix != null) {
			// 应用世界矩阵（相对实体位置），将骨骼局部坐标变换到渲染空间
			val localMatrix = Matrix4f(entry.worldMatrix)
			localMatrix.m30(localMatrix.m30() - entityPos.x.toFloat())
			localMatrix.m31(localMatrix.m31() - entityPos.y.toFloat())
			localMatrix.m32(localMatrix.m32() - entityPos.z.toFloat())
			poseStack.last().pose().mul(localMatrix)
			// BoneCollider 额外应用局部旋转（ZYX 顺序，与检测逻辑一致）
			if (shape is BoneCollider) {
				poseStack.mulPose(Axis.ZP.rotationDegrees(shape.rotation.z()))
				poseStack.mulPose(Axis.YP.rotationDegrees(shape.rotation.y()))
				poseStack.mulPose(Axis.XP.rotationDegrees(shape.rotation.x()))
			}
			LevelRenderer.renderLineBox(
				poseStack, vertexConsumer,
				AABB(
					(c.x - h.x).toDouble(), (c.y - h.y).toDouble(), (c.z - h.z).toDouble(),
					(c.x + h.x).toDouble(), (c.y + h.y).toDouble(), (c.z + h.z).toDouble()
				),
				0.8f, 0.8f, 1.0f, 0.8f
			)
		} else {
			// 无世界矩阵时直接使用实体局部坐标中的中心偏移
			poseStack.translate(c.x.toDouble(), c.y.toDouble(), c.z.toDouble())
			LevelRenderer.renderLineBox(
				poseStack, vertexConsumer,
				AABB(
					(-h.x).toDouble(), (-h.y).toDouble(), (-h.z).toDouble(),
					h.x.toDouble(), h.y.toDouble(), h.z.toDouble()
				),
				0.8f, 0.8f, 1.0f, 0.8f
			)
		}
	}
}
