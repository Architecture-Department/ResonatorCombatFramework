package architecture.resonator_combat_framework.module.animation.model

import org.joml.Vector3f

/**
 * 代理骨骼模型——存储每帧计算的骨骼变换（局部 + 累积）。每 tick remerge 后计算填充。
 *
 * @property name 模型名称
 * @property bones 骨骼名称到姿态数据的映射
 */
data class PoseData
@JvmOverloads
constructor(
	val name: String,
	val bones: HashMap<String, BonePose> = hashMapOf(),
) {
	/** 添加或覆盖骨骼 */
	fun addBone(bone: BonePose): BonePose? = bones.put(bone.name, bone)

	/** 按名称获取骨骼 */
	fun getBone(boneName: String): BonePose? = bones[boneName]
}

/**
 * 骨骼姿态数据——存储一根骨骼在某一帧的变换值（位移、旋转、缩放）。
 *
 * 使用空掩码（emptyMask）标记哪些分量已被显式设置，
 * 以区分"保持默认值"和"未设置"两种状态。
 *
 * @property name 骨骼名称
 * @property pos 局部位移
 * @property rotation 局部旋转（欧拉角，度）
 * @property scale 局部缩放
 * @property noInterp 标记此骨骼来自 STEP 关键帧，渲染插值时应跳过
 */
data class BonePose
@JvmOverloads
constructor(
	val name: String,
	val pos: Vector3f = Vector3f(),
	val rotation: Vector3f = Vector3f(),
	val scale: Vector3f = Vector3f(1f, 1f, 1f),
	/** 位标记：0x1=pos空, 0x2=rot空, 0x4=scale空。setXxxEmpty 系列函数操作此字段 */
	private var emptyMask: Int = 0,
	/** 标记此骨骼来自 STEP 关键帧，渲染插值时应跳过 */
	var noInterp: Boolean = false
) {
	companion object {
		private const val EMPTY_POS = 0x1
		private const val EMPTY_ROT = 0x2
		private const val EMPTY_SCALE = 0x4
	}

	init {
		resetEmpty()
	}

	fun resetEmpty() {
		setPosEmpty(true)
		setRotEmpty(true)
		setScaleEmpty(true)
	}

	/** 局部位移是否已设置（空掩码检查） */
	fun hasPos(): Boolean = emptyMask and EMPTY_POS == 0

	/** 局部旋转是否已设置 */
	fun hasRot(): Boolean = emptyMask and EMPTY_ROT == 0

	/** 局部缩放是否已设置 */
	fun hasScale(): Boolean = emptyMask and EMPTY_SCALE == 0

	/** 标记局部位移是否为空 */
	fun setPosEmpty(v: Boolean) {
		emptyMask = if (v) emptyMask or EMPTY_POS else emptyMask and EMPTY_POS.inv()
	}

	/** 标记局部旋转是否为空 */
	fun setRotEmpty(v: Boolean) {
		emptyMask = if (v) emptyMask or EMPTY_ROT else emptyMask and EMPTY_ROT.inv()
	}

	/** 标记局部缩放是否为空 */
	fun setScaleEmpty(v: Boolean) {
		emptyMask = if (v) emptyMask or EMPTY_SCALE else emptyMask and EMPTY_SCALE.inv()
	}
}
