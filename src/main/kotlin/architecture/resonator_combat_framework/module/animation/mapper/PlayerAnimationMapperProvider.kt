package architecture.resonator_combat_framework.module.animation.mapper

import architecture.resonator_combat_framework.core.RcfEventHooks
import architecture.resonator_combat_framework.module.animation.data.BoneFlags
import architecture.resonator_combat_framework.module.animation.model.GeometryData
import architecture.resonator_combat_framework.module.animation.model.PoseData
import architecture.resonator_combat_framework.module.animation.registry.GeometryModelRegistry
import architecture.resonator_combat_framework.util.RcfUtil
import net.minecraft.client.model.PlayerModel
import net.minecraft.world.entity.player.Player

/**
 * 玩家动画映射器，零外部依赖，提供玩家的动画渲染入口。
 * 继承 [HumanoidEntityAnimationMapperProvider]，额外处理玩家模型的附加层（外套、袖子、裤腿）。
 * 初始化时从注册表加载玩家几何模型并注册所有动画控制器。
 */
class PlayerAnimationMapperProvider(
	val player: Player,
	isClient: Boolean = player.level().isClientSide,
	animationControllerManager: AnimationControllerManager<Player>
) : HumanoidEntityAnimationMapperProvider<Player, PlayerModel<Player>>(player, isClient, animationControllerManager) {
	companion object {
		@JvmField
		val IDENTIFIER = RcfUtil.modRl("player/proxy")
	}

	constructor(holder: Player) : this(holder, holder.level().isClientSide, AnimationControllerManager(holder))

	init {
		animationControllerManager.geometry =
			GeometryModelRegistry.getInstance(isClient).get(IDENTIFIER) ?: GeometryData.EMPTY
		RcfEventHooks.AnimationControllerRegister<Player>().getSortedEntries()
			.forEach { (controllerName, controllerFactory, priority) ->
				animationControllerManager.add(
					controllerName,
					controllerFactory(animationControllerManager, controllerName, isClient)
				)
			}
	}

	/**
	 * 将代理骨骼数据映射到 PlayerModel，包含附加层（外套/袖子/裤腿）。
	 *
	 * @param poseData 代理骨骼姿态数据
	 * @param model 目标 PlayerModel
	 * @param flags 骨骼标志映射
	 */
	override fun applyProxyToModel(
		poseData: PoseData,
		model: PlayerModel<Player>,
		flags: Map<String, BoneFlags>
	) {
		if (!isClient) return
		super.applyProxyToModel(poseData, model, flags)
		applyProxyBone(poseData, "body", flags, model.jacket)
		applyProxyBone(poseData, "left_arm", flags, model.leftSleeve)
		applyProxyBone(poseData, "right_arm", flags, model.rightSleeve)
		applyProxyBone(poseData, "left_leg", flags, model.leftPants)
		applyProxyBone(poseData, "right_leg", flags, model.rightPants)
	}
}
