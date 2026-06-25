package architecture.resonator_combat_framework.module.entity_animation.animation.mapper

import architecture.resonator_combat_framework.module.entity_animation.animation.data.ProxyBoneFlags
import architecture.resonator_combat_framework.module.entity_animation.animation.model.ProxyModel
import architecture.resonator_combat_framework.module.entity_animation.event.AnimationControllerRegisterEvent
import net.minecraft.client.model.PlayerModel
import net.minecraft.world.entity.player.Player
import net.neoforged.neoforge.common.NeoForge

/** 玩家动画映射器——零外部依赖 + 渲染入口 */
class PlayerAnimationMapperProvider
@JvmOverloads
constructor(
	val player: Player,
	isClient: Boolean = player.level().isClientSide,
	animationControllerManager: AnimationControllerManager<Player> = AnimationControllerManager(player)
) : HumanoidEntityAnimationMapperProvider<Player, PlayerModel<Player>>(player, isClient, animationControllerManager) {

	init {
		NeoForge.EVENT_BUS.post(AnimationControllerRegisterEvent<Player>()).getSortedEntries()
			.forEach { (controllerName, controllerFactory, priority) ->
				animationControllerManager.add(
					controllerName,
					controllerFactory(animationControllerManager, controllerName, isClient)
				)
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
}
