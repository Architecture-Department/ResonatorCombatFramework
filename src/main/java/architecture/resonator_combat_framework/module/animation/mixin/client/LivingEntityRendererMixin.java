package architecture.resonator_combat_framework.module.animation.mixin.client;

import architecture.resonator_combat_framework.module.animation.mapper.IEntityAnimationMapperProvider;
import architecture.resonator_combat_framework.module.collision.CollisionEntry;
import architecture.resonator_combat_framework.module.collision.CollisionSystem;
import architecture.resonator_combat_framework.module.collision.collision.CollisionShape;
import architecture.resonator_combat_framework.module.collision.collision.ConvexHull;
import architecture.resonator_combat_framework.module.collision.collision.OBB;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
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
import org.spongepowered.asm.mixin.Unique;
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

	@Unique
	@Inject(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
		at = @At("TAIL"))
	public void rcf$renderDebugColliders(
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
		if (data.getActiveColliders().isEmpty()) return;

		Camera cam = Minecraft.getInstance().getEntityRenderDispatcher().camera;
		float camX = (float) cam.getPosition().x;
		float camY = (float) cam.getPosition().y;
		float camZ = (float) cam.getPosition().z;
		var pose = poseStack.last().pose();
		VertexConsumer consumer = buffer.getBuffer(RenderType.lines());

		for (CollisionEntry entry : data.getActiveColliders()) {
			CollisionShape shape = entry.getShape();
			Matrix4f worldMatrix = entry.getWorldMatrix();
			int color = shape instanceof OBB ? 0xFFFF4444 : 0xFF4444FF;

			if (shape instanceof OBB obb) {
				RCF$renderOBB(pose, consumer, obb, worldMatrix, entity, camX, camY, camZ, color);
			} else if (shape instanceof ConvexHull hull) {
				RCF$renderConvexHull(pose, consumer, hull, worldMatrix, entity, camX, camY, camZ, color);
			}
		}
	}

	// ===== 渲染辅助方法 =====

	@Unique
	private static Vec3 RCF$toCameraRelative(
		Vector3f localPoint,
		Matrix4f worldMatrix,
		Entity entity,
		float camX, float camY, float camZ
	) {
		double wx, wy, wz;
		if (worldMatrix != null) {
			Vector3f world = new Vector3f(localPoint).mulPosition(worldMatrix);
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

	@Unique
	private static void RCF$renderOBB(
		Matrix4f pose,
		VertexConsumer consumer,
		OBB obb,
		Matrix4f worldMatrix,
		Entity entity,
		float camX, float camY, float camZ,
		int color
	) {
		Vector3f halfExtents = obb.getHalfExtents();
		Vector3f center = obb.getCenter();
		float hx = halfExtents.x();
		float hy = halfExtents.y();
		float hz = halfExtents.z();
		float cx = center.x();
		float cy = center.y();
		float cz = center.z();

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

		int[] edges = new int[] {
			0, 1, 1, 2, 2, 3, 3, 0,
			4, 5, 5, 6, 6, 7, 7, 4,
			0, 4, 1, 5, 2, 6, 3, 7,
		};

		Vec3[] worldCorners = new Vec3[8];
		for (int i = 0; i < 8; i++) {
			worldCorners[i] = RCF$toCameraRelative(corners[i], worldMatrix, entity, camX, camY, camZ);
		}

		float r = ((color >> 16) & 0xFF) / 255f;
		float g = ((color >> 8) & 0xFF) / 255f;
		float b = (color & 0xFF) / 255f;
		float a = ((color >> 24) & 0xFF) / 255f;

		for (int i = 0; i < edges.length; i += 2) {
			Vec3 from = worldCorners[edges[i]];
			Vec3 to = worldCorners[edges[i + 1]];
			consumer.addVertex(pose, (float) from.x, (float) from.y, (float) from.z)
				.setColor(r, g, b, a);
			consumer.addVertex(pose, (float) to.x, (float) to.y, (float) to.z)
				.setColor(r, g, b, a);
		}
	}

	@Unique
	private static void RCF$renderConvexHull(
		Matrix4f pose,
		VertexConsumer consumer,
		ConvexHull hull,
		Matrix4f worldMatrix,
		Entity entity,
		float camX, float camY, float camZ,
		int color
	) {
		java.util.List<Vector3f> vertices = hull.getVertices();
		if (vertices.size() < 2) return;

		float r = ((color >> 16) & 0xFF) / 255f;
		float g = ((color >> 8) & 0xFF) / 255f;
		float b = (color & 0xFF) / 255f;
		float a = ((color >> 24) & 0xFF) / 255f;

		int n = vertices.size();
		Vec3[] worldVerts = new Vec3[n];
		for (int i = 0; i < n; i++) {
			worldVerts[i] = RCF$toCameraRelative(vertices.get(i), worldMatrix, entity, camX, camY, camZ);
		}

		for (int i = 0; i < n; i++) {
			for (int j = i + 1; j < n; j++) {
				consumer.addVertex(pose, (float) worldVerts[i].x, (float) worldVerts[i].y, (float) worldVerts[i].z)
					.setColor(r, g, b, a);
				consumer.addVertex(pose, (float) worldVerts[j].x, (float) worldVerts[j].y, (float) worldVerts[j].z)
					.setColor(r, g, b, a);
			}
		}
	}
}
