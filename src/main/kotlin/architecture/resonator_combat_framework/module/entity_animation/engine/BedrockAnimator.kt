package architecture.resonator_combat_framework.module.entity_animation.engine

import architecture.resonator_combat_framework.module.entity_animation.api.ProxyBone
import architecture.resonator_combat_framework.module.entity_animation.api.ProxyModel
import architecture.resonator_combat_framework.module.entity_animation.engine.molang.EasingTypes
import org.joml.Vector2f
import org.joml.Vector3f
import kotlin.math.floor

object BedrockAnimator {

	/** 计算动画在 time 时刻的骨骼变换并写入 proxyModel，返回受影响骨骼集合 */
	fun computeAndWrite(anim: BrBedrockAnimation, time: Float, proxyModel: ProxyModel): Set<String> {
		val affected = mutableSetOf<String>()
		for ((boneName, boneAnim) in anim.bones) {
			val pos = interpolate(boneAnim.pos, time)
			val rot = interpolate(boneAnim.rot, time)
			val scale = interpolate(boneAnim.scale, time)

			val bone = proxyModel.getBone(boneName) ?: ProxyBone(boneName).also { proxyModel.addBone(it) }
			bone.setPosEmpty(pos == null)
			if (pos != null) bone.pos.set(pos) else bone.pos.set(0f, 0f, 0f)
			bone.setRotEmpty(rot == null)
			if (rot != null) bone.rotation.set(rot) else bone.rotation.set(0f, 0f, 0f)
			bone.setScaleEmpty(scale == null)
			if (scale != null) bone.scale.set(scale) else bone.scale.set(1f, 1f, 1f)
			affected.add(boneName)
		}
		return affected
	}

	/** 插值关键帧序列，返回 time 时刻的值 */
	private fun interpolate(frames: List<BrBoneKeyFrame>, time: Float): Vector3f? {
		if (frames.isEmpty()) return null

		val afterIdx = frames.indexOfFirst { it.time > time }

		if (afterIdx < 0) {
			// time >= 最后一帧的时间 → PAST_END，返回最后一帧的值
			return Vector3f(frames.last().evaluateValue())
		}
		if (afterIdx == 0) {
			// time < 第一帧的时间 → BEFORE_START，返回第一帧的值
			return Vector3f(frames.first().evaluateValue())
		}

		val before = frames[afterIdx - 1]
		val after = frames[afterIdx]
		val weight = (time - before.time) / (after.time - before.time)

		return when {
			before.lerp == BrBoneKeyFrame.LerpMode.STEP -> {
				Vector3f(before.evaluateValue())
			}

			before.lerp == BrBoneKeyFrame.LerpMode.CATMULLROM || after.lerp == BrBoneKeyFrame.LerpMode.CATMULLROM -> {
				val beforePlus = if (afterIdx > 1) frames[afterIdx - 2] else null
				val afterPlus = if (afterIdx < frames.size - 1) frames[afterIdx + 1] else null

				val useFirstPoint = beforePlus != null && !(before.hasPreData() && before.hasPostData())
				val useLastPoint = afterPlus != null && !(after.hasPreData() && after.hasPostData())

				fun buildAxis(
					getBpValue: (BrBoneKeyFrame) -> Float,
					getBeforeValue: (BrBoneKeyFrame) -> Float,
					getAfterValue: (BrBoneKeyFrame) -> Float,
					getApValue: (BrBoneKeyFrame) -> Float
				): Float {
					val pts = mutableListOf<Vector2f>()
					if (useFirstPoint) pts.add(Vector2f(beforePlus.time, getBpValue(beforePlus)))
					pts.add(Vector2f(before.time, getBeforeValue(before)))
					pts.add(Vector2f(after.time, getAfterValue(after)))
					if (useLastPoint) pts.add(Vector2f(afterPlus.time, getApValue(afterPlus)))

					val adjWeight = weight + (if (useFirstPoint) 1f else 0f)
					return lerpSplineCurve(pts, adjWeight / (pts.size - 1))
				}

				val cx = buildAxis(
					{ it.evaluatePost().x },
					{ it.evaluatePost().x },
					{ it.evaluatePre().x },
					{ it.evaluatePre().x }
				)
				val cy = buildAxis(
					{ it.evaluatePost().y },
					{ it.evaluatePost().y },
					{ it.evaluatePre().y },
					{ it.evaluatePre().y }
				)
				val cz = buildAxis(
					{ it.evaluatePost().z },
					{ it.evaluatePost().z },
					{ it.evaluatePre().z },
					{ it.evaluatePre().z }
				)
				Vector3f(cx, cy, cz)
			}

			else -> {
				val from = before.evaluatePost()
				val to = after.evaluatePre()
				val out = Vector3f()
				out.set(from).lerp(to, weight)
				out
			}
		}
	}

	/**
	 * 分段 Catmull-Rom 样条求值（匹配 eyelib Curves.lerpSplineCurve）
	 *
	 * @param points 控制点序列 (x=时间, y=值)
	 * @param time 归一化位置 [0, 1]，0=第一个点，1=最后一个点
	 * @return 样条上的值
	 */
	private fun lerpSplineCurve(points: List<Vector2f>, time: Float): Float {
		val p = (points.size - 1) * time
		val intPoint = floor(p.toDouble()).toInt()
		val weight = p - intPoint

		val idx0 = if (intPoint == 0) 0 else (intPoint - 1).coerceAtMost(points.size - 1)
		val idx1 = intPoint.coerceAtMost(points.size - 1)
		val idx2 = if (intPoint > points.size - 2) points.size - 1 else intPoint + 1
		val idx3 = if (intPoint > points.size - 3) points.size - 1 else intPoint + 2

		return EasingTypes.catmullRom(
			weight.toDouble(),
			points[idx0].y.toDouble(),
			points[idx1].y.toDouble(),
			points[idx2].y.toDouble(),
			points[idx3].y.toDouble()
		).toFloat()
	}
}
