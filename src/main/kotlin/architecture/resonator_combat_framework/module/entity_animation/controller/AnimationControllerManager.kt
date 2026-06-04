package architecture.resonator_combat_framework.module.entity_animation.controller

import architecture.goldenboughs_lib.api.AllOpe
import architecture.resonator_combat_framework.events.registry.AnimationControllers
import architecture.resonator_combat_framework.module.entity_animation.api.IAnimationMapper
import architecture.resonator_combat_framework.module.entity_animation.api.ProxyBone
import architecture.resonator_combat_framework.module.entity_animation.api.ProxyModel
import architecture.resonator_combat_framework.module.entity_animation.data.ProxyBoneFlags
import net.minecraft.resources.ResourceLocation
import org.joml.Vector3f
import java.util.function.DoubleSupplier

/** 动画控制器管理器 */
@AllOpe
class AnimationControllerManager {
	private val nameMap = mutableMapOf<ResourceLocation, IAnimationController>()
	private val ordered = mutableListOf<IAnimationController>()

	/** 当前 tick 合并后的代理骨骼 */
	val mergedProxy = ProxyModel("merged")

	/** 上一 tick 合并结果（用于渲染帧插值） */
	val prevMergedProxy = ProxyModel("prevMerged")

	/** 合并后的骨骼标志 */
	val mergedFlags = mutableMapOf<String, ProxyBoneFlags>()

	/** 实体级 MoLang 变量作用域（tickAnimations 时自动压入/弹出） */
	val molangScope: MutableMap<String, DoubleSupplier> = HashMap()

	/** 追加控制器到末尾 */
	fun add(name: ResourceLocation, controller: IAnimationController) {
		nameMap[name] = controller
		ordered.add(controller)
	}

	/** 在指定索引插入控制器 */
	fun add(index: Int, name: ResourceLocation, controller: IAnimationController) {
		nameMap[name] = controller
		ordered.add(index, controller)
	}

	fun addAfter(afterName: ResourceLocation, name: ResourceLocation, controller: IAnimationController) {
		val after = nameMap[afterName]
		if (after != null) {
			val idx = ordered.indexOf(after)
			nameMap[name] = controller
			ordered.add(idx + 1, controller)
		} else {
			add(name, controller)
		}
	}

	fun addBefore(beforeName: ResourceLocation, name: ResourceLocation, controller: IAnimationController) {
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
	fun tickAnimations(mapper: IAnimationMapper) {
		interpCache = null
		prevMergedProxy.bones.clear()
		for ((name, bone) in mergedProxy.bones) {
			val copy = ProxyBone(name)
			copy.pos.set(bone.pos)
			copy.rotation.set(bone.rotation)
			copy.scale.set(bone.scale)
			prevMergedProxy.addBone(copy)
		}
		for (ctrl in ordered) {
			ctrl.tickAdvance(mapper)
		}
		remerge()
	}

	fun remerge() {
		interpCache = null
		mergedProxy.bones.clear()
		mergedFlags.clear()
		val coveredBones = mutableSetOf<String>()
		for (ctrl in ordered.filter { it.isActive() }) {
			val bac = ctrl as BedrockAnimationController
			val weight = ctrl.effectiveWeight

			// 保存当前 mergedProxy 快照（供淡出 blend）
			val snapshot = ProxyModel("snap")
			for ((sn, sb) in mergedProxy.bones) {
				val copy = ProxyBone(sn)
				copy.pos.set(sb.pos)
				copy.rotation.set(sb.rotation)
				copy.scale.set(sb.scale)
				if (sb.hasPos()) copy.setPosEmpty(false)
				if (sb.hasRot()) copy.setRotEmpty(false)
				if (sb.hasScale()) copy.setScaleEmpty(false)
				snapshot.addBone(copy)
			}

			mergedFlags.putAll(bac.resolveBoneFlags(bac.currentAnimTime))
			for ((name, bone) in bac.proxyModel.bones) {
				val mb = mergedProxy.getBone(name) ?: run {
					val nb = ProxyBone(name)
					mergedProxy.addBone(nb)
					nb
				}

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

			// 淡出时 blend 到低优先级控制器的快照
			if (ctrl.isFadingOut && weight < 1f) {
				for ((name, bone) in mergedProxy.bones) {
					val snapBone = snapshot.getBone(name)
					if (snapBone != null) {
						if (bone.hasPos() && snapBone.hasPos())
							bone.pos.set(snapBone.pos).lerp(bone.pos, weight)
						if (bone.hasRot() && snapBone.hasRot())
							bone.rotation.set(snapBone.rotation).lerp(bone.rotation, weight)
						if (bone.hasScale() && snapBone.hasScale())
							bone.scale.set(snapBone.scale).lerp(bone.scale, weight)
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
	fun get(name: ResourceLocation): IAnimationController? = nameMap[name]

	/** 获取主控制器 */
	fun getMainController(): IAnimationController = nameMap[AnimationControllers.MAIN]
		?: error("Main controller not initialized")

	/** 获取所有控制器（按添加顺序） */
	fun getAll(): List<IAnimationController> = ordered

	/** 是否存在指定控制器 */
	fun has(name: ResourceLocation): Boolean = name in nameMap

	/** 是否有任意控制器活跃 */
	fun isAnyActive(): Boolean = ordered.any { it.isActive() }

	/** 指定控制器是否活跃 */
	fun isActive(name: ResourceLocation): Boolean = nameMap[name]?.isActive() == true

	/** 获取所有活跃控制器 */
	fun getSortedActive(): List<IAnimationController> = ordered.filter { it.isActive() }

	/**
	 * 获取可渲染的控制器列表（用于 root 变换）。
	 * 从高到低遍历，isOverriding=true 的控制器若骨骼全被高优先级渲染过则跳过。
	 */
	fun getRenderable(): List<IAnimationController> {
		val active = getSortedActive()
		val result = mutableListOf<IAnimationController>()
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
	fun findBlocking(controller: IAnimationController): List<IAnimationController> {
		if (!controller.isActive()) return emptyList()
		val active = getSortedActive()
		val myIndex = active.indexOf(controller)
		if (myIndex < 0) return emptyList()
		val blocking = mutableListOf<IAnimationController>()
		for (i in 0 until myIndex) {
			val higher = active[i]
			if (higher.isOverriding && hasBoneConflict(higher, controller)) blocking.add(higher)
		}
		return blocking
	}

	/** 两控制器的 affectedBones 是否有交集 */
	private fun hasBoneConflict(a: IAnimationController, b: IAnimationController): Boolean {
		val aBones = a.affectedBones
		val bBones = b.affectedBones
		if (aBones.isEmpty() || bBones.isEmpty()) return false
		return aBones.intersect(bBones).isNotEmpty()
	}
}
