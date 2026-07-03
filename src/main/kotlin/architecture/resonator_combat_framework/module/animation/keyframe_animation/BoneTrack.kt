package architecture.resonator_combat_framework.module.animation.keyframe_animation

import com.google.gson.JsonObject

/**
 * 骨骼动画轨道数据类，包含一根骨骼在动画中的位置、旋转和缩放关键帧序列。
 * 每根骨骼在动画中可以有三组独立的关键帧轨道（位置/旋转/缩放）。
 *
 * @property pos 位置关键帧列表
 * @property rot 旋转关键帧列表
 * @property scale 缩放关键帧列表
 */
data class BoneTrack
@JvmOverloads constructor(
	val pos: List<Keyframe> = emptyList(),
	val rot: List<Keyframe> = emptyList(),
	val scale: List<Keyframe> = emptyList()
) {
	/**
	 * 返回镜像后的骨骼动画副本：位置 X 取反，旋转 Y/Z 取反。
	 *
	 * @return 镜像后的骨骼动画轨道
	 */
	fun mirrored(): BoneTrack = BoneTrack(
		pos = pos.map { it.mirroredPos() },
		rot = rot.map { it.mirroredRot() },
		scale = scale
	)

	companion object {
		/**
		 * 从所有骨骼关键帧中取最大时间作为动画长度，至少返回 1 秒。
		 *
		 * @param bones 骨骼名称到骨骼动画轨道的映射
		 * @return 计算得到的动画长度（秒）
		 */
		@JvmStatic
		fun calcAnimLength(bones: Map<String, BoneTrack>): Float {
			var maxTime = 0f
			for ((_, ba) in bones) {
				for (kf in ba.pos) {
					if (kf.time > maxTime) maxTime = kf.time
				}
				for (kf in ba.rot) {
					if (kf.time > maxTime) maxTime = kf.time
				}
				for (kf in ba.scale) {
					if (kf.time > maxTime) maxTime = kf.time
				}
			}
			return if (maxTime > 0f) maxTime else 1f  // 至少 1 秒（会被外部 *20）
		}

		/**
		 * 解析 JSON 骨骼字典：{ 骨骼名 → { position/rotation/scale } }。
		 *
		 * @param bonesJson JSON 骨骼对象
		 * @return 骨骼名称到骨骼动画轨道的映射
		 */
		@JvmStatic
		fun parses(bonesJson: JsonObject): Map<String, BoneTrack> {
			val bones = mutableMapOf<String, BoneTrack>()
			for ((name, bj) in bonesJson.entrySet()) {
				val bObj = bj.asJsonObject
				bones[name] = BoneTrack(
					Keyframe.parse(bObj.get("position")),
					Keyframe.parse(bObj.get("rotation")),
					Keyframe.parse(bObj.get("scale"))
				)
			}
			return bones
		}
	}
}
