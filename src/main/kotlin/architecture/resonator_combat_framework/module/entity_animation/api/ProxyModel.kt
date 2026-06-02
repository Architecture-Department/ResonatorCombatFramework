package architecture.resonator_combat_framework.module.entity_animation.api

import org.joml.Vector3f

class ProxyModel(
	var name: String,
	val bones: HashMap<String, ProxyBone> = hashMapOf()
) {
	fun addBone(bone: ProxyBone): ProxyBone? = bones.put(bone.name, bone)
	fun getBone(boneName: String): ProxyBone? = bones[boneName]
}

data class ProxyBone(
	var name: String,
	val pos: Vector3f = Vector3f(),
	val rotation: Vector3f = Vector3f(),
	val scale: Vector3f = Vector3f(1f, 1f, 1f),
	val locators: HashMap<String, ProxyLocator> = hashMapOf(),
	/** 位标记：0x1=pos空, 0x2=rot空, 0x4=scale空。setXxxEmpty 系列函数操作此字段 */
	var emptyMask: Int = 0
) {
	fun addLocator(locator: ProxyLocator): ProxyLocator? = locators.put(locator.name, locator)
	fun getLocator(locatorName: String): ProxyLocator? = locators[locatorName]
}

const val EMPTY_POS = 0x1
const val EMPTY_ROT = 0x2
const val EMPTY_SCALE = 0x4

fun ProxyBone.hasPos(): Boolean = emptyMask and EMPTY_POS == 0
fun ProxyBone.hasRot(): Boolean = emptyMask and EMPTY_ROT == 0
fun ProxyBone.hasScale(): Boolean = emptyMask and EMPTY_SCALE == 0

fun ProxyBone.setPosEmpty(v: Boolean) {
	emptyMask = if (v) emptyMask or EMPTY_POS else emptyMask and EMPTY_POS.inv()
}

fun ProxyBone.setRotEmpty(v: Boolean) {
	emptyMask = if (v) emptyMask or EMPTY_ROT else emptyMask and EMPTY_ROT.inv()
}

fun ProxyBone.setScaleEmpty(v: Boolean) {
	emptyMask = if (v) emptyMask or EMPTY_SCALE else emptyMask and EMPTY_SCALE.inv()
}

data class ProxyLocator(
	var name: String,
	val pos: Vector3f = Vector3f(),
	val rotation: Vector3f = Vector3f(),
	val scale: Vector3f = Vector3f(1f, 1f, 1f),
	var ignoreInheritedScale: Boolean = true
)
