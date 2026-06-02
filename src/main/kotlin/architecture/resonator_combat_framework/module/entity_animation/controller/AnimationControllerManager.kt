package architecture.resonator_combat_framework.module.entity_animation.controller

import architecture.goldenboughs_lib.api.AllOpe
import architecture.goldenboughs_lib.util.lerp
import architecture.resonator_combat_framework.events.registry.AnimationControllerRegistry
import architecture.resonator_combat_framework.module.entity_animation.api.IAnimationMapper
import architecture.resonator_combat_framework.module.entity_animation.api.ProxyBone
import architecture.resonator_combat_framework.module.entity_animation.api.ProxyModel
import architecture.resonator_combat_framework.module.entity_animation.data.ProxyBoneFlags
import net.minecraft.resources.ResourceLocation
import org.joml.Vector3f

/** 动画控制器管理器 */
@AllOpe
class AnimationControllerManager {
	private val nameMap = mutableMapOf<ResourceLocation, IAnimationController>()
	private val ordered = mutableListOf<IAnimationController>()

	/** 当前 tick 合并后的代理骨骼数据 */
	val mergedProxy = ProxyModel("merged")

	/** 上一 tick 合并后的代理骨骼数据（用于渲染帧插值） */
	val prevMergedProxy = ProxyModel("prevMerged")

	/** 合并后的骨骼标志 */
	val mergedFlags = mutableMapOf<String, ProxyBoneFlags>()

	fun add(name: ResourceLocation, controller: IAnimationController) {
		nameMap[name] = controller
		ordered.add(controller)
	}

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

	fun remove(name: ResourceLocation) {
		val ctrl = nameMap.remove(name) ?: return
		ctrl.stop(0)
		ordered.remove(ctrl)
	}

	fun tickAnimations(mapper: IAnimationMapper) {
		// 保存上一帧合并结果用于插值
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

		// 合并当前所有可渲染控制器的骨骼数据
		remerge()
	}

	/** 重新合并所有可渲染控制器的骨骼数据（tickAnimations 和 crossfade 后均调用） */
	fun remerge() {
		interpCache = null
		mergedProxy.bones.clear()
		mergedFlags.clear()
		for (ctrl in getRenderable()) {
			val bac = ctrl as BaseAnimationController
			val weight = ctrl.effectiveWeight
			mergedFlags.putAll(bac.resolveBoneFlags(bac.currentAnimTime))
			for ((name, bone) in bac.proxyModel.bones) {
//				if (name == "root") continue
				val mb = mergedProxy.getBone(name) ?: run {
					val nb = ProxyBone(name)
					mergedProxy.addBone(nb)
					nb
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
		}
	}

	/** 骨骼插值：prev → curr 按 partialTick lerp */
	private fun interpolateBone(name: String, partialTick: Float): ProxyBone? {
		val currBone = mergedProxy.getBone(name) ?: return null
		val prevBone = prevMergedProxy.getBone(name)
		val mb = ProxyBone(name)
		if (prevBone != null) {
			if (currBone.hasPos()) {
				mb.setPosEmpty(false)
				mb.pos.set(lerp(prevBone.pos, currBone.pos, partialTick))
			} else if (prevBone.hasPos()) {
				mb.setPosEmpty(false)
				mb.pos.set(prevBone.pos)
			}
			if (currBone.hasRot()) {
				mb.setRotEmpty(false)
				mb.rotation.set(lerp(prevBone.rotation, currBone.rotation, partialTick))
			} else if (prevBone.hasRot()) {
				mb.setRotEmpty(false)
				mb.rotation.set(prevBone.rotation)
			}
			if (currBone.hasScale()) {
				mb.setScaleEmpty(false)
				mb.scale.set(lerp(prevBone.scale, currBone.scale, partialTick))
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

	fun getInterEmptypolatedBone(name: String, partialTick: Float): ProxyBone? = interpolateBone(name, partialTick)

	/** 插值缓存，tickAnimations 时失效 */
	private var interpCache: ProxyModel? = null

	/** 上次缓存的 partialTick */
	private var cachedPartialTick: Float = -1f

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

	fun get(name: ResourceLocation): IAnimationController? = nameMap[name]

	fun getMainController(): IAnimationController = nameMap[AnimationControllerRegistry.MAIN]
		?: error("Main controller not initialized")

	fun getAll(): List<IAnimationController> = ordered

	fun has(name: ResourceLocation): Boolean = name in nameMap

	fun isAnyActive(): Boolean = ordered.any { it.isActive() }

	fun isActive(name: ResourceLocation): Boolean = nameMap[name]?.isActive() == true

	fun getSortedActive(): List<IAnimationController> =
		ordered.filter { it.isActive() }

	fun getRenderable(): List<IAnimationController> {
		val active = getSortedActive()
		val result = mutableListOf<IAnimationController>()
		val renderedBones = mutableSetOf<String>()
		for (ctrl in active) {
			if (ctrl.affectedBones.isNotEmpty() && ctrl.affectedBones.all { it in renderedBones } && ctrl.isOverriding) {
				continue
			}
			result.add(ctrl)
			renderedBones.addAll(ctrl.affectedBones)
		}
		return result
	}

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

	private fun hasBoneConflict(a: IAnimationController, b: IAnimationController): Boolean {
		val aBones = a.affectedBones
		val bBones = b.affectedBones
		if (aBones.isEmpty() || bBones.isEmpty()) return false
		return aBones.intersect(bBones).isNotEmpty()
	}
}