package architecture.resonator_combat_framework.module.entity_animation.mapper

import architecture.resonator_combat_framework.module.entity_animation.api.ProxyModel
import architecture.resonator_combat_framework.module.entity_animation.data.ProxyBoneFlags
import architecture.resonator_combat_framework.module.entity_animation.event.AnimationControllerRegisterEvent
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.model.EntityModel
import net.minecraft.client.model.PlayerModel
import net.minecraft.world.entity.player.Player
import net.neoforged.neoforge.common.NeoForge

/** 玩家动画映射器——零外部依赖 + 渲染入口 */
class PlayerAnimationMapper(
	val player: Player
) : HumanoidEntityAnimationMapper<Player, PlayerModel<Player>>(player) {

	/** 上一渲染帧的 tickSec，用于计算 deltaSec */
	private var lastRenderTick = 0f

	/** 当前渲染帧的 partialTick，供 applyItemTransform 使用 */
	private var currentPartialTick = 0f

	init {
		NeoForge.EVENT_BUS.post(AnimationControllerRegisterEvent()).getSortedEntries()
			.forEach { (controllerName, controllerFactory, priority) ->
				animationControllerManager.add(controllerName, controllerFactory(controllerName, isClient))
			}
	}

	/** 附加层：外套/袖子/裤腿 */
	override fun applyProxyToModel(
		proxyModel: ProxyModel,
		model: PlayerModel<Player>,
		flags: Map<String, ProxyBoneFlags>
	) {
		if (!isClient) return
		super.applyProxyToModel(proxyModel, model, flags)
		applyProxyBone(proxyModel, "body", flags, model.jacket)
		applyProxyBone(proxyModel, "left_arm", flags, model.leftSleeve)
		applyProxyBone(proxyModel, "right_arm", flags, model.rightSleeve)
		applyProxyBone(proxyModel, "left_leg", flags, model.leftPants)
		applyProxyBone(proxyModel, "right_leg", flags, model.rightPants)
	}

	/** 由 Mixin 每帧调用：更新过渡状态 → 重新合并 → 渲染到模型 */
	override fun tickAndRender(model: EntityModel<*>, partialTick: Float, poseStack: PoseStack) {
		if (!isClient || !isActive()) return
		val playerModel = model as PlayerModel<Player>
		val tickSec = (player.tickCount + partialTick) / 20f
		val deltaSec = if (lastRenderTick == 0f) 0f else tickSec - lastRenderTick
		lastRenderTick = tickSec
		currentPartialTick = partialTick

		for (ctrl in animationControllerManager.getAll()) {
			ctrl.tickRender(deltaSec)
		}
		animationControllerManager.remerge()
		val renderable = animationControllerManager.getRenderable()
		if (renderable.isEmpty()) return

		val interpolatedProxyModel = animationControllerManager.getInterpolatedProxy(partialTick)
		applyRootTransform(interpolatedProxyModel, poseStack, animationControllerManager.mergedFlags)

		applyProxyToModel(interpolatedProxyModel, playerModel, animationControllerManager.mergedFlags)
	}

	/** ItemInHandLayerMixin 调用：物品定位器 → PoseStack */
	fun applyItemTransform(isLeft: Boolean, poseStack: PoseStack) {
		if (!isClient) return
		applyProxyToItem(
			animationControllerManager.getInterpolatedProxy(currentPartialTick),
			isLeft, poseStack, animationControllerManager.mergedFlags
		)
	}
}
