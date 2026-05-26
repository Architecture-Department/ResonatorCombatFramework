package architecture.resonator_combat_framework.module.player_animation.api

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
	var posEmpty: Boolean = false,
	var rotationEmpty: Boolean = false,
	var scalaEmpty: Boolean = false
) {
	fun addLocator(locator: ProxyLocator): ProxyLocator? = locators.put(locator.name, locator)
	fun getLocator(locatorName: String): ProxyLocator? = locators[locatorName]
}

data class ProxyLocator(
	var name: String,
	val pos: Vector3f = Vector3f(),
	val rotation: Vector3f = Vector3f(),
	val scale: Vector3f = Vector3f(1f, 1f, 1f),
	var ignoreInheritedScale: Boolean = true
)
