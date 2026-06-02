package architecture.resonator_combat_framework.module.entity_animation.mapper

import architecture.resonator_combat_framework.module.entity_animation.api.ProxyModel
import architecture.resonator_combat_framework.module.entity_animation.controller.BaseAnimationController
import architecture.resonator_combat_framework.module.entity_animation.data.ProxyBoneFlags
import architecture.resonator_combat_framework.module.entity_animation.event.AnimationControllerRegisterEvent
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.model.PlayerModel
import net.minecraft.world.entity.player.Player
import net.neoforged.neoforge.common.NeoForge

/** 玩家动画映射器：零外部依赖 + 渲染入口 */
class PlayerAnimationMapper(
	val player: Player
) : HumanoidEntityAnimationMapper<Player, PlayerModel<Player>>(player) {

	private var lastRenderTick = 0f
	private var currentPartialTick = 0f

	init {
		NeoForge.EVENT_BUS.post(AnimationControllerRegisterEvent()).getSortedEntries()
			.forEach { (controllerName, controllerFactory, priority) ->
				animationControllerManager.add(controllerName, controllerFactory(controllerName, isClient))
			}
	}


	/** 附加层：外套/袖子/裤腿 */
	override fun applyProxyToModel(
		proxyModels: List<ProxyModel>,
		model: PlayerModel<Player>,
		flags: Map<String, ProxyBoneFlags>,
		weight: Float
	) {
		if (!isClient) return
		super.applyProxyToModel(proxyModels, model, flags, weight)
		for (proxy in proxyModels) {
			applyProxyBone(proxy, "body", flags, weight, model.jacket)
			applyProxyBone(proxy, "left_arm", flags, weight, model.leftSleeve)
			applyProxyBone(proxy, "right_arm", flags, weight, model.rightSleeve)
			applyProxyBone(proxy, "left_leg", flags, weight, model.leftPants)
			applyProxyBone(proxy, "right_leg", flags, weight, model.rightPants)
		}
	}

	/**
	 * 由 Mixin 每帧调用：tick 所有控制器，逐控制器渲染到模型。
	 * 每个控制器使用自己的权重和骨骼标志独立应用。
	 */
	fun tickAndRender(model: PlayerModel<*>, partialTick: Float, poseStack: PoseStack) {
		if (!isClient || !isActive()) return
		val tickSec = (player.tickCount + partialTick) / 20f
		val deltaSec = if (lastRenderTick == 0f) 0f else tickSec - lastRenderTick
		lastRenderTick = tickSec
		currentPartialTick = partialTick

		for (ctrl in animationControllerManager.getAll()) {
			(ctrl as BaseAnimationController).tickRender(partialTick, deltaSec)
		}
		val renderable = animationControllerManager.getRenderable()
		if (renderable.isEmpty()) return
		for (ctrl in renderable) {
			val bac = ctrl as BaseAnimationController
			val flags = bac.resolveBoneFlags(bac.currentAnimTime)
			val weight = ctrl.effectiveWeight

			applyRootTransform(listOf(bac.getInterpolatedProxy(partialTick)), poseStack, flags, weight)
			applyProxyToModel(listOf(bac.getInterpolatedProxy(partialTick)), model as PlayerModel<Player>, flags, weight)
		}
	}

	/** ItemInHandLayerMixin 调用：物品定位器 → PoseStack */
	fun applyItemTransform(isLeft: Boolean, poseStack: PoseStack) {
		if (!isClient) return
		val renderable = getRenderableControllers()
		if (renderable.isEmpty()) return
		for (ctrl in renderable) {
			val bac = ctrl as BaseAnimationController
			val flags = bac.resolveBoneFlags(bac.currentAnimTime)
			applyProxyToItem(
				listOf(bac.getInterpolatedProxy(currentPartialTick)), isLeft, poseStack, flags,
				ctrl.effectiveWeight
			)
		}
	}
}

