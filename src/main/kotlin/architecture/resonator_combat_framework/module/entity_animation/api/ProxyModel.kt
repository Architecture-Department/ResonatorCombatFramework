package architecture.resonator_combat_framework.module.entity_animation.api

import org.joml.Vector3f

data class ProxyModel(
	val name: String,
	val bones: HashMap<String, ProxyBone> = hashMapOf()
) {
	fun addBone(bone: ProxyBone): ProxyBone? = bones.put(bone.name, bone)
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

	fun hasPos(): Boolean = emptyMask and EMPTY_POS == 0
	fun hasRot(): Boolean = emptyMask and EMPTY_ROT == 0
	fun hasScale(): Boolean = emptyMask and EMPTY_SCALE == 0

	fun setPosEmpty(v: Boolean) {
		emptyMask = if (v) emptyMask or EMPTY_POS else emptyMask and EMPTY_POS.inv()
	}

	fun setRotEmpty(v: Boolean) {
		emptyMask = if (v) emptyMask or EMPTY_ROT else emptyMask and EMPTY_ROT.inv()
	}

	fun setScaleEmpty(v: Boolean) {
		emptyMask = if (v) emptyMask or EMPTY_SCALE else emptyMask and EMPTY_SCALE.inv()
	}

	fun addLocator(locator: ProxyLocator): ProxyLocator? = locators.put(locator.name, locator)
	fun getLocator(locatorName: String): ProxyLocator? = locators[locatorName]
}

data class ProxyLocator(
	val name: String,
	val pos: Vector3f = Vector3f(),
	val rotation: Vector3f = Vector3f(),
	val scale: Vector3f = Vector3f(1f, 1f, 1f),
	var ignoreInheritedScale: Boolean = true
)
