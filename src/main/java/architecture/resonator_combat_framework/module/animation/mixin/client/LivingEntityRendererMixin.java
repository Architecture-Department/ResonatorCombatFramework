package architecture.resonator_combat_framework.module.animation.mixin.client;

import architecture.resonator_combat_framework.module.animation.mapper.IEntityAnimationMapperProvider;
import architecture.resonator_combat_framework.module.collision.CollisionEntry;
import architecture.resonator_combat_framework.module.collision.CollisionSystem;
import architecture.resonator_combat_framework.module.collision.collision.CollisionShape;
import architecture.resonator_combat_framework.module.collision.collision.ConvexHull;
import architecture.resonator_combat_framework.module.collision.collision.OBB;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// 在 vanilla setupAnim() 之后、renderToBuffer() 之前注入
// 参考 TheElixir 模式：读 ModelPart 初始值 → 动画偏移 → 写回
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends LivingEntity> {
	@Shadow
	protected EntityModel<T> model;

	@Inject(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/LivingEntityRenderer;getOverlayCoords(Lnet/minecraft/world/entity/LivingEntity;F)I"))
	public void render(
		T entity,
		float entityYaw,
		float partialTicks,
		PoseStack poseStack,
		MultiBufferSource buffer,
		int packedLight,
		CallbackInfo ci
	) {
		if (!(entity instanceof Player player) || !(model instanceof PlayerModel<?> playerModel)) {
			return;
		}
		//noinspection rawtypes
		IEntityAnimationMapperProvider transformer = player.resonator_combat_framework$getMapperProvider();
		//noinspection unchecked
		transformer.tickAndRender(playerModel, partialTicks, poseStack);
	}

	// ===== F3+B 碰撞体调试渲染 =====

	@Inject(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
		at = @At("TAIL"))
	public void renderTail(
		T entity,
		float entityYaw,
		float partialTicks,
		PoseStack poseStack,
		MultiBufferSource buffer,
		int packedLight,
		CallbackInfo ci
	) {
		if (!Minecraft.getInstance().getEntityRenderDispatcher().shouldRenderHitBoxes()) return;
		var data = CollisionSystem.getData(entity);
		if (data.activeColliders.isEmpty()) return;

		var cam = Minecraft.getInstance().getEntityRenderDispatcher().camera;
		float camX = (float) cam.getPosition().x;
		float camY = (float) cam.getPosition().y;
		float camZ = (float) cam.getPosition().z;
		var pose = poseStack.last().pose();
		var consumer = buffer.getBuffer(RenderType.lines());

		for (CollisionEntry entry : data.activeColliders) {
			CollisionShape shape = entry.shape;
			var worldMatrix = entry.worldMatrix;
			int color = shape instanceof OBB ? 0xFFFF4444 : 0xFF4444FF;

			if (shape instanceof OBB obb) {
				renderOBB(pose, consumer, obb, worldMatrix, entity, camX, camY, camZ, color);
			} else if (shape instanceof ConvexHull hull) {
				renderConvexHull(pose, consumer, hull, worldMatrix, entity, camX, camY, camZ, color);
			}
		}
	}

	// ===== 渲染辅助方法 =====

	/**
	 * 将局部坐标转换到世界空间，再转为相机相对坐标。
	 */
	private static Vec3 toCameraRelative(
		Vector3f localPoint,
		Matrix4f worldMatrix,
		Entity entity,
		float camX, float camY, float camZ
	) {
		double wx, wy, wz;
		if (worldMatrix != null) {
			var world = new Vector3f(localPoint).mulPosition(worldMatrix);
			wx = world.x();
			wy = world.y();
			wz = world.z();
		} else {
			wx = entity.getX() + localPoint.x();
			wy = entity.getY() + localPoint.y();
			wz = entity.getZ() + localPoint.z();
		}
		return new Vec3(wx - camX, wy - camY, wz - camZ);
	}

	/**
	 * 渲染 OBB 的 12 条边线框。
	 */
	private static void renderOBB(
		org.joml.Matrix4f pose,
		MultiBufferSource consumer,
		OBB obb,
		Matrix4f worldMatrix,
		Entity entity,
		float camX, float camY, float camZ,
		int color
	) {
		float hx = obb.halfExtents.x();
		float hy = obb.halfExtents.y();
		float hz = obb.halfExtents.z();
		float cx = obb.center.x();
		float cy = obb.center.y();
		float cz = obb.center.z();

		// 8 个角点（局部坐标）
		Vector3f[] corners = new Vector3f[] {
			new Vector3f(cx - hx, cy - hy, cz - hz),
			new Vector3f(cx + hx, cy - hy, cz - hz),
			new Vector3f(cx + hx, cy - hy, cz + hz),
			new Vector3f(cx - hx, cy - hy, cz + hz),
			new Vector3f(cx - hx, cy + hy, cz - hz),
			new Vector3f(cx + hx, cy + hy, cz - hz),
			new Vector3f(cx + hx, cy + hy, cz + hz),
			new Vector3f(cx - hx, cy + hy, cz + hz),
		};

		// 12 条边的索引对
		int[] edges = new int[] {
			0, 1, 1, 2, 2, 3, 3, 0,
			4, 5, 5, 6, 6, 7, 7, 4,
			0, 4, 1, 5, 2, 6, 3, 7,
		};

		// 转换到相机相对坐标
		Vec3[] worldCorners = new Vec3[8];
		for (int i = 0; i < 8; i++) {
			worldCorners[i] = toCameraRelative(corners[i], worldMatrix, entity, camX, camY, camZ);
		}

		// 绘制每条边
		var buffer = consumer.getBuffer(RenderType.lines());
		float r = ((color >> 16) & 0xFF) / 255f;
		float g = ((color >> 8) & 0xFF) / 255f;
		float b = (color & 0xFF) / 255f;
		float a = ((color >> 24) & 0xFF) / 255f;

		for (int i = 0; i < edges.length; i += 2) {
			Vec3 from = worldCorners[edges[i]];
			Vec3 to = worldCorners[edges[i + 1]];
			buffer.addVertex(pose, (float) from.x, (float) from.y, (float) from.z)
				.setColor(r, g, b, a);
			buffer.addVertex(pose, (float) to.x, (float) to.y, (float) to.z)
				.setColor(r, g, b, a);
		}
	}

	/**
	 * 渲染 ConvexHull 的边线，绘制所有顶点对之间的连线。
	 */
	private static void renderConvexHull(
		org.joml.Matrix4f pose,
		MultiBufferSource consumer,
		ConvexHull hull,
		Matrix4f worldMatrix,
		Entity entity,
		float camX, float camY, float camZ,
		int color
	) {
		if (hull.vertices.size() < 2) return;

		var buffer = consumer.getBuffer(RenderType.lines());
		float r = ((color >> 16) & 0xFF) / 255f;
		float g = ((color >> 8) & 0xFF) / 255f;
		float b = (color & 0xFF) / 255f;
		float a = ((color >> 24) & 0xFF) / 255f;

		// 转换所有顶点到相机相对坐标
		int n = hull.vertices.size();
		Vec3[] worldVerts = new Vec3[n];
		for (int i = 0; i < n; i++) {
			worldVerts[i] = toCameraRelative(hull.vertices.get(i), worldMatrix, entity, camX, camY, camZ);
		}

		// 绘制所有顶点对
		for (int i = 0; i < n; i++) {
			for (int j = i + 1; j < n; j++) {
				buffer.addVertex(pose, (float) worldVerts[i].x, (float) worldVerts[i].y, (float) worldVerts[i].z)
					.setColor(r, g, b, a);
				buffer.addVertex(pose, (float) worldVerts[j].x, (float) worldVerts[j].y, (float) worldVerts[j].z)
					.setColor(r, g, b, a);
			}
		}
	}
}
