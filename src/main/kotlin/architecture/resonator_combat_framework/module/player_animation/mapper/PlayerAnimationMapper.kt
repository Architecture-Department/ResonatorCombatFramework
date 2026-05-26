package architecture.resonator_combat_framework.module.player_animation.mapper

import architecture.resonator_combat_framework.module.player_animation.PlayerAnimationSetup
import architecture.resonator_combat_framework.module.player_animation.api.IAnimationMapper
import architecture.resonator_combat_framework.module.player_animation.api.ProxyModel
import architecture.resonator_combat_framework.module.player_animation.config.AnimationPlayConfig
import architecture.resonator_combat_framework.module.player_animation.config.ProxyBoneFlags
import architecture.resonator_combat_framework.module.player_animation.controller.BaseAnimationController
import architecture.resonator_combat_framework.module.player_animation.controller.IAnimationController
import architecture.resonator_combat_framework.module.player_animation.controller.eyelib.EyeLibAnimationController
import com.mojang.blaze3d.vertex.PoseStack
import io.github.tt432.eyelib.capability.RenderData
import io.github.tt432.eyelib.client.ClientTickHandler
import net.minecraft.client.model.PlayerModel
import net.minecraft.world.entity.player.Player

/** 玩家动画映射器：eyelib 后端 + 渲染入口 */
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

	override fun trigger(config: AnimationPlayConfig) {
		super.trigger(config)
		animTimeTracker = 0f
		lastRenderTick = 0f
	}

	override fun trigger(animId: String) = trigger(AnimationPlayConfig.of(animId))

	override fun trigger(controllerName: String, animId: String) {
		super.trigger(controllerName, animId)
		animTimeTracker = 0f
		lastRenderTick = 0f
	}

	/**
	 * 由 Mixin 每帧调用：tick 控制器 → 合并数据 → 渲染到模型。
	 * 控制器驱动与渲染分离，Mapper 负责合并多控制器数据。
	 */
	fun tickAndRender(model: PlayerModel<*>, partialTick: Float, poseStack: PoseStack) {
		if (!isClient || !isActive()) return
		val tickSec = (ClientTickHandler.getTick() + partialTick) / 20f
		val deltaSec = if (lastRenderTick == 0f) 0f else tickSec - lastRenderTick
		lastRenderTick = tickSec
		animTimeTracker += deltaSec

		// 1. 驱动所有控制器（控制器只操控自己的 proxyModel）
		tick(tickSec, deltaSec)

		// 2. 合并多控制器数据
		val proxyModels = collectProxyModels()
		if (proxyModels.isEmpty()) return
		val flags = resolveMergedFlags(defaultController.currentAnimTime)
		val weight = mergedWeight()

		// 3. 输出到渲染（修改外部模型）
		applyRootTransform(proxyModels, poseStack, flags, weight)
		applyProxyToModel(proxyModels, model as PlayerModel<Player>, flags, weight)
	}

	/** ItemInHandLayerMixin 调用：物品定位器 → PoseStack */
	fun applyItemTransform(isLeft: Boolean, poseStack: PoseStack) {
		if (!isClient) return
		val renderableControllers = getRenderableControllers()
		if (renderableControllers.isEmpty()) return
		val weight = defaultController.effectiveWeight
		val flags = resolveMergedFlags(defaultController.currentAnimTime)
		applyProxyToItem(
			renderableControllers.map { (it as BaseAnimationController).proxyModel },
			isLeft,
			poseStack,
			flags,
			weight,
		)
	}
}
