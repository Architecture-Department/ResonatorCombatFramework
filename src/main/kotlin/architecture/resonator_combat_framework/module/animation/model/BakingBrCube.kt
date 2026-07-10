package architecture.resonator_combat_framework.module.animation.model

import com.google.gson.JsonElement
import org.joml.Vector3f
import org.joml.Vector3fc

/**
 * 烘培立方体数据——模型中的单个立方体定义。
 *
 * @property inflate 膨胀值
 * @property origin 原点坐标
 * @property size 尺寸
 * @property rotation 旋转
 */
data class BakingBrCube
@JvmOverloads
constructor(
	val inflate: Float = 0f,
	val origin: Vector3fc = Vector3f(),
	val size: Vector3fc = Vector3f(),
	val rotation: Vector3fc = Vector3f()
) {
	companion object {
		/**
		 * 从 JSON 数组解析 [BakingBrCube] 列表。
		 *
		 * @param json JSON 元素
		 * @return 立方体列表
		 */
		@JvmStatic
		fun parses(json: JsonElement): List<BakingBrCube> {
			val list = mutableListOf<BakingBrCube>()
			json.asJsonArray.forEach {
				val obj = it.asJsonObject
				val origin = obj.getAsJsonArray("origin")?.map { it1 -> it1.asFloat } ?: listOf(0f, 0f, 0f)
				val size = obj.getAsJsonArray("size")?.map { it1 -> it1.asFloat } ?: listOf(0f, 0f, 0f)
				val inflate = obj.get("inflate")?.asFloat ?: 0f
				val rotation = obj.getAsJsonArray("size")?.map { it1 -> it1.asFloat } ?: listOf(0f, 0f, 0f)
				list.add(
					BakingBrCube(
						inflate,
						Vector3f(origin[0], origin[1], origin[2]),
						Vector3f(size[0], size[1], size[2]),
						Vector3f(rotation[0], rotation[1], rotation[2]),
					)
				)
			}
			return list
		}
	}
}