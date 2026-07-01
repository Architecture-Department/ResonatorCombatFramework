package architecture.resonator_combat_framework.module.entity_animation.animation.mapper

import architecture.resonator_combat_framework.core.RcfEventHooks
import architecture.resonator_combat_framework.module.entity_animation.animation.data.ProxyBoneFlags
import architecture.resonator_combat_framework.module.entity_animation.animation.model.GeometryData
import architecture.resonator_combat_framework.module.entity_animation.animation.model.PoseData
import architecture.resonator_combat_framework.module.entity_animation.registry.BedrockModelRegistry
import architecture.resonator_combat_framework.util.RcfUtil
import net.minecraft.client.model.PlayerModel
import net.minecraft.world.entity.player.Player

/** 玩家动画映射器——零外部依赖 + 渲染入口 */
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
			BedrockModelRegistry.getInstance(isClient).get(IDENTIFIER) ?: GeometryData.EMPTY
		RcfEventHooks.AnimationControllerRegister<Player>().getSortedEntries()
			.forEach { (controllerName, controllerFactory, priority) ->
				animationControllerManager.add(
					controllerName,
					controllerFactory(animationControllerManager, controllerName, isClient)
				)
			}
	}

	/** 附加层：外套/袖子/裤腿 */
	override fun applyProxyToModel(
		poseData: PoseData,
		model: PlayerModel<Player>,
		flags: Map<String, ProxyBoneFlags>
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
