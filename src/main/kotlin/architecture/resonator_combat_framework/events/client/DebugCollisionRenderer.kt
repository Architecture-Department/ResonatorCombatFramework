package architecture.resonator_combat_framework.events.client

import architecture.resonator_combat_framework.module.collision.CollisionSystem
import architecture.resonator_combat_framework.module.collision.collision.ConvexHull
import architecture.resonator_combat_framework.module.collision.collision.OBB
import architecture.resonator_combat_framework.util.RcfUtil
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.Camera
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.phys.Vec3
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.RenderLevelStageEvent
import org.joml.Matrix4f
import org.joml.Vector3f

/**
 * F3+B 碰撞体调试渲染。
 * 使用 [RenderLevelStageEvent] 绘制 [CollisionSystem] 中所有实体的活跃碰撞体。
 * 仅在开启 F3+B 碰撞箱时渲染。
 *
 * 颜色：OBB 红色，ConvexHull 蓝色
 */
@EventBusSubscriber(modid = RcfUtil.ID, value = [Dist.CLIENT])
object DebugCollisionRenderer {

    private const val COLOR_OBB = 0xFFFF4444
    private const val COLOR_CONVEX_HULL = 0xFF4444FF

    @SubscribeEvent
    fun onRenderLevelStage(event: RenderLevelStageEvent) {
        if (event.stage !== RenderLevelStageEvent.Stage.AFTER_ENTITIES) return
        if (!Minecraft.getInstance().entityRenderDispatcher.shouldRenderHitBoxes()) return

        val mc = Minecraft.getInstance()
        val level = mc.level ?: return
        val camPos: Vec3 = event.camera.position
        val poseStack = event.poseStack
        val pose = poseStack.last().pose()
        val consumer: VertexConsumer = mc.renderBuffers().bufferSource().getBuffer(RenderType.lines())

        for (entity in level.getEntities().getAll()) {
            if (entity !is LivingEntity || !entity.isAlive) continue
            val data = CollisionSystem.getData(entity)
            if (data.activeColliders.isEmpty()) continue
            renderColliders(data, entity, pose, consumer, camPos)
        }
    }

    private fun renderColliders(
        data: architecture.resonator_combat_framework.module.collision.CollisionEntityData,
        entity: LivingEntity,
        pose: org.joml.Matrix4f,
        consumer: VertexConsumer,
        camPos: Vec3,
    ) {
        val ex = entity.x
        val ey = entity.y
        val ez = entity.z

        for (entry in data.activeColliders) {
            val shape = entry.shape
            val worldMatrix = entry.worldMatrix
            val color = if (shape is OBB) COLOR_OBB else COLOR_CONVEX_HULL

            when (shape) {
                is OBB -> renderOBB(pose, consumer, shape, worldMatrix, ex, ey, ez, color, camPos)
                is ConvexHull -> renderConvexHull(pose, consumer, shape, worldMatrix, ex, ey, ez, color, camPos)
            }
        }
    }

    private fun toWorld(
        local: Vector3f,
        worldMatrix: Matrix4f?,
        ex: Double, ey: Double, ez: Double,
    ): Vec3 {
        return if (worldMatrix != null) {
            val w = Vector3f(local).mulPosition(worldMatrix)
            Vec3(w.x.toDouble(), w.y.toDouble(), w.z.toDouble())
        } else {
            Vec3(ex + local.x.toDouble(), ey + local.y.toDouble(), ez + local.z.toDouble())
        }
    }

    private fun renderOBB(
        pose: org.joml.Matrix4f,
        consumer: VertexConsumer,
        obb: OBB,
        worldMatrix: Matrix4f?,
        ex: Double, ey: Double, ez: Double,
        color: Int,
        camPos: Vec3,
    ) {
        val h = obb.halfExtents
        val c = obb.center
        val corners = arrayOf(
            Vector3f(c.x - h.x, c.y - h.y, c.z - h.z),
            Vector3f(c.x + h.x, c.y - h.y, c.z - h.z),
            Vector3f(c.x + h.x, c.y - h.y, c.z + h.z),
            Vector3f(c.x - h.x, c.y - h.y, c.z + h.z),
            Vector3f(c.x - h.x, c.y + h.y, c.z - h.z),
            Vector3f(c.x + h.x, c.y + h.y, c.z - h.z),
            Vector3f(c.x + h.x, c.y + h.y, c.z + h.z),
            Vector3f(c.x - h.x, c.y + h.y, c.z + h.z),
        )
        val edges = intArrayOf(
            0, 1, 1, 2, 2, 3, 3, 0,
            4, 5, 5, 6, 6, 7, 7, 4,
            0, 4, 1, 5, 2, 6, 3, 7,
        )

        val worldCorners = corners.map { toWorld(it, worldMatrix, ex, ey, ez) }
        val r = ((color shr 16) and 0xFF) / 255f
        val g = ((color shr 8) and 0xFF) / 255f
        val b = (color and 0xFF) / 255f
        val a = ((color shr 24) and 0xFF) / 255f

        for (i in edges.indices step 2) {
            val p1 = worldCorners[edges[i]]
            val p2 = worldCorners[edges[i + 1]]
            consumer.addVertex(pose,
                (p1.x - camPos.x).toFloat(),
                (p1.y - camPos.y).toFloat(),
                (p1.z - camPos.z).toFloat(),
            ).setColor(r, g, b, a)
            consumer.addVertex(pose,
                (p2.x - camPos.x).toFloat(),
                (p2.y - camPos.y).toFloat(),
                (p2.z - camPos.z).toFloat(),
            ).setColor(r, g, b, a)
        }
    }

    private fun renderConvexHull(
        pose: org.joml.Matrix4f,
        consumer: VertexConsumer,
        hull: ConvexHull,
        worldMatrix: Matrix4f?,
        ex: Double, ey: Double, ez: Double,
        color: Int,
        camPos: Vec3,
    ) {
        val verts = hull.vertices
        if (verts.size < 2) return
        val worldVerts = verts.map { toWorld(it, worldMatrix, ex, ey, ez) }
        val r = ((color shr 16) and 0xFF) / 255f
        val g = ((color shr 8) and 0xFF) / 255f
        val b = (color and 0xFF) / 255f
        val a = ((color shr 24) and 0xFF) / 255f

        for (i in worldVerts.indices) {
            for (j in i + 1 until worldVerts.size) {
                consumer.addVertex(pose,
                    (worldVerts[i].x - camPos.x).toFloat(),
                    (worldVerts[i].y - camPos.y).toFloat(),
                    (worldVerts[i].z - camPos.z).toFloat(),
                ).setColor(r, g, b, a)
                consumer.addVertex(pose,
                    (worldVerts[j].x - camPos.x).toFloat(),
                    (worldVerts[j].y - camPos.y).toFloat(),
                    (worldVerts[j].z - camPos.z).toFloat(),
                ).setColor(r, g, b, a)
            }
        }
    }
}
