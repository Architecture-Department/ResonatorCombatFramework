package architecture.resonator_combat_framework.module.entity_animation.engine

import com.google.gson.JsonObject

data class BrBoneAnimation(
	val pos: List<BrBoneKeyFrame> = emptyList(),
	val rot: List<BrBoneKeyFrame> = emptyList(),
	val scale: List<BrBoneKeyFrame> = emptyList()
) {
	companion object {
		/** 从所有骨骼关键帧中取最大时间作为动画长度，至少 1 秒 */
		@JvmStatic
		fun calcAnimLength(bones: Map<String, BrBoneAnimation>): Float {
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
		fun parses(bonesJson: JsonObject): Map<String, BrBoneAnimation> {
			val bones = mutableMapOf<String, BrBoneAnimation>()
			for ((name, bj) in bonesJson.entrySet()) {
				val bObj = bj.asJsonObject
				bones[name] = BrBoneAnimation(
					BrBoneKeyFrame.parse(bObj.get("position")),
					BrBoneKeyFrame.parse(bObj.get("rotation")),
					BrBoneKeyFrame.parse(bObj.get("scale"))
				)
			}
			return bones
		}
	}
}