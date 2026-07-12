package architecture.resonator_combat_framework.model

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

	companion object {
		/**
		 * 深拷贝 PoseData 的所有骨骼到新对象。
		 *
		 * @param source 源姿态数据
		 * @return 深拷贝后的姿态数据
		 */
		@JvmStatic
		fun copy(source: PoseData): PoseData {
			val result = PoseData("interp")
			for ((name, bone) in source.bones) {
				val copy = BonePose(name)
				copy.pos.set(bone.pos)
				copy.rotation.set(bone.rotation)
				copy.scale.set(bone.scale)
				if (bone.hasPos()) copy.setPosEmpty(false)
				if (bone.hasRot()) copy.setRotEmpty(false)
				if (bone.hasScale()) copy.setScaleEmpty(false)
				copy.noInterp = bone.noInterp
				result.addBone(copy)
			}
			return result
		}

		/**
		 * 在前后帧姿态之间对所有骨骼插值，返回插值后的 [PoseData]。
		 *
		 * 使用 [BonePose.interpolate] 分别对每根骨骼做 pos/rot/scale 插值。
		 *
		 * @param prevPose 前一帧姿态
		 * @param currPose 当前帧姿态
		 * @param partialTick 插值系数（0~1）
		 * @return 插值后的姿态数据
		 */
		@JvmStatic
		fun interpolate(prevPose: PoseData, currPose: PoseData, partialTick: Float): PoseData {
			if (partialTick == 0f) return copy(prevPose)
			if (partialTick == 1f) return copy(currPose)
			val result = PoseData("interp")
			for ((name, _) in currPose.bones) {
				BonePose.interpolate(name, prevPose, currPose, partialTick)?.let { result.addBone(it) }
			}
			return result
		}
	}
}