package architecture.resonator_combat_framework.module.player_animation.mapper

import architecture.resonator_combat_framework.module.player_animation.api.IAnimationMapper
import architecture.resonator_combat_framework.module.player_animation.api.ProxyModel
import architecture.resonator_combat_framework.module.player_animation.config.ProxyBoneConfigData
import architecture.resonator_combat_framework.module.player_animation.config.ProxyBoneConfigLoader
import architecture.resonator_combat_framework.module.player_animation.config.ProxyBoneFlags
import architecture.resonator_combat_framework.module.player_animation.controller.IAnimationController
import net.minecraft.client.model.EntityModel
import net.minecraft.world.entity.Entity

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

	fun resolveConfig(animId: String): ProxyBoneConfigData = configLoader.getConfig(animId)

	// 触发

	override fun trigger(animId: String) = trigger(resolveController(animId), animId)

	override fun trigger(controllerName: String, animId: String) {
		val config = resolveConfig(animId)
		// 清除旧配置防止 lock/unlock 标志泄漏
		boneConfigs.clear()
		boneConfigs[animId] = config
		(controllers[controllerName] ?: defaultController).trigger(animId, config.transitionTicks)
	}

	// 停止

	override fun stop(controllerName: String) {
		(controllers[controllerName] ?: defaultController).stop()
	}

	override fun stopImmediate(controllerName: String) {
		(controllers[controllerName] ?: defaultController).stopImmediate()
	}

	override fun stopAll() {
		for (ctrl in controllers.values) ctrl.stop()
	}

	override fun stopAllImmediate() {
		for (ctrl in controllers.values) ctrl.stopImmediate()
	}

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

	override fun getActiveControllersSorted(): List<IAnimationController> {
		return controllers.values
			.filter { it.isActive() }
			.sortedByDescending { it.priority }
	}

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
