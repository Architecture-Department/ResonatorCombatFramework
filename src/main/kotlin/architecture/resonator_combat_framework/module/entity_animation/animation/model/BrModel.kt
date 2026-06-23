package architecture.resonator_combat_framework.module.entity_animation.animation.model

import architecture.goldenboughs_lib.util.PoseStack
import architecture.goldenboughs_lib.util.toRadians
import com.mojang.math.Axis
import org.joml.Vector3f
import org.joml.Vector3fc

// TODO对应不同体型的玩家取不同的位置
data class BrModel
@JvmOverloads
constructor(
	val bones: MutableMap<String, BrBone> = mutableMapOf(),
	val locators: MutableMap<String, BrLocator> = mutableMapOf()
) {

	/**
	 * 用 PoseStack 计算定位器的变换矩阵（模型空间）。
	 */
	fun computeLocatorGlobalMatrix(
		name: String?,
		animationData: ProxyModel,
		poseStack: PoseStack = PoseStack(),
		isWorld: Boolean = false
	): PoseStack {
		name ?: return poseStack
		val locator = locators[name] ?: return poseStack
		val bone = bones[locator.boneName] ?: return poseStack

		computeBoneGlobalMatrix(bone.name, animationData, poseStack, isWorld)

		val scale = if (isWorld) 16 else 1
		poseStack.translate(
			(locator.offset.x() - bone.pivot.x()) / scale,
			(locator.offset.y() - bone.pivot.y()) / scale,
			-(locator.offset.z() - bone.pivot.z()) / scale
		)
		poseStack.mulPose(Axis.ZP.rotation(locator.rotation.z().toRadians()))
		poseStack.mulPose(Axis.YP.rotation(locator.rotation.y().toRadians()))
		poseStack.mulPose(Axis.XP.rotation(locator.rotation.x().toRadians()))
//		poseStack.translate(-locator.offset.x() , -locator.offset.y() , -locator.offset.z() )

		return poseStack
	}

	/**
	 * 用 PoseStack 计算骨骼的层次变换矩阵（模型空间）。
	 */
	fun computeBoneGlobalMatrix(
		name: String?,
		animationData: ProxyModel,
		poseStack: PoseStack = PoseStack(),
		isWorld: Boolean = false
	): PoseStack {
		name ?: return poseStack
		if (!bones.containsKey(name)) return poseStack

		val scale = if (isWorld) 16 else 1
		buildChainOnPoseStack(poseStack, name, animationData, scale)

		return poseStack
	}

	/**
	 * 在 PoseStack 上构建从根到指定骨骼的完整变换链。
	 * 使用相对父骨骼偏移定位，旋转通过 Axis 应用。
	 */
	private fun buildChainOnPoseStack(
		poseStack: PoseStack,
		startName: String,
		animationData: ProxyModel,
		scale: Int
	) {
		val result = mutableListOf<Pair<BrBone, ProxyBone?>>()
		var currentName: String? = startName
		while (true) {
			val bone = bones[currentName] ?: break
			val proxy = animationData.bones[currentName]
			result.add(bone to proxy)
			currentName = bone.parent
		}
		val chain = result.reversed()

		val prevPivot = Vector3f()
		for ((bone, proxyBone) in chain) {
			poseStack.translate(
				(bone.pivot.x() - prevPivot.x()) / scale,
				(bone.pivot.y() - prevPivot.y()) / scale,
				-(bone.pivot.z() - prevPivot.z()) / scale
			)
			if (proxyBone != null) poseStack.translate(
				proxyBone.pos.x / scale,
				proxyBone.pos.y / scale,
				-proxyBone.pos.z / scale
			)
			poseStack.mulPose(Axis.ZP.rotation(bone.rotation.z().toRadians()))
			if (proxyBone != null) poseStack.mulPose(Axis.ZP.rotation(-proxyBone.rotation.z.toRadians()))
			poseStack.mulPose(Axis.YP.rotation(bone.rotation.y().toRadians()))
			if (proxyBone != null) poseStack.mulPose(Axis.YP.rotation(-proxyBone.rotation.y.toRadians()))
			poseStack.mulPose(Axis.XP.rotation(bone.rotation.x().toRadians()))
			if (proxyBone != null) poseStack.mulPose(Axis.XP.rotation(proxyBone.rotation.x.toRadians()))
			if (proxyBone != null) poseStack.scale(proxyBone.scale.x, proxyBone.scale.y, proxyBone.scale.z)

			prevPivot.set(bone.pivot)
		}
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
	val name: String,
	val parent: String? = null,
	val pivot: Vector3fc = Vector3f(0f, 0f, 0f),
	val rotation: Vector3fc = Vector3f(0f, 0f, 0f),
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
