package architecture.resonator_combat_framework.module.entity_animation.animation.baking_animation

import com.google.gson.JsonObject

data class BakingBrBoneAnimation
@JvmOverloads constructor(
	val pos: List<BakingBrBoneKeyFrame> = emptyList(),
	val rot: List<BakingBrBoneKeyFrame> = emptyList(),
	val scale: List<BakingBrBoneKeyFrame> = emptyList()
) {
	/**
	 * 返回镜像后的骨骼动画副本：位置 X 取反，旋转 Y/Z 取反。
	 */
	fun mirrored(): BakingBrBoneAnimation = BakingBrBoneAnimation(
		pos = pos.map { it.mirroredPos() },
		rot = rot.map { it.mirroredRot() },
		scale = scale
	)

	companion object {
		/** 从所有骨骼关键帧中取最大时间作为动画长度，至少 1 秒 */
		@JvmStatic
		fun calcAnimLength(bones: Map<String, BakingBrBoneAnimation>): Float {
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

		/** 解析骨骼字典：{ 骨骼名 → { position/rotation/scale } } */
		@JvmStatic
		fun parses(bonesJson: JsonObject): Map<String, BakingBrBoneAnimation> {
			val bones = mutableMapOf<String, BakingBrBoneAnimation>()
			for ((name, bj) in bonesJson.entrySet()) {
				val bObj = bj.asJsonObject
				bones[name] = BakingBrBoneAnimation(
					BakingBrBoneKeyFrame.parse(bObj.get("position")),
					BakingBrBoneKeyFrame.parse(bObj.get("rotation")),
					BakingBrBoneKeyFrame.parse(bObj.get("scale"))
				)
			}
			return bones
		}
	}
}