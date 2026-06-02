package architecture.resonator_combat_framework.module.entity_animation.mapper

import architecture.resonator_combat_framework.module.entity_animation.api.ProxyModel
import architecture.resonator_combat_framework.module.entity_animation.controller.BaseAnimationController
import architecture.resonator_combat_framework.module.entity_animation.data.ProxyBoneFlags
import architecture.resonator_combat_framework.module.entity_animation.event.AnimationControllerRegisterEvent
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.model.EntityModel
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
		proxyModel: ProxyModel,
		model: PlayerModel<Player>,
		flags: Map<String, ProxyBoneFlags>,
		weight: Float
	) {
		if (!isClient) return
		super.applyProxyToModel(proxyModel, model, flags, weight)
		applyProxyBone(proxyModel, "body", flags, weight, model.jacket)
		applyProxyBone(proxyModel, "left_arm", flags, weight, model.leftSleeve)
		applyProxyBone(proxyModel, "right_arm", flags, weight, model.rightSleeve)
		applyProxyBone(proxyModel, "left_leg", flags, weight, model.leftPants)
		applyProxyBone(proxyModel, "right_leg", flags, weight, model.rightPants)
	}

	override fun tickAndRender(model: EntityModel<*>, partialTick: Float, poseStack: PoseStack) {
		if (!isClient || !isActive()) return
		val tickSec = (player.tickCount + partialTick) / 20f
		val deltaSec = if (lastRenderTick == 0f) 0f else tickSec - lastRenderTick
		lastRenderTick = tickSec
		currentPartialTick = partialTick

		for (ctrl in animationControllerManager.getAll()) {
			(ctrl as BaseAnimationController).tickRender(partialTick, deltaSec)
		}
		animationControllerManager.remerge()
		val renderable = animationControllerManager.getRenderable()
		if (renderable.isEmpty()) return

		val firstBac = renderable.first() as BaseAnimationController
		val firstFlags = firstBac.resolveBoneFlags(firstBac.currentAnimTime)
		applyRootTransform(firstBac.getInterpolatedProxy(partialTick), poseStack, firstFlags, firstBac.effectiveWeight)

		applyProxyToModel(
			animationControllerManager.getInterpolatedProxy(partialTick),
			model as PlayerModel<Player>,
			animationControllerManager.mergedFlags,
			1f
		)
	}

	fun applyItemTransform(isLeft: Boolean, poseStack: PoseStack) {
		if (!isClient) return
		val renderable = getRenderableControllers()
		if (renderable.isEmpty()) return
		for (ctrl in renderable) {
			val bac = ctrl as BaseAnimationController
			val flags = bac.resolveBoneFlags(bac.currentAnimTime)
			applyProxyToItem(
				bac.getInterpolatedProxy(currentPartialTick), isLeft, poseStack, flags,
				ctrl.effectiveWeight
			)
		}
	}
}
