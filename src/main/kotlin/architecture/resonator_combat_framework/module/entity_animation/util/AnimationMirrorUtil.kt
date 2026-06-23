package architecture.resonator_combat_framework.module.entity_animation.util

/** 动画镜像工具 — 骨骼名称映射 */
object AnimationMirrorUtil {

	/** 骨骼名称镜像映射表 */
	private val BONE_MIRROR_MAP = mapOf(
		"left_arm" to "right_arm",
		"right_arm" to "left_arm",
		"left_leg" to "right_leg",
		"right_leg" to "left_leg",
		"left_item" to "right_item",
		"right_item" to "left_item",
		"left_sleeve" to "right_sleeve",
		"right_sleeve" to "left_sleeve",
		"left_pants" to "right_pants",
		"right_pants" to "left_pants",
	)

	/** 镜像骨骼名称 */
	fun mirrorBoneName(name: String): String = BONE_MIRROR_MAP[name] ?: name
}
