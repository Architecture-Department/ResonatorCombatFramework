package architecture.resonator_combat_framework.module.entity_animation.util

/**
 * 动画镜像工具，提供骨骼名称的左右镜像映射。
 * 用于将左侧骨骼名称映射到对应的右侧骨骼名称（反之亦然），
 * 以实现动画的左右对称播放。
 */
object MirrorUtil {

	/** 骨骼名称镜像映射表，key 为源名称，value 为镜像后的名称 */
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

	/**
	 * 获取指定骨骼名称的镜像名称。
	 * 如果该骨骼没有对应的镜像映射，则返回原名称。
	 * @param name 原始骨骼名称
	 * @return 镜像后的骨骼名称，若无映射则返回原名称
	 */
	fun mirrorBoneName(name: String): String = BONE_MIRROR_MAP[name] ?: name
}
