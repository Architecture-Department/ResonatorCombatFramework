package architecture.resonator_combat_framework.module.player_animation.mapper

import architecture.resonator_combat_framework.module.player_animation.api.ProxyModel
import architecture.resonator_combat_framework.module.player_animation.config.ProxyBoneFlags
import architecture.resonator_combat_framework.module.player_animation.controller.BaseAnimationController
import architecture.resonator_combat_framework.module.player_animation.event.AnimationControllerRegisterEvent
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.model.PlayerModel
import net.minecraft.world.entity.player.Player
import net.neoforged.neoforge.common.NeoForge

/** 玩家动画映射器：零外部依赖 + 渲染入口 */
class PlayerAnimationMapper(
	val player: Player
) : HumanoidEntityAnimationMapper<Player, PlayerModel<Player>>(player) {

	private var lastRenderTick = 0f

	init {
		NeoForge.EVENT_BUS.post(AnimationControllerRegisterEvent()).getSortedEntries()
			.forEach { (controllerName, controllerFactory, priority) ->
				controllerManager.add(controllerName, controllerFactory(isClient))
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
	 * 由 Mixin 每帧调用：tick 控制器 → 合并数据 → 渲染到模型。
	 */
	fun tickAndRender(model: PlayerModel<*>, partialTick: Float, poseStack: PoseStack) {
		if (!isClient || !isActive()) return
		val tickSec = (player.tickCount + partialTick) / 20f
		val deltaSec = if (lastRenderTick == 0f) 0f else tickSec - lastRenderTick
		lastRenderTick = tickSec

		tick(tickSec, deltaSec)
		val proxyModels = collectProxyModels()
		if (proxyModels.isEmpty()) return
		val flags = resolveMergedFlags(defaultController.currentAnimTime)
		val weight = mergedWeight()

		applyRootTransform(proxyModels, poseStack, flags, weight)
		applyProxyToModel(proxyModels, model as PlayerModel<Player>, flags, weight)
	}

	/** ItemInHandLayerMixin 调用：物品定位器 → PoseStack */
	fun applyItemTransform(isLeft: Boolean, poseStack: PoseStack) {
		if (!isClient) return
		val renderable = getRenderableControllers()
		if (renderable.isEmpty()) return
		applyProxyToItem(
			renderable.map { (it as BaseAnimationController).proxyModel },
			isLeft, poseStack,
			resolveMergedFlags(defaultController.currentAnimTime),
			defaultController.effectiveWeight,
		)
	}
}