package architecture.resonator_combat_framework.module.player_animation

import architecture.resonator_combat_framework.event.PlayerControllerEvent
import net.minecraft.client.Minecraft
import net.minecraft.client.model.PlayerModel
import net.minecraft.client.model.geom.ModelPart
import net.minecraft.client.renderer.entity.LivingEntityRenderer
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.player.Player
import net.neoforged.neoforge.common.NeoForge
import software.bernie.geckolib.animatable.GeoAnimatable
import software.bernie.geckolib.animatable.SingletonGeoAnimatable
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache
import software.bernie.geckolib.animation.AnimatableManager
import software.bernie.geckolib.animation.AnimationController
import software.bernie.geckolib.cache.`object`.BakedGeoModel
import software.bernie.geckolib.cache.`object`.GeoBone
import software.bernie.geckolib.renderer.GeoObjectRenderer
import software.bernie.geckolib.util.GeckoLibUtil
import java.util.function.BiConsumer

class GeoPlayer(
	val player: Player
) : SingletonGeoAnimatable {

	@JvmField
	val model: GeoPlayerModel = GeoPlayerModel(player)

	@JvmField
	val renderer: GeoObjectRenderer<GeoPlayer> = GeoObjectRenderer(model)

	@JvmField
	val cache: AnimatableInstanceCache = GeckoLibUtil.createInstanceCache(this)

	init {
		SingletonGeoAnimatable.registerSyncedAnimatable(this)
	}

	private var activated = false

	override fun registerControllers(controllers: AnimatableManager.ControllerRegistrar) {
		repeat(NeoForge.EVENT_BUS.post(PlayerControllerEvent.Register(player)).getAll().values.size) {
			controllers.add(it as AnimationController<*>)
		}
//		controllers.add(new ProxiedPlayerAnimationController(this, TEST,
//			h -> PlayState.STOP,
//			p -> p.getOffhandItem().is(Items.CUCUMBER.get()))
//			.triggerableAnim(TEST, ANIM_TEST));
//		controllers.add(new ProxiedPlayerAnimationController(this, OTSUCHI_HOLD,
//			h -> PlayState.STOP,
//			p -> p.getMainHandItem().getItem() instanceof Otsuchi)
//			.triggerableAnim(OTSUCHI_HOLD, ANIM_OTSUCHI_HOLD));
//		controllers.add(new ProxiedPlayerAnimationController(this, OTSUCHI_SMASH,
//			h -> PlayState.STOP,
//			p -> p.getMainHandItem().getItem() instanceof Otsuchi)
//			.triggerableAnim(OTSUCHI_SMASH, ANIM_OTSUCHI_SMASH));
	}

	override fun getAnimatableInstanceCache(): AnimatableInstanceCache {
		return cache
	}

	override fun getTick(`object`: Any): Double {
		return if (`object` is Player) `object`.tickCount.toDouble() else 0.0
	}

	/**
	 * Actually triggers player animation.
	 * @param modelConsumer play your animation via lambda here.
	 */
	fun startProxy(modelConsumer: BiConsumer<Player, GeoPlayerModel>) {
		this.activated = true
		modelConsumer.accept(this.player, this.model)
	}

	fun endProxy() {
		this.activated = false
	}

	// 客户端方法
	fun proxy() {
		if (!activated) return
		if (!checkAnimationStat()) {
			endProxy()
			return
		}

		if (player.level() is ServerLevel) return

		val playerModel = (Minecraft.getInstance().entityRenderDispatcher
			.getRenderer(player) as LivingEntityRenderer<*, *>).getModel() as PlayerModel<*>
		val bakedGeoModel: BakedGeoModel = model.getBakedModel(model.getModelResource(this))
		bakedGeoModel.getBone("head").ifPresent {
			syncBones(it, playerModel.head)
			syncBones(it, playerModel.hat)
		}
		bakedGeoModel.getBone("body").ifPresent {
			syncBones(it, playerModel.body)
			syncBones(it, playerModel.jacket)
		}
		bakedGeoModel.getBone("left_arm").ifPresent {
			syncBones(it, playerModel.leftArm)
			syncBones(it, playerModel.leftSleeve)
		}
		bakedGeoModel.getBone("right_arm").ifPresent {
			syncBones(it, playerModel.rightArm)
			syncBones(it, playerModel.rightSleeve)
		}
		bakedGeoModel.getBone("left_leg").ifPresent {
			syncBones(it, playerModel.leftLeg)
			syncBones(it, playerModel.leftPants)
		}
		bakedGeoModel.getBone("right_leg").ifPresent {
			syncBones(it, playerModel.rightLeg)
			syncBones(it, playerModel.rightPants)
		}
		bakedGeoModel.getBone("cloak").ifPresent {
			syncBones(it, playerModel.cloak)
		}
		bakedGeoModel.getBone("jacket").ifPresent {
			syncBones(it, playerModel.cloak)
		}
	}

	private fun checkAnimationStat(): Boolean {
		var flag = false
		for (controller in animatableInstanceCache
			.getManagerForId<GeoAnimatable>(this.hashCode().toLong())
			.getAnimationControllers().values
		) {
			if (controller !is GeoPlayerModel.ProxiedPlayerAnimationController) continue
			if (controller.check(player)) flag = true
			else controller.outro(this)
		}
		return flag
	}

	// 客户端方法
	private fun syncBones(bone: GeoBone, part: ModelPart) {
		part.x += bone.posX
		part.y -= bone.posY
		part.z += bone.posZ
		part.xRot += (bone.rotX)
		part.yRot += (bone.rotY)
		part.zRot += (bone.rotZ)
		part.xScale *= bone.scaleX
		part.yScale *= bone.scaleY
		part.zScale *= bone.scaleZ
	}
}
