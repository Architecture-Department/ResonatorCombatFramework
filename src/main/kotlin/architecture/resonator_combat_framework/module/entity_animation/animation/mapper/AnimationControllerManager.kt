package architecture.resonator_combat_framework.module.entity_animation.animation.mapper

import architecture.goldenboughs_lib.api.AllOpe
import architecture.resonator_combat_framework.events.registry.AnimationControllers
import architecture.resonator_combat_framework.module.entity_animation.animation.AnimationEventsToFire
import architecture.resonator_combat_framework.module.entity_animation.animation.ParticleStormAnimAdapter
import architecture.resonator_combat_framework.module.entity_animation.animation.controller.BedrockAnimationController
import architecture.resonator_combat_framework.module.entity_animation.animation.controller.IEntityAnimationController
import architecture.resonator_combat_framework.module.entity_animation.animation.data.ProxyBoneFlags
import architecture.resonator_combat_framework.module.entity_animation.animation.model.BakingBrModel
import architecture.resonator_combat_framework.module.entity_animation.animation.model.BrModel
import architecture.resonator_combat_framework.module.entity_animation.animation.model.ProxyBone
import architecture.resonator_combat_framework.module.entity_animation.animation.model.ProxyModel
import architecture.resonator_combat_framework.util.RcfUtil
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.Entity
import org.joml.Vector3f
import org.mesdag.particlestorm.particle.MolangParticleEngine

/** 动画控制器管理器 */
@AllOpe
class AnimationControllerManager<T : Entity>(val mapper: IEntityAnimationMapper<T, *>) {
	/** 名称 → 控制器映射（O(1) 查找） */
	private val nameMap = mutableMapOf<ResourceLocation, IEntityAnimationController<T>>()

	/** 有序控制器列表（按添加顺序，决定骨骼合并优先级） */
	private val ordered = mutableListOf<IEntityAnimationController<T>>()

	/** 当前 tick 合并后的代理骨骼 */
	val mergedProxy = ProxyModel("merged")

	/** 上一 tick 合并结果（用于渲染帧插值） */
	val prevMergedProxy = ProxyModel("prevMerged")

	/** 合并后的骨骼标志 */
	val mergedFlags = mutableMapOf<String, ProxyBoneFlags>()

	/** 待触发的事件队列（控制器收集 -> tick后统一执行） */
	private val pendingEvents = mutableListOf<Pair<IEntityAnimationController<*>, AnimationEventsToFire>>()

	/** 几何骨骼定义（外部设置后自动同步到 bones） */
	var bakingBrModel: BakingBrModel = BakingBrModel.EMPTY
		set(value) {
			field = value
			rebuildBones()
		}

	var brModel: BrModel = BrModel()

	/** 发射器追踪："controllerId:locatorName" → ParticleStorm 发射器 ID */
	private val emitterTracker = mutableMapOf<String, Int>()

	/** 注册发射器追踪 */
	fun trackEmitter(controllerId: ResourceLocation, locatorName: String?, emitterId: Int) {
		val key = "${controllerId}:${locatorName ?: "@entity"}"
		emitterTracker[key] = emitterId
	}

	/** 清除指定控制器+定位器的发射器 */
	fun clearEmitter(controllerId: ResourceLocation, locatorName: String?) {
		if (!RcfUtil.PARTICLESTORM_LOADED) return
		val key = "${controllerId}:${locatorName ?: "@entity"}"
		val id = emitterTracker.remove(key) ?: return
		MolangParticleEngine.INSTANCE.removeEmitter(id, false)
	}

	/** 清除指定控制器的所有发射器（动画切换/结束时调用） */
	fun clearEmittersFor(controllerId: ResourceLocation) {
		if (!RcfUtil.PARTICLESTORM_LOADED) return
		val prefix = "${controllerId}:"
		val toRemove = emitterTracker.filterKeys { it.startsWith(prefix) }.values.toList()
		emitterTracker.keys.removeAll { it.startsWith(prefix) }
		toRemove.forEach { MolangParticleEngine.INSTANCE.removeEmitter(it, false) }
	}

	/** 更新所有已追踪发射器的 parentSpace，实现骨骼跟随。每渲染帧调用 */
	fun updateEmitterTransforms(partialTick: Float) {
		if (!RcfUtil.PARTICLESTORM_LOADED) return
		if (emitterTracker.isEmpty()) return
		val animData = getInterpolatedProxy(partialTick)
		for ((key, emitterId) in emitterTracker) {
			val locatorName = key.substringAfter(":").let { if (it == "@entity") null else it }
			ParticleStormAnimAdapter.updateEmitterTransform(emitterId, brModel, animData, locatorName)
		}
	}

	/** 追加控制器到末尾 */
	fun add(name: ResourceLocation, controller: IEntityAnimationController<T>) {
		nameMap[name] = controller
		ordered.add(controller)
	}

	/** 在指定索引插入控制器 */
	fun add(index: Int, name: ResourceLocation, controller: IEntityAnimationController<T>) {
		nameMap[name] = controller
		ordered.add(index, controller)
	}

	fun addAfter(afterName: ResourceLocation, name: ResourceLocation, controller: IEntityAnimationController<T>) {
		val after = nameMap[afterName]
		if (after != null) {
			val idx = ordered.indexOf(after)
			nameMap[name] = controller
			ordered.add(idx + 1, controller)
		} else {
			add(name, controller)
		}
	}

	fun addBefore(beforeName: ResourceLocation, name: ResourceLocation, controller: IEntityAnimationController<T>) {
		val before = nameMap[beforeName]
		if (before != null) {
			val idx = ordered.indexOf(before)
			nameMap[name] = controller
			ordered.add(idx, controller)
		} else {
			add(name, controller)
		}
	}

	/** 移除控制器并立即停止 */
	fun remove(name: ResourceLocation) {
		val ctrl = nameMap.remove(name) ?: return
		ctrl.stop(0)
		ordered.remove(ctrl)
	}

	/** 推进所有控制器的动画时间并重新合并 */
	fun tickAnimations(partialTick: Float) {
		// 保存当前帧快照供渲染插值（prev -> current）
		prevMergedProxy.bones.clear()
		for ((name, bone) in mergedProxy.bones) {
			val copy = ProxyBone(name)
			copy.pos.set(bone.pos)
			copy.rotation.set(bone.rotation)
			copy.scale.set(bone.scale)
			copy.noInterp = bone.noInterp
			prevMergedProxy.addBone(copy)
		}

		for (ctrl in ordered) {
			ctrl.tickAdvance()
		}
		remerge()
		firePendingEvents(partialTick)
	}

	fun remerge() {
		mergedFlags.clear()
		mergedProxy.bones.clear()
		val coveredBones = mutableSetOf<String>()
		for (ctrl in ordered.filter { it.isActive() }) {
			val bac = ctrl as BedrockAnimationController
			val weight = ctrl.effectiveWeight

			mergedFlags.putAll(bac.resolveBoneFlags(bac.currentAnimTime))
			for ((name, bone) in bac.proxyModel.bones) {
				val mb = mergedProxy.getBone(name) ?: run {
					val nb = ProxyBone(name)
					mergedProxy.addBone(nb)
					nb
				}

				// 复制 noInterp 标记（STEP 关键帧骨骼在渲染时不插值）
				if (bone.noInterp) mb.noInterp = true

				if (name in coveredBones && ctrl.isOverriding) {
					mb.pos.set(0f)
					mb.rotation.set(0f)
					mb.scale.set(1f, 1f, 1f)
					mb.setPosEmpty(true)
					mb.setRotEmpty(true)
					mb.setScaleEmpty(true)
				}

				if (bone.hasPos()) {
					mb.setPosEmpty(false)
					mb.pos.add(Vector3f(bone.pos).mul(weight))
				}

				if (bone.hasRot()) {
					mb.setRotEmpty(false)
					mb.rotation.add(Vector3f(bone.rotation).mul(weight))
				}

				if (bone.hasScale()) {
					mb.setScaleEmpty(false)
					mb.scale.add(Vector3f(bone.scale).sub(1f, 1f, 1f).mul(weight))
				}
			}
			coveredBones.addAll(bac.affectedBones)

		}
	}

	private fun interpolateBone(name: String, partialTick: Float): ProxyBone? {
		val currBone = mergedProxy.getBone(name) ?: return null
		val prevBone = prevMergedProxy.getBone(name)
		val mb = ProxyBone(name)
		// STEP 关键帧的骨骼直接取当前值，不做插值
		if (currBone.noInterp) {
			if (currBone.hasPos()) {
				mb.setPosEmpty(false); mb.pos.set(currBone.pos)
			}
			if (currBone.hasRot()) {
				mb.setRotEmpty(false); mb.rotation.set(currBone.rotation)
			}
			if (currBone.hasScale()) {
				mb.setScaleEmpty(false); mb.scale.set(currBone.scale)
			}
			return mb
		}
		if (prevBone != null) {
			if (currBone.hasPos()) {
				mb.setPosEmpty(false)
				mb.pos.set(prevBone.pos).lerp(currBone.pos, partialTick)
			} else if (prevBone.hasPos()) {
				mb.setPosEmpty(false)
				mb.pos.set(prevBone.pos)
			}
			if (currBone.hasRot()) {
				mb.setRotEmpty(false)
				mb.rotation.set(prevBone.rotation).lerp(currBone.rotation, partialTick)
			} else if (prevBone.hasRot()) {
				mb.setRotEmpty(false)
				mb.rotation.set(prevBone.rotation)
			}
			if (currBone.hasScale()) {
				mb.setScaleEmpty(false)
				mb.scale.set(prevBone.scale).lerp(currBone.scale, partialTick)
			} else if (prevBone.hasScale()) {
				mb.setScaleEmpty(false)
				mb.scale.set(prevBone.scale)
			}
		} else {
			if (currBone.hasPos()) {
				mb.setPosEmpty(false); mb.pos.set(currBone.pos)
			}
			if (currBone.hasRot()) {
				mb.setRotEmpty(false); mb.rotation.set(currBone.rotation)
			}
			if (currBone.hasScale()) {
				mb.setScaleEmpty(false); mb.scale.set(currBone.scale)
			}
		}
		return mb
	}

	fun getInterpolatedBone(name: String, partialTick: Float): ProxyBone? = interpolateBone(name, partialTick)

	/** 获取合并后的插值代理骨骼（逐帧在 prevMergedProxy 和 mergedProxy 之间线性插值） */
	/** 复制 ProxyModel 的所有骨骼到新模型（深拷贝） */
	private fun copyProxyModel(source: ProxyModel): ProxyModel {
		val result = ProxyModel("interp")
		for ((name, bone) in source.bones) {
			val copy = ProxyBone(name)
			copy.pos.set(bone.pos)
			copy.rotation.set(bone.rotation)
			copy.scale.set(bone.scale)
			if (bone.hasPos()) copy.setPosEmpty(false)
			if (bone.hasRot()) copy.setRotEmpty(false)
			if (bone.hasScale()) copy.setScaleEmpty(false)
			copy.noInterp = bone.noInterp
			result.addBone(copy)
		}
		return result
	}

	/** 获取合并后的插值代理骨骼（逐帧在 prevMergedProxy 和 mergedProxy 之间线性插值） */
	fun getInterpolatedProxy(partialTick: Float): ProxyModel {
		// partialTick=0 或 1 时直接返回对应源，避免不必要的插值计算
		if (partialTick == 0f) return copyProxyModel(prevMergedProxy)
		if (partialTick == 1f) return copyProxyModel(mergedProxy)
		val result = ProxyModel("interp")
		for ((name, _) in mergedProxy.bones) {
			interpolateBone(name, partialTick)?.let { result.addBone(it) }
		}
		return result
	}

	/** 按名称获取控制器 */
	fun get(name: ResourceLocation): IEntityAnimationController<T>? = nameMap[name]

	/** 获取主控制器 */
	fun getMainController(): IEntityAnimationController<T> = nameMap[AnimationControllers.MAIN]
		?: error("Main controller not initialized")

	/** 获取所有控制器（按添加顺序） */
	fun getAll(): List<IEntityAnimationController<T>> = ordered

	/** 是否存在指定控制器 */
	fun has(name: ResourceLocation): Boolean = name in nameMap

	/** 是否有任意控制器活跃 */
	fun isAnyActive(): Boolean = ordered.any { it.isActive() }

	/** 指定控制器是否活跃 */
	fun isActive(name: ResourceLocation): Boolean = nameMap[name]?.isActive() == true

	/** 获取所有活跃控制器 */
	fun getSortedActive(): List<IEntityAnimationController<T>> = ordered.filter { it.isActive() }

	/**
	 * 获取可渲染的控制器列表（用于 root 变换）。
	 * 从高到低遍历，isOverriding=true 的控制器若骨骼全被高优先级渲染过则跳过。
	 */
	fun getRenderable(): List<IEntityAnimationController<T>> {
		val active = getSortedActive()
		val result = mutableListOf<IEntityAnimationController<T>>()
		val renderedBones = mutableSetOf<String>()
		for (ctrl in active) {
			if (ctrl.affectedBones.isNotEmpty() && ctrl.affectedBones.all { it in renderedBones } && !ctrl.isOverriding) {
				continue
			}
			result.add(ctrl)
			renderedBones.addAll(ctrl.affectedBones)
		}
		return result
	}

	/** 查找排在 controller 之前、isOverriding 且骨骼冲突的更高优先级控制器 */
	fun findBlocking(controller: IEntityAnimationController<T>): List<IEntityAnimationController<T>> {
		if (!controller.isActive()) return emptyList()
		val active = getSortedActive()
		val myIndex = active.indexOf(controller)
		if (myIndex < 0) return emptyList()
		val blocking = mutableListOf<IEntityAnimationController<T>>()
		for (i in 0 until myIndex) {
			val higher = active[i]
			if (higher.isOverriding && hasBoneConflict(higher, controller)) blocking.add(higher)
		}
		return blocking
	}

	/** 合并所有控制器的额外骨骼到 [bones] */
	fun rebuildBones() {
		brModel.set(bakingBrModel)
		for (ctrl in ordered) {
			val controller = ctrl as? BedrockAnimationController ?: continue
			if (controller.extraModel == null) continue
			brModel.overwriteAdd(controller.extraModel)
		}
	}

	/** 两控制器的 affectedBones 是否有交集 */
	private fun hasBoneConflict(a: IEntityAnimationController<T>, b: IEntityAnimationController<T>): Boolean {
		val aBones = a.affectedBones
		val bBones = b.affectedBones
		if (aBones.isEmpty() || bBones.isEmpty()) return false
		return aBones.intersect(bBones).isNotEmpty()
	}

	/** 添加待触发的事件到队列（由控制器在 tickBackend 中收集） */
	fun queueEvents(animationController: IEntityAnimationController<*>, events: AnimationEventsToFire) {
		pendingEvents.add(animationController to events)
	}

	/** 执行所有待触发的动画事件并清空队列 */
	fun firePendingEvents(partialTick: Float) {
		if (pendingEvents.isEmpty()) return
		val entity = mapper.holder
		val data = mapper.molangData

		// 时间线事件：双端执行
		for ((controller, events) in pendingEvents) {
			try {
				events.timelines.forEach { it.run(entity, data) }
			} catch (e: Exception) {
				RcfUtil.LOGGER.error("[CONTROLLER] {} timeline effects failed: {}", controller.id, e.message)
			}
			try {
				events.sounds.forEach {
					it.runs(controller, entity, brModel, mergedProxy, data, partialTick)
				}
			} catch (e: Exception) {
				RcfUtil.LOGGER.error("[CONTROLLER] {} sound effects failed: {}", controller.id, e.message)
			}
			try {
				events.particles.forEach {
					it.runs(controller, entity, brModel, mergedProxy, data, partialTick)
				}
			} catch (e: Exception) {
				RcfUtil.LOGGER.error("[CONTROLLER] {} particle effects failed: {}", controller.id, e.message)
			}
		}

		pendingEvents.clear()
	}
}
