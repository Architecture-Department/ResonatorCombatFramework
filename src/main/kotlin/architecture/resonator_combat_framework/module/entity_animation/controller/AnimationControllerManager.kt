package architecture.resonator_combat_framework.module.entity_animation.controller

import architecture.goldenboughs_lib.api.AllOpe
import architecture.resonator_combat_framework.events.registry.AnimationControllers
import architecture.resonator_combat_framework.module.entity_animation.ProxyBone
import architecture.resonator_combat_framework.module.entity_animation.ProxyModel
import architecture.resonator_combat_framework.module.entity_animation.data.ProxyBoneFlags
import architecture.resonator_combat_framework.module.entity_animation.engine.BrAnimationParticle
import architecture.resonator_combat_framework.module.entity_animation.engine.BrAnimationSound
import architecture.resonator_combat_framework.module.entity_animation.engine.BrBone
import architecture.resonator_combat_framework.module.entity_animation.engine.BrModel
import architecture.resonator_combat_framework.module.entity_animation.mapper.IEntityAnimationMapper
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.Entity
import org.joml.Vector3d
import org.joml.Vector3f

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
	private val pendingEvents = mutableListOf<AnimationEventsToFire>()

	/** 几何骨骼定义（外部设置后自动同步到 bones） */
	var brModel: BrModel = BrModel("")
		set(value) {
			field = value
			bones = value.bones.associateBy { it.name }
		}

	/** 当前骨骼映射（骨骼名 -> BrBone，基础骨骼 + 所有控制器额外骨骼合并） */
	var bones: Map<String, BrBone> = emptyMap()

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
	fun tickAnimations() {
		interpCache = null
		prevMergedProxy.bones.clear()
		for ((name, bone) in mergedProxy.bones) {
			val copy = ProxyBone(name)
			copy.pos.set(bone.pos)
			copy.rotation.set(bone.rotation)
			copy.scale.set(bone.scale)
			prevMergedProxy.addBone(copy)
		}

		// 3. 运行动画控制器
		for (ctrl in ordered) {
			ctrl.tickAdvance()
		}
		remerge()
		firePendingEvents()
	}

	fun remerge() {
		interpCache = null
		mergedFlags.clear()
		// 保存当前 mergedProxy 快照（供淡出 blend），先于 clear()
		val fadeSnapshot = mergedProxy.toSnapshot()
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

				if (name in coveredBones && ctrl.isOverriding) {
					mb.localPos.set(0f)
					mb.localRot.set(0f)
					mb.localScale.set(1f, 1f, 1f)
					mb.setPosEmpty(true)
					mb.setRotEmpty(true)
					mb.setScaleEmpty(true)
				}

				if (bone.hasPos()) {
					mb.setPosEmpty(false)
					mb.localPos.add(Vector3f(bone.localPos).mul(weight))
				}

				if (bone.hasRot()) {
					mb.setRotEmpty(false)
					mb.localRot.add(Vector3f(bone.localRot).mul(weight))
				}

				if (bone.hasScale()) {
					mb.setScaleEmpty(false)
					mb.localScale.add(Vector3f(bone.localScale).sub(1f, 1f, 1f).mul(weight))
				}
			}
			coveredBones.addAll(bac.affectedBones)

			// 淡出时 blend 到低优先级控制器的快照
			if (ctrl.isFadingOut && weight < 1f) {
				for ((name, bone) in mergedProxy.bones) {
					val snapBone = fadeSnapshot.getBone(name) ?: continue
					if (bone.hasPos() && snapBone.hasPos()) {
						bone.localPos.set(snapBone.localPos).lerp(bone.localPos, weight)
					}
					if (bone.hasRot() && snapBone.hasRot()) {
						bone.localRot.set(snapBone.localRot).lerp(bone.localRot, weight)
					}
					if (bone.hasScale() && snapBone.hasScale()) {
						bone.localScale.set(snapBone.localScale).lerp(bone.localScale, weight)
					}
				}
			}
		}
	}

	private fun interpolateBone(name: String, partialTick: Float): ProxyBone? {
		val currBone = mergedProxy.getBone(name) ?: return null
		val prevBone = prevMergedProxy.getBone(name)
		val mb = ProxyBone(name)
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
				mb.setPosEmpty(false)
				mb.pos.set(currBone.pos)
			}
			if (currBone.hasRot()) {
				mb.setRotEmpty(false)
				mb.rotation.set(currBone.rotation)
			}
			if (currBone.hasScale()) {
				mb.setScaleEmpty(false)
				mb.scale.set(currBone.scale)
			}
		}
		return mb
	}

	fun getInterpolatedBone(name: String, partialTick: Float): ProxyBone? = interpolateBone(name, partialTick)

	/** 插值缓存，tickAnimations 时失效 */
	private var interpCache: ProxyModel? = null

	/** 上次缓存的 partialTick */
	private var cachedPartialTick: Float = -1f

	/** 获取合并后的插值代理骨骼（缓存，仅 partialTick 变化或源数据更新时重算） */
	fun getInterpolatedProxy(partialTick: Float): ProxyModel {
		var cached = interpCache
		if (cached == null || cachedPartialTick != partialTick) {
			cached = ProxyModel("interp")
			for ((name, _) in mergedProxy.bones) {
				interpolateBone(name, partialTick)?.let { cached.addBone(it) }
			}
			interpCache = cached
			cachedPartialTick = partialTick
		}
		return cached
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

	/** 从合并后的代理模型中解析骨骼在世界空间中的坐标 */
	private fun resolveBonePos(sound: BrAnimationSound): Vector3d {
		return resolveBonePos(sound.effects.firstOrNull()?.boneName ?: return Vector3d())
	}

	/** 从合并后的代理模型中解析骨骼在世界空间中的坐标（粒子版） */
	private fun resolveBonePos(particle: BrAnimationParticle): Vector3d {
		return resolveBonePos(particle.effects.firstOrNull()?.boneName ?: return Vector3d())
	}

	private fun resolveBonePos(boneName: String): Vector3d {
		val bone = mergedProxy.getBone(boneName)
		bone ?: return Vector3d()
		bone.getLocator(boneName)?.let { return Vector3d(it.pos.x.toDouble(), it.pos.y.toDouble(), it.pos.z.toDouble()) }
		return bone.let { Vector3d(it.pos.x.toDouble(), it.pos.y.toDouble(), it.pos.z.toDouble()) }
	}

	/** 按父→子顺序构建骨骼层次列表（拓扑排序，父骨骼先于子骨骼） */
	private fun buildHierarchyOrder(): List<String> {
		val result = mutableListOf<String>()
		val visited = mutableSetOf<String>()
		for (name in bones.keys) {
			addWithParents(name, result, visited)
		}
		return result
	}

	/** 递归添加父骨骼及其子骨骼到有序列表 */
	private fun addWithParents(name: String, result: MutableList<String>, visited: MutableSet<String>) {
		if (name in visited) return
		visited.add(name)
		val brBone = bones[name] ?: return
		if (brBone.parent != null && brBone.parent !in visited) {
			addWithParents(brBone.parent, result, visited)
		}
		result.add(name)
	}

	/** 合并所有控制器的额外骨骼到 [bones] */
	fun rebuildBones() {
		val merged = brModel.bones.associateBy { it.name }.toMutableMap()
		for (ctrl in ordered) {
			val controller = ctrl as? BedrockAnimationController ?: continue
			merged.putAll(controller.getExtraBones())
		}
		bones = merged
	}

	/**
	 * 计算骨骼层级继承变换。
	 * 遍历骨骼层级，从父到子累加 localPos/localRot/localScale 到 pos/rotation/scale。
	 * @param enableInheritance false 时不计算继承，pos/rotation/scale 直接等于 local 值
	 * @param noInheritNames 不继承父变换的骨骼名集合
	 */
	fun computeInheritedTransforms(enableInheritance: Boolean, noInheritNames: Set<String>) {
		val ordered = buildHierarchyOrder()
		for (boneName in ordered) {
			val pb = mergedProxy.getBone(boneName) ?: continue
			val brBone = bones[boneName] ?: continue
			val skip = !enableInheritance || boneName in noInheritNames || brBone.parent == null
			val parentPb = if (!skip) mergedProxy.getBone(brBone.parent) else null
			if (skip || parentPb == null) {
				pb.pos.set(pb.localPos)
				pb.rotation.set(pb.localRot)
				pb.scale.set(pb.localScale)
			} else {
				pb.pos.set(parentPb.pos).add(pb.localPos)
				pb.rotation.set(parentPb.rotation).add(pb.localRot)
				pb.scale.set(parentPb.scale).mul(pb.localScale)
			}
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
	fun queueEvents(events: AnimationEventsToFire) {
		pendingEvents.add(events)
	}

	/** 执行所有待触发的动画事件并清空队列 */
	fun firePendingEvents() {
		if (pendingEvents.isEmpty()) return
		val entity = mapper.holder
		val data = mapper.molangData

		// 时间线事件：双端执行
		for (events in pendingEvents) {
			events.timelines.forEach { it.apply(entity, data) }
		}

		// 音效/粒子事件：仅客户端执行，由映射器解析实际骨骼位置
		if (mapper.isClient) {
			for (events in pendingEvents) {
				events.sounds.forEach {
					mapper.playSoundEffect(it, resolveBonePos(it), data)
				}
				events.particles.forEach {
					mapper.playParticleEffect(it, resolveBonePos(it), data)
				}
			}
		}

		pendingEvents.clear()
	}
}