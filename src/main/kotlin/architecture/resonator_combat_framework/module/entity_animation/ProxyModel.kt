package architecture.resonator_combat_framework.module.entity_animation

import org.joml.Vector3f

/** 代理骨骼模型：存储每帧计算的骨骼变换（局部 + 累积）。每 tick remerge 后计算填充。 */
data class ProxyModel(
	val name: String,
	val bones: HashMap<String, ProxyBone> = hashMapOf()
) {
	/** 添加或覆盖骨骼 */
	fun addBone(bone: ProxyBone): ProxyBone? = bones.put(bone.name, bone)

	/** 按名称获取骨骼 */
	fun getBone(boneName: String): ProxyBone? = bones[boneName]

}

data class ProxyBone(
	val name: String,
	val pos: Vector3f = Vector3f(),
	val rotation: Vector3f = Vector3f(),
	val scale: Vector3f = Vector3f(1f, 1f, 1f),
	val locators: HashMap<String, ProxyLocator> = hashMapOf(),
	/** 位标记：0x1=pos空, 0x2=rot空, 0x4=scale空。setXxxEmpty 系列函数操作此字段 */
	private var emptyMask: Int = 0
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

	/** 添加定位器 */
	fun addLocator(locator: ProxyLocator): ProxyLocator? = locators.put(locator.name, locator)

	/** 按名称获取定位器 */
	fun getLocator(locatorName: String): ProxyLocator? = locators[locatorName]
}

/** 代理定位器：骨骼上的附加点，用于音效/粒子发射位置 */
data class ProxyLocator(
	val name: String,
	/** 定位器位置（模型空间） */
	val pos: Vector3f = Vector3f(),
	/** 定位器旋转 */
	val rotation: Vector3f = Vector3f(),
	/** 定位器缩放 */
	val scale: Vector3f = Vector3f(1f, 1f, 1f),
	var ignoreInheritedScale: Boolean = true
)
