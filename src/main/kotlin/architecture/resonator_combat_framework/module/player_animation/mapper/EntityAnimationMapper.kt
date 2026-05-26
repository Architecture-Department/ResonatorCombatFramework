package architecture.resonator_combat_framework.module.player_animation.mapper

import architecture.resonator_combat_framework.module.player_animation.api.IAnimationMapper
import architecture.resonator_combat_framework.module.player_animation.api.ProxyModel
import architecture.resonator_combat_framework.module.player_animation.config.AnimationPlayConfig
import architecture.resonator_combat_framework.module.player_animation.config.ProxyBoneConfigData
import architecture.resonator_combat_framework.module.player_animation.config.ProxyBoneConfigLoader
import architecture.resonator_combat_framework.module.player_animation.config.ProxyBoneFlags
import architecture.resonator_combat_framework.module.player_animation.controller.BaseAnimationController
import architecture.resonator_combat_framework.module.player_animation.controller.IAnimationController
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.model.EntityModel
import net.minecraft.world.entity.Entity
import org.joml.Quaternionf

/** 实体动画映射器：控制器、生命周期、优先级、骨骼冲突 */
abstract class EntityAnimationMapper<T : Entity, M : EntityModel<T>>(
	val entity: T,
	protected val isClient: Boolean = entity.level().isClientSide
) : IAnimationMapper {

	override val animControllerMap = mutableMapOf<String, String>()
	protected val configLoader = ProxyBoneConfigLoader.getInstance(isClient)
	protected val boneConfigs = mutableMapOf<String, ProxyBoneConfigData>()

	abstract val controllers: LinkedHashMap<String, IAnimationController>
	abstract val defaultController: IAnimationController

	abstract fun applyProxyToModel(
		proxyModels: List<ProxyModel>,
		model: M,
		flags: Map<String, ProxyBoneFlags>,
		weight: Float
	)

	/**
	 * 读取 "root" 骨骼变换，应用到 PoseStack，影响整个模型渲染。
	 * root.pos → translate, root.rotation → rotate, root.scale → scale。
	 * 全部值乘以 weight，与动画过渡同步。
	 */
	@Suppress("DuplicatedCode")
	fun applyRootTransform(proxyModels: List<ProxyModel>, poseStack: PoseStack, weight: Float) {
		if (!isClient) return
		if (weight <= 0f) return
		for (proxy in proxyModels) {
			val root = proxy.getBone("root") ?: continue
			val px = root.pos.x * weight
			val py = root.pos.y * weight
			val pz = root.pos.z * weight
			val rx = root.rotation.x * weight
			val ry = root.rotation.y * weight
			val rz = root.rotation.z * weight
			val sx = 1f + (root.scale.x - 1f) * weight
			val sy = 1f + (root.scale.y - 1f) * weight
			val sz = 1f + (root.scale.z - 1f) * weight
			if (px != 0f || py != 0f || pz != 0f) poseStack.translate(px.toDouble(), py.toDouble(), pz.toDouble())
			if (rz != 0f || ry != 0f || rx != 0f) poseStack.mulPose(Quaternionf().rotationZYX(rz, ry, rx))
			if (sx != 1f || sy != 1f || sz != 1f) poseStack.scale(sx, sy, sz)
			return
		}
	}

	fun resolveConfig(animId: String): ProxyBoneConfigData = configLoader.getConfig(animId)

	// tick

	/** 驱动所有控制器（控制器内部用状态机决定是否处理） */
	fun tick(gameTime: Float, deltaSec: Float) {
		for (ctrl in controllers()) {
			ctrl.tick(gameTime, deltaSec)
		}
	}

	/** 收集可渲染控制器的代理模型 */
	fun collectProxyModels(): List<ProxyModel> {
		return getRenderableControllers().map { (it as BaseAnimationController).proxyModel }
	}

	/** 合并所有活跃动画的骨骼标志 */
	fun resolveMergedFlags(animTime: Float): Map<String, ProxyBoneFlags> {
		val flags = mutableMapOf<String, ProxyBoneFlags>()
		for ((_, config) in boneConfigs) flags.putAll(config.resolveBoneFlags(animTime))
		return flags
	}

	/** 合并后的混合权重 */
	fun mergedWeight(): Float = defaultController.effectiveWeight

	// 触发

	override fun trigger(config: AnimationPlayConfig) {
		val controller = controllers[config.controllerName] ?: defaultController
		val loaded = configLoader.getConfig(config.animId)
		val used = config.boneConfig ?: loaded
		boneConfigs.clear()
		boneConfigs[config.animId] = used
		// 传给控制器，避免重复加载
		(controller as BaseAnimationController).resolvedBoneConfig = used
		controller.trigger(config)
	}

	override fun trigger(animId: String) = trigger(AnimationPlayConfig.of(animId))

	override fun trigger(controllerName: String, animId: String) {
		val cfg = resolveConfig(animId)
		val ctrl = controllers[controllerName] ?: defaultController
		val usedCfg = AnimationPlayConfig.of(animId).copy(
			controllerName = controllerName,
			fadeInTicks = cfg.transitionTicks,
			speedMultiplier = ctrl.speedMultiplier
		)
		boneConfigs.clear()
		boneConfigs[animId] = cfg
		ctrl.trigger(usedCfg)
	}

	// 停止

	override fun stop(controllerName: String) {
		(controllers[controllerName] ?: defaultController).stop()
	}

	override fun stopImmediate(controllerName: String) {
		(controllers[controllerName] ?: defaultController).stopImmediate()
	}

	override fun stopAll() = controllers().forEach { it.stop() }

	override fun stopAllImmediate() = controllers().forEach { it.stopImmediate() }

	override fun pause(controllerName: String) {
		(controllers[controllerName] ?: defaultController).pause()
	}

	override fun resume(controllerName: String) {
		(controllers[controllerName] ?: defaultController).resume()
	}

	override fun stopAnimation(animId: String) {
		boneConfigs.remove(animId)
		defaultController.stopAnimation(animId)
	}

	override fun stopAnimation(controllerName: String, animId: String) {
		boneConfigs.remove(animId)
		(controllers[controllerName] ?: defaultController).stopAnimation(animId)
	}

	// 查询

	override fun isActive(): Boolean = controllers.values.any { it.isActive() }

	override fun isControllerActive(): Boolean = defaultController.isActive()
	override fun isControllerActive(controllerName: String): Boolean =
		(controllers[controllerName] ?: defaultController).isActive()

	override fun getController(): IAnimationController = defaultController
	override fun getController(controllerName: String): IAnimationController =
		controllers[controllerName] ?: defaultController

	override fun hasController(): Boolean = true
	override fun controllers() = controllers.values
	override fun hasController(controllerName: String): Boolean = controllerName in controllers

	// 控制器管理

	override fun addController(name: String, controller: IAnimationController) {
		if (name == IAnimationMapper.DEFAULT_CONTROLLER_NAME) return
		controllers[name] = controller
	}

	override fun removeController(name: String) {
		if (name == IAnimationMapper.DEFAULT_CONTROLLER_NAME) return
		controllers.remove(name)?.stop()
	}

	// 优先级 + 骨骼冲突

	override fun getActiveControllersSorted(): List<IAnimationController> =
		controllers().filter { it.isActive() }.sortedByDescending { it.priority }

	override fun findBlockingControllers(controller: IAnimationController): List<IAnimationController> {
		if (!controller.isActive()) return emptyList()
		val sorted = getActiveControllersSorted()
		val myIndex = sorted.indexOf(controller)
		if (myIndex < 0) return emptyList()

		val blocking = mutableListOf<IAnimationController>()
		// 更高优先级
		for (i in 0 until myIndex) {
			val higher = sorted[i]
			if (higher.isOverriding && hasBoneConflict(higher, controller)) {
				blocking.add(higher)
			}
		}
		return blocking
	}

	/** 返回可渲染控制器，按优先级降序，过滤骨骼冲突阻塞的 */
	fun getRenderableControllers(): List<IAnimationController> {
		val sorted = getActiveControllersSorted()
		if (sorted.isEmpty()) return sorted

		val result = mutableListOf(sorted.first())
		val blockedBones = mutableSetOf<String>()

		// 累积被阻塞的骨骼
		if (sorted.first().isOverriding) {
			blockedBones.addAll(sorted.first().affectedBones)
		}

		for (i in 1 until sorted.size) {
			val ctrl = sorted[i]
			// 检查与高优先级覆盖控制器的骨骼冲突
			val ctrlBones = ctrl.affectedBones
			if (ctrlBones.isNotEmpty() && ctrlBones.any { it in blockedBones }) {
				// 被阻塞，跳过
				continue
			}
			result.add(ctrl)
			if (ctrl.isOverriding) {
				blockedBones.addAll(ctrlBones)
			}
		}
		return result
	}
}
