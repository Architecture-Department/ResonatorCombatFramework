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
	internal fun applyParentChainTransform(chain: List<Pair<BrBone, ProxyBone?>>, matrix: Matrix4f) {
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

	fun resolveLocatorGlobal(name: String, proxyModel: ProxyModel): Matrix4fc {
		val locator = locators[name] ?: return Matrix4f()

		// 使用定位器的缓存全局矩阵
		locator.getCachedGlobalMatrix()?.let { return it }

		val bone = bones[locator.boneName] ?: return Matrix4f()
		val proxyBone = proxyModel.bones[locator.boneName]

		// 使用缓存的全局矩阵
		val boneMatrix = bone.getOrComputeGlobalMatrix(this, proxyModel)

		// 在骨骼全局矩阵基础上应用定位器偏移
		val result = Matrix4f(boneMatrix)
		result.translate(locator.position)

		// 如果有代理骨骼，应用缩放（影响定位器位置）
		if (proxyBone != null) {
			result.scale(proxyBone.scale)
		}

		// 缓存结果
		locator.cacheGlobalMatrix(result)
		return result
	}

	fun resolveBoneGlobal(name: String, proxyModel: ProxyModel): Matrix4fc {
		val bone = bones[name] ?: return Matrix4f()

		// 直接使用缓存的全局矩阵
		return bone.getOrComputeGlobalMatrix(this, proxyModel)
	}

	fun clear() {
		bones.clear()
		locators.clear()
		// 清除所有骨骼的缓存
		bones.values.forEach { it.invalidateCache() }
		// 清除所有定位器的缓存
		locators.values.forEach { it.invalidateCache() }
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
			else locators[locator.name]?.invalidateCache()  // 如果已存在，清除缓存
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
			locators[locator.name]?.invalidateCache()  // 清除旧定位器缓存
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
	// 缓存的全局变换矩阵（null表示需要重新计算）
	@Volatile
	private var cachedGlobalMatrix: Matrix4f? = null

	/**
	 * 标记缓存为无效（当骨骼数据变化时调用）
	 */
	fun invalidateCache() {
		cachedGlobalMatrix = null
	}

	/**
	 * 获取或计算全局变换矩阵（带缓存）
	 */
	fun getOrComputeGlobalMatrix(brModel: BrModel, proxyModel: ProxyModel): Matrix4f {
		// 检查缓存是否有效
		cachedGlobalMatrix?.let { return it }

		// 计算新的全局矩阵
		val matrix = Matrix4f()
		val proxyBone = proxyModel.bones[name]

		// 构建并应用父骨骼链变换
		val parentChain = buildParentChainFromBone(brModel, proxyModel)
		brModel.applyParentChainTransform(parentChain, matrix)

		// 应用当前骨骼变换
		matrix.translate(pivot)
		matrix.rotateXYZ(rotation)

		// 应用代理骨骼变换
		if (proxyBone != null) {
			matrix.translate(proxyBone.pos)
			matrix.rotateXYZ(proxyBone.rotation)
			matrix.scale(proxyBone.scale)
		}

		// 缓存结果
		cachedGlobalMatrix = matrix.clone() as Matrix4f
		return matrix
	}

	/**
	 * 构建从指定骨骼到根节点的父骨骼链（存储骨骼对象引用，避免重复Map查找）
	 */
	private fun buildParentChainFromBone(brModel: BrModel, proxyModel: ProxyModel): List<Pair<BrBone, ProxyBone?>> {
		val chain = mutableListOf<Pair<BrBone, ProxyBone?>>()
		var currentBone: BrBone? = this
		while (true) {
			val parentName = currentBone?.parent ?: break
			val parentBone = brModel.bones[parentName] ?: break
			val parentProxy = proxyModel.bones[parentName]
			chain.add(parentBone to parentProxy)
			currentBone = parentBone
		}
		// 反转列表，使其从根节点到当前骨骼的顺序
		return chain.reversed()
	}

	fun add(model: BakingBrModel) {
		model.bones[name]?.let {
			cubes.addAll(it.cubes.map(BrCube::of))
			locators.putAll(it.locators.map { (k, v) -> k to BrLocator.of(v) })
			invalidateCache()  // 数据变化，清除缓存
		}
	}

	fun set(model: BakingBrModel) {
		model.bones[name]?.let {
			cubes.clear()
			locators.clear()
			cubes.addAll(it.cubes.map(BrCube::of))
			locators.putAll(it.locators.map { (k, v) -> k to BrLocator.of(v) })
			invalidateCache()  // 数据变化，清除缓存
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
	// 缓存的全局变换矩阵（null表示需要重新计算）
	@Volatile
	private var cachedGlobalMatrix: Matrix4f? = null

	/**
	 * 标记缓存为无效（当定位器数据变化时调用）
	 */
	fun invalidateCache() {
		cachedGlobalMatrix = null
	}

	/**
	 * 缓存全局矩阵
	 */
	internal fun cacheGlobalMatrix(matrix: Matrix4f) {
		cachedGlobalMatrix = matrix
	}

	/**
	 * 获取缓存的全局矩阵（如果有效）
	 */
	internal fun getCachedGlobalMatrix(): Matrix4f? {
		return cachedGlobalMatrix
	}

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
