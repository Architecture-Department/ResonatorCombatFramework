package architecture.resonator_combat_framework.module.entity_animation.animation.model

import architecture.goldenboughs_lib.util.toRadians
import org.joml.Matrix4f
import org.joml.Vector3f
import org.joml.Vector3fc

// TODO 对应不同体型的玩家取不同的位置
data class GeometryModel
@JvmOverloads
constructor(
	val bones: MutableMap<String, BrBone> = mutableMapOf(),
	val locators: MutableMap<String, BrLocator> = mutableMapOf()
) {

	/**
	 * 用 PoseStack 计算定位器的变换矩阵（模型空间）。
	 * 先计算所属骨骼的变换，再应用定位器相对于骨骼的偏移。
	 */
	fun computeLocatorGlobalMatrix(
		name: String?,
		animationData: PoseData,
		isWorld: Boolean = false
	): Matrix4f {
		name ?: return Matrix4f()
		val locator = locators[name] ?: return Matrix4f()
		val bone = bones[locator.boneName] ?: return Matrix4f()

		val scale = if (isWorld) 16 else 1
		val matrix = computeBoneGlobalMatrix(bone.name, animationData, isWorld)

		matrix.translate(
			(locator.offset.x() - bone.pivot.x()) / scale,
			(locator.offset.y() - bone.pivot.y()) / scale,
			-(locator.offset.z() - bone.pivot.z()) / scale
		)
		matrix.rotateZ(locator.rotation.z().toRadians())
		matrix.rotateY(locator.rotation.y().toRadians())
		matrix.rotateX(locator.rotation.x().toRadians())

		return matrix
	}

	/**
	 * 用 PoseStack 计算骨骼的层次变换矩阵（模型空间）。
	 * 从根到目标骨骼构建完整变换链。
	 */
	fun computeBoneGlobalMatrix(
		name: String?,
		animationData: PoseData,
		isWorld: Boolean = false
	): Matrix4f {
		name ?: return Matrix4f()
		if (!bones.containsKey(name)) return Matrix4f()

		val scale = if (isWorld) 16 else 1
		return buildChain(name, animationData, scale)
	}

	/**
	 * 从根到指定骨骼构建完整变换链，应用每层的 pivot 偏移 + 代理变换 + 旋转 + 缩放。
	 * 使用相对父骨骼的 pivot 偏移来确定位置。
	 */
	private fun buildChain(
		startName: String,
		animationData: PoseData,
		scale: Int
	): Matrix4f {
		// 收集从 startName 到根的所有骨骼（自身→父级），然后反转得到根→自身的顺序
		val chain = mutableListOf<Pair<BrBone, BonePose?>>()
		var currentName: String? = startName
		while (true) {
			val bone = bones[currentName] ?: break
			val proxy = animationData.bones[currentName]
			chain.add(bone to proxy)
			currentName = bone.parent
		}
		chain.reverse()

		val matrix = Matrix4f()
		val prevPivot = Vector3f()
		for ((bone, proxyBone) in chain) {
			// pivot 相对父骨骼偏移（模型空间坐标 → 相对坐标）
			matrix.translate(
				(bone.pivot.x() - prevPivot.x()) / scale,
				(bone.pivot.y() - prevPivot.y()) / scale,
				-(bone.pivot.z() - prevPivot.z()) / scale
			)
			// ZYX 旋转变换（先 Z → Y → X）
			matrix.rotateZ(bone.rotation.z().toRadians())
			matrix.rotateY(bone.rotation.y().toRadians())
			matrix.rotateX(bone.rotation.x().toRadians())

			// 代理动画位移
			if (proxyBone != null) matrix.translate(
				proxyBone.pos.x / scale,
				proxyBone.pos.y / scale,
				-proxyBone.pos.z / scale
			)
			// ZYX 旋转变换（先 Z → Y → X）代理动画旋转
			if (proxyBone != null) {
				matrix.rotateZ(-proxyBone.rotation.z.toRadians())
				matrix.rotateY(-proxyBone.rotation.y.toRadians())
				matrix.rotateX(proxyBone.rotation.x.toRadians())
				// 代理动画缩放
				matrix.scale(proxyBone.scale.x, proxyBone.scale.y, proxyBone.scale.z)
			}

			prevPivot.set(bone.pivot)
		}

		return matrix
	}

	/** 清空所有骨骼和定位器 */
	fun clear() {
		bones.clear()
		locators.clear()
	}

	/** 合并模型：骨骼合并不覆盖已有，定位器不覆盖已有 */
	fun add(model: GeometryData?) {
		mergeBones(model, overwriteLocators = false)
	}

	/** 合并模型：骨骼合并不覆盖已有，定位器覆盖已有 */
	fun overwriteAdd(model: GeometryData?) {
		mergeBones(model, overwriteLocators = true)
	}

	/** 完全替换为指定模型 */
	fun set(model: GeometryData?) {
		clear()
		if (model == null) return
		for (bone in model.bones.values) {
			bones[bone.name] = BrBone.of(bone)
		}
		for (locator in model.locators.values) {
			locators[locator.name] = BrLocator.of(locator)
		}
	}

	/**
	 * 合并 BakingBrModel 的骨骼和定位器到当前模型。
	 * 骨骼合并不覆盖已有（仅向已有骨骼添加 cubes/locators），
	 * 定位器按 [overwriteLocators] 决定是否覆盖。
	 */
	private fun mergeBones(model: GeometryData?, overwriteLocators: Boolean) {
		if (model == null) return
		for (bone in model.bones.values) {
			val brBone = bones[bone.name]
			if (brBone != null) {
				brBone.add(model)
			} else {
				bones[bone.name] = BrBone.of(bone)
			}
		}
		for (locator in model.locators.values) {
			if (overwriteLocators || locators[locator.name] == null) {
				locators[locator.name] = BrLocator.of(locator)
			}
		}
	}

	companion object {
		@JvmStatic
		fun of(geometryData: GeometryData): GeometryModel {
			return GeometryModel(
				geometryData.bones.map { (k, v) -> k to BrBone.of(v) }.toMap().toMutableMap(),
				geometryData.locators.map { (k, v) -> k to BrLocator.of(v) }.toMap().toMutableMap()
			)
		}
	}
}

/** 骨骼数据 */
data class BrBone
@JvmOverloads
constructor(
	val name: String,
	val parent: String? = null,
	val pivot: Vector3fc = Vector3f(0f, 0f, 0f),
	val rotation: Vector3fc = Vector3f(0f, 0f, 0f),
	val cubes: MutableList<BrCube> = mutableListOf(),
	val locators: MutableMap<String, BrLocator> = mutableMapOf()
) {
	/** 从 BakingBrModel 合并 cubes 和 locators 到已有骨骼 */
	fun add(model: GeometryData) {
		model.bones[name]?.let {
			cubes.addAll(it.cubes.map(BrCube::of))
			locators.putAll(it.locators.map { (k, v) -> k to BrLocator.of(v) })
		}
	}

	/** 从 BakingBrModel 替换 cubes 和 locators */
	fun set(model: GeometryData) {
		model.bones[name]?.let {
			cubes.clear()
			locators.clear()
			cubes.addAll(it.cubes.map(BrCube::of))
			locators.putAll(it.locators.map { (k, v) -> k to BrLocator.of(v) })
		}
	}

	companion object {
		@JvmStatic
		fun of(bone: BakingBrBone): BrBone {
			return BrBone(
				bone.name,
				bone.parent,
				Vector3f(bone.pivot),
				Vector3f(bone.rotation),
				bone.cubes.map { BrCube.of(it) }.toMutableList(),
				bone.locators.map { (k, v) -> k to BrLocator.of(v) }.toMap().toMutableMap()
			)
		}
	}
}

/** 立方体数据 */
data class BrCube
@JvmOverloads
constructor(
	val inflate: Float = 0f,
	val origin: Vector3f = Vector3f(),
	val size: Vector3f = Vector3f(),
	val rotation: Vector3f = Vector3f()
) {
	companion object {
		@JvmStatic
		fun of(cube: BakingBrCube): BrCube {
			return BrCube(
				cube.inflate,
				Vector3f(cube.origin),
				Vector3f(cube.size),
				Vector3f(cube.rotation)
			)
		}
	}
}

/** 定位器数据 */
data class BrLocator
@JvmOverloads
constructor(
	val name: String,
	val boneName: String,
	val offset: Vector3fc = Vector3f(),
	val rotation: Vector3fc = Vector3f()
) {
	companion object {
		@JvmStatic
		fun of(locator: BakingBrLocator): BrLocator {
			return BrLocator(
				locator.name,
				locator.boneName,
				Vector3f(locator.offset),
				Vector3f(locator.rotation)
			)
		}
	}
}
