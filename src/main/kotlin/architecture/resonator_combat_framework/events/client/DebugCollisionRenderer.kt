package architecture.resonator_combat_framework.events.client

import architecture.resonator_combat_framework.module.collision.CollisionSystem
import architecture.resonator_combat_framework.module.collision.collision.CollisionShape
import architecture.resonator_combat_framework.module.collision.collision.ConvexHull
import architecture.resonator_combat_framework.module.collision.collision.OBB
import architecture.resonator_combat_framework.util.RcfUtil
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Camera
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.LevelRenderer
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
 * F3+B 碰撞体调试渲染 —— 在开启 F3+B 碰撞箱时绘制 [CollisionSystem] 中所有实体的碰撞体形状。
 *
 * 渲染时机：[RenderLevelStageEvent.Stage.AFTER_ENTITIES]
 * 仅在 [EntityRenderDispatcher.shouldRenderHitBoxes] 为 true 时生效。
 *
 * 颜色约定：
 * - 红色：OBB 碰撞体
 * - 蓝色：ConvexHull 碰撞体
 * - 绿色：其他类型碰撞体
 */
@EventBusSubscriber(modid = RcfUtil.ID, value = [Dist.CLIENT])
object DebugCollisionRenderer {

    // 颜色常量（ARGB）
    private const val COLOR_OBB = 0xFFFF4444       // 红色
    private const val COLOR_CONVEX_HULL = 0xFF4444FF // 蓝色
    private const val COLOR_OTHER = 0xFF44FF44       // 绿色

    /**
     * 在世界渲染的 AFTER_ENTITIES 阶段绘制碰撞体调试线框。
     *
     * @param event 渲染事件
     */
    @SubscribeEvent
    fun onRenderLevelStage(event: RenderLevelStageEvent) {
        if (event.stage != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return
        if (!Minecraft.getInstance().entityRenderDispatcher.shouldRenderHitBoxes()) return

        val poseStack = event.poseStack
        val camera = event.camera
        val partialTick = event.partialTick

        val bufferSource = Minecraft.getInstance().renderBuffers().bufferSource()
        val consumer = bufferSource.getBuffer(RenderType.lines())
        val level = Minecraft.getInstance().level ?: return

        // 遍历世界中所有 LivingEntity，检查是否有活跃碰撞体
        for (entity in level.getAllEntities()) {
            if (entity !is LivingEntity || !entity.isAlive) continue
            renderCollisionShapes(entity, poseStack, camera, partialTick, consumer)
        }
    }

    /**
     * 渲染指定实体的所有碰撞形状。
     *
     * @param entity      持有碰撞数据的实体
     * @param poseStack   当前模型视图矩阵堆栈
     * @param camera      当前摄像机
     * @param partialTick 部分 tick 插值
     * @param consumer    线框渲染的顶点消费者
     */
    private fun renderCollisionShapes(
        entity: LivingEntity,
        poseStack: PoseStack,
        camera: Camera,
        partialTick: Float,
        consumer: net.minecraft.client.renderer.MultiBufferSource,
    ) {
        val data = CollisionSystem.getData(entity)
        if (data.activeColliders.isEmpty()) return

        // 获取实体插值位置
        val entityPos = entity.getPosition(partialTick)
        val cameraPos = camera.position

        for (entry in data.activeColliders) {
            val shape = entry.shape
            val color = when (shape) {
                is OBB -> COLOR_OBB
                is ConvexHull -> COLOR_CONVEX_HULL
                else -> COLOR_OTHER
            }
            renderShape(shape, entry.worldMatrix, entityPos, cameraPos, poseStack, consumer, color)
        }
    }

    /**
     * 根据碰撞体类型分发到对应的渲染方法。
     */
    private fun renderShape(
        shape: CollisionShape,
        worldMatrix: Matrix4f?,
        entityPos: Vec3,
        cameraPos: Vec3,
        poseStack: PoseStack,
        consumer: net.minecraft.client.renderer.MultiBufferSource,
        color: Int,
    ) {
        when (shape) {
            is OBB -> renderOBB(shape, worldMatrix, entityPos, cameraPos, poseStack, consumer, color)
            is ConvexHull -> renderConvexHull(shape, worldMatrix, entityPos, cameraPos, poseStack, consumer, color)
        }
    }

    /**
     * 将局部坐标点转换到世界空间。
     *
     * @param localPoint  局部坐标点（相对于实体/骨骼原点）
     * @param worldMatrix 世界变换矩阵，为 null 时使用实体位置直接偏移
     * @param entityPos   实体世界坐标
     * @return 世界空间坐标
     */
    private fun toWorld(
        localPoint: Vector3f,
        worldMatrix: Matrix4f?,
        entityPos: Vec3,
    ): Vec3 {
        return if (worldMatrix != null) {
            val world = Vector3f(localPoint).mulPosition(worldMatrix)
            Vec3(world.x.toDouble(), world.y.toDouble(), world.z.toDouble())
        } else {
            Vec3(
                entityPos.x + localPoint.x.toDouble(),
                entityPos.y + localPoint.y.toDouble(),
                entityPos.z + localPoint.z.toDouble(),
            )
        }
    }

    // ===== OBB 渲染 =====

    /**
     * 以线框方式渲染 OBB 碰撞体。
     * 将 OBB 的 12 条边转换为世界坐标后绘制。
     */
    private fun renderOBB(
        obb: OBB,
        worldMatrix: Matrix4f?,
        entityPos: Vec3,
        cameraPos: Vec3,
        poseStack: PoseStack,
        consumer: net.minecraft.client.renderer.MultiBufferSource,
        color: Int,
    ) {
        val hx = obb.halfExtents.x
        val hy = obb.halfExtents.y
        val hz = obb.halfExtents.z
        val center = obb.center

        // 8 个角点（局部坐标，含 center 偏移）
        val corners = arrayOf(
            Vector3f(center.x - hx, center.y - hy, center.z - hz),
            Vector3f(center.x + hx, center.y - hy, center.z - hz),
            Vector3f(center.x + hx, center.y - hy, center.z + hz),
            Vector3f(center.x - hx, center.y - hy, center.z + hz),
            Vector3f(center.x - hx, center.y + hy, center.z - hz),
            Vector3f(center.x + hx, center.y + hy, center.z - hz),
            Vector3f(center.x + hx, center.y + hy, center.z + hz),
            Vector3f(center.x - hx, center.y + hy, center.z + hz),
        )

        // 12 条边的索引对（底 4 + 顶 4 + 垂直 4）
        val edges = intArrayOf(
            0, 1, 1, 2, 2, 3, 3, 0, // 底面
            4, 5, 5, 6, 6, 7, 7, 4, // 顶面
            0, 4, 1, 5, 2, 6, 3, 7, // 垂直棱
        )

        // 转换到世界坐标
        val worldCorners = corners.map { toWorld(it, worldMatrix, entityPos) }

        // 绘制每条边
        val dx = cameraPos.x
        val dy = cameraPos.y
        val dz = cameraPos.z

        poseStack.pushPose()
        // 使用 LevelRenderer.renderLineBox 风格的直接绘制
        // 需要从观察点偏移以正确渲染世界坐标中的线
        val pose = poseStack.last().pose()

        for (i in edges.indices step 2) {
            val from = worldCorners[edges[i]]
            val to = worldCorners[edges[i + 1]]
            drawLine(pose, consumer, color, from, to, dx, dy, dz)
        }

        poseStack.popPose()
    }

    // ===== ConvexHull 渲染 =====

    /**
     * 以线框方式渲染 ConvexHull 碰撞体。
     * 遍历所有顶点对，绘制相邻顶点之间的边。
     */
    private fun renderConvexHull(
        hull: ConvexHull,
        worldMatrix: Matrix4f?,
        entityPos: Vec3,
        cameraPos: Vec3,
        poseStack: PoseStack,
        consumer: net.minecraft.client.renderer.MultiBufferSource,
        color: Int,
    ) {
        if (hull.vertices.size < 2) return

        val dx = cameraPos.x
        val dy = cameraPos.y
        val dz = cameraPos.z
        val pose = poseStack.last().pose()

        // 转换所有顶点到世界坐标
        val worldVerts = hull.vertices.map { toWorld(it, worldMatrix, entityPos) }

        // 绘制所有边缘：对每对顶点绘制一条线
        for (i in worldVerts.indices) {
            for (j in i + 1 until worldVerts.size) {
                drawLine(pose, consumer, color, worldVerts[i], worldVerts[j], dx, dy, dz)
            }
        }
    }

    // ===== 底层绘制 =====

    /**
     * 在三维空间中绘制一条线段。
     * 使用 [LevelRenderer] 的 ADD 混合线渲染器，确保线框始终可见。
     */
    private fun drawLine(
        pose: org.joml.Matrix4f,
        consumer: net.minecraft.client.renderer.MultiBufferSource,
        color: Int,
        from: Vec3,
        to: Vec3,
        camX: Double,
        camY: Double,
        camZ: Double,
    ) {
        val alpha = (color shr 24 and 0xFF).toFloat() / 255f
        val red = (color shr 16 and 0xFF).toFloat() / 255f
        val green = (color shr 8 and 0xFF).toFloat() / 255f
        val blue = (color and 0xFF).toFloat() / 255f

        val buffer = consumer.getBuffer(RenderType.lines())
        buffer.addVertex(pose, (from.x - camX).toFloat(), (from.y - camY).toFloat(), (from.z - camZ).toFloat())
            .setColor(red, green, blue, alpha)
        buffer.addVertex(pose, (to.x - camX).toFloat(), (to.y - camY).toFloat(), (to.z - camZ).toFloat())
            .setColor(red, green, blue, alpha)
    }

    /**
     * 使用 [LevelRenderer.renderLineBox] 绘制一个 AABB 线框。
     * 当需要快速调试 AABB 时使用。
     */
    @Suppress("unused")
    private fun renderAABB(
        poseStack: PoseStack,
        consumer: net.minecraft.client.renderer.MultiBufferSource,
        minX: Double, minY: Double, minZ: Double,
        maxX: Double, maxY: Double, maxZ: Double,
        red: Float, green: Float, blue: Float, alpha: Float,
    ) {
        LevelRenderer.renderLineBox(
            poseStack,
            consumer,
            minX, minY, minZ,
            maxX, maxY, maxZ,
            red, green, blue, alpha,
        )
    }
}
