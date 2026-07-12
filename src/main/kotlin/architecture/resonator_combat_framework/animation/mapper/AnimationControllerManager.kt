package architecture.resonator_combat_framework.animation.mapper

import architecture.goldenboughs_lib.api.AllOpe
import architecture.resonator_combat_framework.animation.EventsToFire
import architecture.resonator_combat_framework.animation.IAnimationProvider
import architecture.resonator_combat_framework.animation.IAnimationProvider.Companion.getMapperProvider
import architecture.resonator_combat_framework.animation.ParticleStormAnimAdapter
import architecture.resonator_combat_framework.animation.controller.AnimationController
import architecture.resonator_combat_framework.animation.controller.IEntityAnimationController
import architecture.resonator_combat_framework.animation.data.BoneFlags
import architecture.resonator_combat_framework.init.registry.AnimationControllers
import architecture.resonator_combat_framework.model.BonePose
import architecture.resonator_combat_framework.model.DynamicGeometryModel
import architecture.resonator_combat_framework.model.GeometryModel
import architecture.resonator_combat_framework.model.PoseData
import architecture.resonator_combat_framework.util.RcfUtil
import architecture.resonator_combat_framework.util.RotationUtil
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.Entity
import org.joml.Matrix4fc
import org.joml.Vector3f
import org.mesdag.particlestorm.particle.MolangParticleEngine

/**
 * 动画控制器管理器，负责管理实体上所有动画控制器的生命周期、骨骼合并和事件调度。
 * 维护名称到控制器的映射（O(1)查找）和有序控制器列表（决定骨骼合并优先级）。
 * 提供骨骼变换合并、帧间插值、ParticleStorm 发射器追踪等功能。
 *
 * @param T 实体类型
 * @property holder 所属实体
 * @property mapperProvider 动画映射器提供者
 */
@AllOpe
class AnimationControllerManager<T : Entity>
@JvmOverloads
constructor(
	val holder: T,
	mapperProvider: IEntityAnimationMapperProvider<T, *>? = null
) {
	private val _mapperProvider: IEntityAnimationMapperProvider<T, *>? = mapperProvider

	@Suppress("UNCHECKED_CAST")
	val mapperProvider: IEntityAnimationMapperProvider<T, *>
		get() = _mapperProvider
			?: (holder as IAnimationProvider).getMapperProvider() as IEntityAnimationMapperProvider<T, *>

	/** 名称 → 控制器映射（O(1) 查找） */
	private val nameMap = mutableMapOf<ResourceLocation, IEntityAnimationController<T>>()

	/** 有序控制器列表（按添加顺序，决定骨骼合并优先级） */
	private val ordered = mutableListOf<IEntityAnimationController<T>>()

	/** 当前 tick 合并后的代理骨骼 */
	val mergedPose = PoseData("merged")

	/** 上一 tick 合并结果（用于渲染帧插值） */
	val prevMergedPose = PoseData("prevMerged")

	/** 合并后的骨骼标志 */
	val mergedBoneFlags = mutableMapOf<String, BoneFlags>()

	/** 待触发的事件队列（控制器收集 -> tick后统一执行） */
	private val pendingEvents = mutableListOf<Pair<IEntityAnimationController<*>, EventsToFire>>()

	/** 几何骨骼定义（外部设置后自动同步到 bones） */
	var geometry: GeometryModel = GeometryModel.EMPTY
		set(value) {
			field = value
			rebuildBones()
		}

	var brModel: DynamicGeometryModel = DynamicGeometryModel()

	/** 本 tick 的骨骼全局矩阵缓存（惰性填充，tick 切换时清空） */
	private val boneMatrixCache = mutableMapOf<String, Matrix4fc>()

	/** 发射器追踪："controllerId:locatorName" → ParticleStorm 发射器 ID */
	private val emitterTracker = mutableMapOf<String, Int>()

	/**
	 * 注册发射器追踪。
	 *
	 * @param controllerId 控制器 ID
	 * @param locatorName 定位器名称
	 * @param emitterId ParticleStorm 发射器 ID
	 */
	fun trackEmitter(controllerId: ResourceLocation, locatorName: String?, emitterId: Int) {
		val key = "${controllerId}:${locatorName ?: "@entity"}"
		emitterTracker[key] = emitterId
	}

	/**
	 * 清除指定控制器和定位器的发射器。
	 *
	 * @param controllerId 控制器 ID
	 * @param locatorName 定位器名称
	 */
	fun clearEmitter(controllerId: ResourceLocation, locatorName: String?) {
		if (!RcfUtil.PARTICLESTORM_LOADED) return
		val key = "${controllerId}:${locatorName ?: "@entity"}"
		val id = emitterTracker.remove(key) ?: return
		MolangParticleEngine.INSTANCE.removeEmitter(id, false)
	}

	/**
	 * 清除指定控制器的所有发射器（动画切换/结束时调用）。
	 *
	 * @param controllerId 控制器 ID
	 */
	fun clearEmittersFor(controllerId: ResourceLocation) {
		if (!RcfUtil.PARTICLESTORM_LOADED) return
		val prefix = "${controllerId}:"
		val toRemove = emitterTracker.filterKeys { it.startsWith(prefix) }.values.toList()
		emitterTracker.keys.removeAll { it.startsWith(prefix) }
		toRemove.forEach { MolangParticleEngine.INSTANCE.removeEmitter(it, false) }
	}

	/**
	 * 更新所有已追踪发射器的 parentSpace，实现骨骼跟随。每渲染帧调用。
	 *
	 * @param partialTick 渲染帧插值系数
	 */
	fun updateEmitterTransforms(partialTick: Float) {
		if (!RcfUtil.PARTICLESTORM_LOADED) return
		if (emitterTracker.isEmpty()) return
		val animData = getInterpolatedProxy(partialTick)
		for ((key, emitterId) in emitterTracker) {
			val locatorName = key.substringAfter(":").let { if (it == "@entity") null else it }
			ParticleStormAnimAdapter.updateEmitterTransform(emitterId, brModel, animData, locatorName)
		}
	}

	/**
	 * 追加控制器到末尾。
	 *
	 * @param name 控制器名称
	 * @param controller 控制器实例
	 */
	fun add(name: ResourceLocation, controller: IEntityAnimationController<T>) {
		nameMap[name] = controller
		ordered.add(controller)
	}

	/**
	 * 在指定索引插入控制器。
	 *
	 * @param index 插入位置
	 * @param name 控制器名称
	 * @param controller 控制器实例
	 */
	fun add(index: Int, name: ResourceLocation, controller: IEntityAnimationController<T>) {
		nameMap[name] = controller
		ordered.add(index, controller)
	}

	/**
	 * 在指定控制器之后插入新控制器。
	 *
	 * @param afterName 参考控制器名称
	 * @param name 新控制器名称
	 * @param controller 新控制器实例
	 */
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

	/**
	 * 在指定控制器之前插入新控制器。
	 *
	 * @param beforeName 参考控制器名称
	 * @param name 新控制器名称
	 * @param controller 新控制器实例
	 */
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

	/**
	 * 移除控制器并立即停止。
	 *
	 * @param name 控制器名称
	 */
	fun remove(name: ResourceLocation) {
		val ctrl = nameMap.remove(name) ?: return
		ctrl.stop(0f)
		ordered.remove(ctrl)
	}

	/**
	 * 推进所有控制器的动画时间并重新合并骨骼。
	 * 流程：清缓存 → 保存上一帧快照 → 逐控制器 tick → 合并骨骼 → tickAdvance → 触发事件。
	 */
	fun tick() {
		// 清空骨骼矩阵缓存（本 tick 所有数据重算）
		boneMatrixCache.clear()
		// 保存当前帧快照供渲染插值（prev -> current）
		prevMergedPose.bones.clear()
		for ((name, bone) in mergedPose.bones) {
			val copy = BonePose(name)
			copy.pos.set(bone.pos)
			copy.rotation.set(bone.rotation)
			copy.scale.set(bone.scale)
			copy.noInterp = bone.noInterp
			prevMergedPose.addBone(copy)
		}

		for (ctrl in ordered) {
			ctrl.tick()
		}
		mergeAll()
		for (ctrl in ordered) {
			ctrl.tickAdvance()
		}
		firePendingEvents()
	}

	/**
	 * 合并所有活跃控制器的骨骼变换。
	 * 按控制器顺序逐层叠加，支持权重混合、归一化旋转和骨骼覆盖。
	 */
	fun mergeAll() {
		mergedBoneFlags.clear()
		mergedPose.bones.clear()
		val coveredBones = mutableSetOf<String>()

		for (ctrl in ordered.filter { it.isActive() }) {
			val bac = ctrl as AnimationController
			val weight = ctrl.mergeWeight

			mergedBoneFlags.putAll(bac.activeBoneConfig.resolveBoneFlags(bac.currentAnimTime))
			for ((name, bone) in bac.poseData.bones) {
				val mb = mergedPose.getBone(name) ?: run {
					val nb = BonePose(name)
					mergedPose.addBone(nb)
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
					// 用四元数加权累加，避免欧拉角插值的万向锁和绕远路问题

					val normalized = Vector3f(bone.rotation)
					normalized.x = RotationUtil.normalizeDelta(normalized.x)
					normalized.y = RotationUtil.normalizeDelta(normalized.y)
					normalized.z = RotationUtil.normalizeDelta(normalized.z)
					mb.rotation.add(normalized.mul(weight))

				}

				if (bone.hasScale()) {
					mb.setScaleEmpty(false)
					mb.scale.add(Vector3f(bone.scale).sub(1f, 1f, 1f).mul(weight))
				}
			}
			coveredBones.addAll(bac.affectedBones)
		}
	}

	/**
	 * 获取骨骼的全局变换矩阵（惰性计算 + 缓存）。
	 * 同一 tick 内对同一骨骼的重复查询直接返回缓存。
	 *
	 * @param boneName 骨骼名称
	 * @param poseData 姿态数据
	 * @return 全局变换矩阵
	 */
	@JvmOverloads
	fun getTickBoneMatrix(boneName: String, poseData: PoseData = mergedPose): Matrix4fc {
		return boneMatrixCache.getOrPut(boneName) {
			brModel.computeBoneGlobalMatrix(boneName, poseData, true)
		}
	}

	/**
	 * 获取合并后的插值骨骼数据副本（逐帧在 prevMergedPose 和 mergedPose 之间线性插值）。
	 * partialTick=0 或 1 时直接返回对应源，避免不必要的插值计算。
	 *
	 * @param partialTick 渲染帧插值系数
	 * @return 插值后的姿态数据
	 */
	fun getInterpolatedProxy(partialTick: Float): PoseData {
		return PoseData.interpolate(prevMergedPose, mergedPose, partialTick)
	}

	/**
	 * 按名称获取控制器。
	 *
	 * @param name 控制器名称
	 * @return 控制器实例，不存在时返回 null
	 */
	fun get(name: ResourceLocation): IEntityAnimationController<T>? = nameMap[name]

	/**
	 * 获取主控制器。
	 *
	 * @return 主控制器实例
	 */
	fun getMainController(): IEntityAnimationController<T> = nameMap[AnimationControllers.MAIN]
		?: error("Main controller not initialized")

	/**
	 * 获取所有控制器（按添加顺序）。
	 *
	 * @return 控制器列表
	 */
	fun getAll(): List<IEntityAnimationController<T>> = ordered

	/**
	 * 是否存在指定控制器。
	 *
	 * @param name 控制器名称
	 * @return 是否存在
	 */
	fun has(name: ResourceLocation): Boolean = name in nameMap

	/**
	 * 是否有任意控制器活跃。
	 *
	 * @return 是否有活跃控制器
	 */
	fun isAnyActive(): Boolean = ordered.any { it.isActive() }

	/**
	 * 指定控制器是否活跃。
	 *
	 * @param name 控制器名称
	 * @return 是否活跃
	 */
	fun isActive(name: ResourceLocation): Boolean = nameMap[name]?.isActive() == true

	/**
	 * 获取所有活跃控制器。
	 *
	 * @return 活跃控制器列表
	 */
	fun getSortedActive(): List<IEntityAnimationController<T>> = ordered.filter { it.isActive() }

	/**
	 * 获取可渲染的控制器列表（用于 root 变换）。
	 * 从高到低遍历，isOverriding=true 的控制器若骨骼全被高优先级渲染过则跳过。
	 *
	 * @return 可渲染的控制器列表
	 */
	fun getRenderable(): List<IEntityAnimationController<T>> {
		val active = getSortedActive()
		val result = mutableListOf<IEntityAnimationController<T>>()
		val renderedBones = mutableSetOf<String>()
		for (ctrl in active) {
			val skip =
				ctrl.affectedBones.isNotEmpty() && ctrl.affectedBones.all { it in renderedBones } && !ctrl.isOverriding
			if (skip) continue
			result.add(ctrl)
			renderedBones.addAll(ctrl.affectedBones)
		}
		return result
	}

	/**
	 * 查找排在 controller 之前、isOverriding 且骨骼冲突的更高优先级控制器。
	 *
	 * @param controller 目标控制器
	 * @return 阻塞目标控制器的更高优先级控制器列表
	 */
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

	/**
	 * 合并所有控制器的额外骨骼到几何模型。
	 */
	fun rebuildBones() {
		brModel.set(geometry)
		for (ctrl in ordered) {
			val controller = ctrl as? AnimationController ?: continue
			if (controller.extraModel == null) continue
			brModel.overwriteAdd(controller.extraModel)
		}
	}

	/**
	 * 判断两个控制器的 affectedBones 是否有交集。
	 *
	 * @param a 控制器 A
	 * @param b 控制器 B
	 * @return 是否有骨骼冲突
	 */
	fun hasBoneConflict(a: IEntityAnimationController<T>, b: IEntityAnimationController<T>): Boolean {
		val aBones = a.affectedBones
		val bBones = b.affectedBones
		if (aBones.isEmpty() || bBones.isEmpty()) return false
		return aBones.intersect(bBones).isNotEmpty()
	}

	/**
	 * 添加待触发的事件到队列（由控制器在 tickBackend 中收集）。
	 *
	 * @param animationController 事件来源控制器
	 * @param events 待触发的事件
	 */
	fun queueEvents(animationController: IEntityAnimationController<*>, events: EventsToFire) {
		pendingEvents.add(animationController to events)
	}

	/**
	 * 执行所有待触发的动画事件并清空队列。
	 * 按类型分组执行：先时间线脚本，再声音，最后粒子。
	 */
	fun firePendingEvents() {
		if (pendingEvents.isEmpty()) return
		val entity = mapperProvider.holder
		val data = mapperProvider.molangData

		for ((controller, events) in pendingEvents) {
			try {
				events.timelines.forEach { it.run(entity, data) }
			} catch (e: Exception) {
				RcfUtil.LOGGER.error("[CONTROLLER] {} timeline effects failed: {}", controller.id, e.message)
			}
			try {
				events.sounds.forEach { it.runs(controller, entity, brModel, mergedPose, data) }
			} catch (e: Exception) {
				RcfUtil.LOGGER.error("[CONTROLLER] {} sound effects failed: {}", controller.id, e.message)
			}
			try {
				events.particles.forEach { it.runs(controller, entity, brModel, mergedPose, data) }
			} catch (e: Exception) {
				RcfUtil.LOGGER.error("[CONTROLLER] {} particle effects failed: {}", controller.id, e.message)
			}
		}
		pendingEvents.clear()
	}
}
