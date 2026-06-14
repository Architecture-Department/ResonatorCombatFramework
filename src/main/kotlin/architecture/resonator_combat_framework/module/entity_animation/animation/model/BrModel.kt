package architecture.resonator_combat_framework.module.entity_animation.animation.model

import org.joml.Matrix4f
import org.joml.Matrix4fc
import org.joml.Vector3f

data class BrModel
@JvmOverloads
constructor(
	val bones: MutableMap<String, BrBone> = mutableMapOf(),
	val locators: MutableMap<String, BrLocator> = mutableMapOf()
) {
	/**
	 * 对父骨骼链应用变换矩阵（直接使用骨骼引用，无需Map查找）
	 */
	private fun applyParentChainTransform(chain: List<Pair<BrBone, ProxyBone?>>, matrix: Matrix4f) {
		for ((bone, proxyBone) in chain) {
			matrix.translate(bone.pivot)
			matrix.rotateXYZ(bone.rotation)
			if (proxyBone != null) {
				matrix.translate(proxyBone.pos)
				matrix.rotateXYZ(proxyBone.rotation)
				matrix.scale(proxyBone.scale)
			}
		}
	}

	/**
	 * 构建从指定骨骼到根节点的父骨骼链（存储骨骼对象引用，避免重复Map查找）
	 */
	private fun buildParentChain(startBone: BrBone, animationData: ProxyModel): List<Pair<BrBone, ProxyBone?>> {
		val chain = mutableListOf<Pair<BrBone, ProxyBone?>>()
		var currentBone: BrBone? = startBone
		while (true) {
			val parentName = currentBone?.parent ?: break
			val parentBone = bones[parentName] ?: break
			val parentProxy = animationData.bones[parentName]
			chain.add(parentBone to parentProxy)
			currentBone = parentBone
		}
		return chain.reversed()
	}

	/**
	 * 计算定位器的全局变换矩阵
	 */
	fun computeLocatorGlobalMatrix(name: String?, animationData: ProxyModel): Matrix4fc {
		val matrix = Matrix4f()
		name ?: return matrix
		val locator = locators[name] ?: return matrix
		val bone = bones[locator.boneName] ?: return matrix
		val proxyBone = animationData.bones[locator.boneName]

		// 构建并应用父骨骼链变换
		val parentChain = buildParentChain(bone, animationData)
		applyParentChainTransform(parentChain, matrix)

		// 应用当前骨骼变换
		matrix.translate(bone.pivot)
		matrix.rotateXYZ(bone.rotation)

		// 应用代理骨骼变换
		if (proxyBone != null) {
			matrix.translate(proxyBone.pos)
			matrix.rotateXYZ(proxyBone.rotation)
		}

		// 应用定位器自身位置
		matrix.translate(locator.position)

		// 如果有代理骨骼，应用缩放（影响定位器位置）
		if (proxyBone != null) {
			matrix.scale(proxyBone.scale)
		}

		return matrix
	}

	/**
	 * 计算骨骼的全局变换矩阵
	 */
	fun computeBoneGlobalMatrix(name: String?, proxyModel: ProxyModel): Matrix4fc {
		val matrix = Matrix4f()
		name ?: return matrix
		val bone = bones[name] ?: return matrix
		val proxyBone = proxyModel.bones[name]

		// 构建并应用父骨骼链变换
		val parentChain = buildParentChain(bone, proxyModel)
		applyParentChainTransform(parentChain, matrix)

		// 应用当前骨骼变换
		matrix.translate(bone.pivot)
		matrix.rotateXYZ(bone.rotation)

		// 应用代理骨骼变换
		if (proxyBone != null) {
			matrix.translate(proxyBone.pos)
			matrix.rotateXYZ(proxyBone.rotation)
			matrix.scale(proxyBone.scale)
		}
		return matrix
	}

	fun clear() {
		bones.clear()
		locators.clear()
	}

	fun add(model: BakingBrModel?) {
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
			if (locators[locator.name] == null) locators[locator.name] = BrLocator.of(locator)
		}
	}

	fun overwriteAdd(model: BakingBrModel?) {
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
			locators[locator.name] = BrLocator.of(locator)
		}
	}

	fun set(model: BakingBrModel?) {
		clear()
		if (model == null) return
		for (bone in model.bones.values) {
			bones[bone.name] = BrBone.of(bone)
		}

		for (locator in model.locators.values) {
			locators[locator.name] = BrLocator.of(locator)
		}
	}

	companion object {
		@JvmStatic
		fun of(bakingBrModel: BakingBrModel): BrModel {
			return BrModel(
				bakingBrModel.bones.map { (k, v) -> k to BrBone.of(v) }.toMap().toMutableMap(),
				bakingBrModel.locators.map { (k, v) -> k to BrLocator.of(v) }.toMap().toMutableMap()
			)
		}
	}
}

data class BrBone
@JvmOverloads
constructor(
	var name: String,
	var parent: String? = null,
	val pivot: Vector3f = Vector3f(0f, 0f, 0f),
	val rotation: Vector3f = Vector3f(0f, 0f, 0f),
	val cubes: MutableList<BrCube> = mutableListOf(),
	val locators: MutableMap<String, BrLocator> = mutableMapOf()
) {
	fun add(model: BakingBrModel) {
		model.bones[name]?.let {
			cubes.addAll(it.cubes.map(BrCube::of))
			locators.putAll(it.locators.map { (k, v) -> k to BrLocator.of(v) })
		}
	}

	fun set(model: BakingBrModel) {
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

data class BrCube
@JvmOverloads
constructor(
	var inflate: Float = 0f,
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

data class BrLocator
@JvmOverloads
constructor(
	var name: String,
	val boneName: String,
	val position: Vector3f = Vector3f()
) {
	companion object {
		@JvmStatic
		fun of(locator: BakingBrLocator): BrLocator {
			return BrLocator(
				locator.name,
				locator.boneName,
				Vector3f(locator.position)
			)
		}
	}
}
