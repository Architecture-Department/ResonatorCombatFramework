package architecture.resonator_combat_framework.module.player_animation.mapper

import architecture.resonator_combat_framework.module.player_animation.PlayerAnimationSetup
import architecture.resonator_combat_framework.module.player_animation.api.IAnimationMapper
import architecture.resonator_combat_framework.module.player_animation.api.ProxyModel
import architecture.resonator_combat_framework.module.player_animation.config.ProxyBoneFlags
import architecture.resonator_combat_framework.module.player_animation.controller.IAnimationController
import architecture.resonator_combat_framework.module.player_animation.controller.eyelib.EyeLibAnimationController
import com.mojang.blaze3d.vertex.PoseStack
import io.github.tt432.eyelib.capability.RenderData
import io.github.tt432.eyelib.client.ClientTickHandler
import net.minecraft.client.model.PlayerModel
import net.minecraft.world.entity.player.Player

/** 玩家动画映射器 — eyelib 后端 + 渲染入口 */
class PlayerAnimationMapper(
	val player: Player
) : HumanoidEntityAnimationMapper<Player, PlayerModel<Player>>(player) {

	private val renderData: RenderData<Player> = RenderData.getComponent(player)

	override val controllers = linkedMapOf<String, IAnimationController>()
	override val defaultController =
		EyeLibAnimationController(renderData, isClient).also { controllers[IAnimationMapper.DEFAULT_CONTROLLER_NAME] = it }

	private var lastRenderTick = 0f

	init {
		PlayerAnimationSetup.setupRenderData(renderData)
	}

	/** 扩展父类: jacket/sleeve/pants 附加层 */
	override fun applyProxyToModel(
		proxyModels: List<ProxyModel>,
		model: PlayerModel<Player>,
		flags: Map<String, ProxyBoneFlags>,
		weight: Float
	) {
		super.applyProxyToModel(proxyModels, model, flags, weight)
		for (proxy in proxyModels) {
			applyProxyBone(proxy, "body", flags, weight, model.jacket)
			applyProxyBone(proxy, "left_arm", flags, weight, model.leftSleeve)
			applyProxyBone(proxy, "right_arm", flags, weight, model.rightSleeve)
			applyProxyBone(proxy, "left_leg", flags, weight, model.leftPants)
			applyProxyBone(proxy, "right_leg", flags, weight, model.rightPants)
		}
	}

	override fun trigger(animId: String) = trigger(resolveController(animId), animId)

	override fun trigger(controllerName: String, animId: String) {
		super.trigger(controllerName, animId)
		animTimeTracker = 0f
		lastRenderTick = 0f
	}

	/** 由 LivingEntityRendererMixin 每帧调用: 驱动 eyelib → ProxyModel → ModelPart */
	@Suppress("UNCHECKED_CAST")
	fun applyTransform(model: PlayerModel<*>, partialTick: Float) {
		if (!isClient) return
		val tickSec = (ClientTickHandler.getTick() + partialTick) / 20f
		val deltaSec = if (lastRenderTick == 0f) 0f else tickSec - lastRenderTick
		lastRenderTick = tickSec
		animTimeTracker += deltaSec

		// 驱动所有控制器
		for (ctrl in controllers.values) {
			ctrl.tick(partialTick, deltaSec)
		}

		// 使用优先级过滤获取可渲染的控制器
		val renderableControllers = getRenderableControllers()
		if (renderableControllers.isEmpty()) return

		val proxyModels = renderableControllers.map { (it as EyeLibAnimationController).proxyModel }
		val root = defaultController
		applyProxyToModel(proxyModels, model as PlayerModel<Player>, resolveBoneFlags(animTimeTracker), root.blendFactor)
	}

	/** 由 ItemInHandLayerMixin 调用: ProxyLocator → PoseStack, 支持 blendFactor 过渡 */
	fun applyItemTransform(isLeft: Boolean, poseStack: PoseStack) {
		val renderableControllers = getRenderableControllers()
		if (renderableControllers.isEmpty()) return
		val weight = defaultController.blendFactor
		applyProxyToItem(
			renderableControllers.map { (it as EyeLibAnimationController).proxyModel },
			isLeft,
			poseStack,
			weight
		)
	}
}
