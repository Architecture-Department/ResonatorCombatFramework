package architecture.resonator_combat_framework.model

import org.joml.Vector3f
import org.joml.Vector3fc

/**
 * 运行时骨骼数据。
 *
 * @property name 骨骼名称
 * @property parent 父骨骼名称
 * @property pivot 轴心点
 * @property rotation 默认旋转
 * @property cubes 立方体列表
 * @property locators 定位器映射
 */
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
	/** 从 [GeometryModel] 合并 cubes 和 locators 到已有骨骼 */
	fun add(model: GeometryModel) {
		model.bones[name]?.let {
			cubes.addAll(it.cubes.map(BrCube::of))
			locators.putAll(it.locators.map { (k, v) -> k to BrLocator.of(v) })
		}
	}

	/** 从 [GeometryModel] 替换 cubes 和 locators */
	fun set(model: GeometryModel) {
		model.bones[name]?.let {
			cubes.clear()
			locators.clear()
			cubes.addAll(it.cubes.map(BrCube::of))
			locators.putAll(it.locators.map { (k, v) -> k to BrLocator.of(v) })
		}
	}

	companion object {
		/**
		 * 从 [BakingBrBone] 转换为运行时 [BrBone]。
		 *
		 * @param bone 烘培骨骼数据
		 * @return 运行时骨骼实例
		 */
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