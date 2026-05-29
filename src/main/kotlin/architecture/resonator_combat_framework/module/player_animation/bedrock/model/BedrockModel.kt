package architecture.resonator_combat_framework.module.player_animation.bedrock.model

// Bedrock 模型数据模型。对应 geo.json 格式，包含骨骼(BedrockBone)和立方体(BedrockCube)，不含 UV/纹理数据

import org.joml.Vector3f

/**
 * 基岩模型数据。
 * 对应 geo.json 中 minecraft:geometry[] 的单个条目。
 */
data class BedrockModel(
	/** 模型标识符，如 "geometry.default" */
	val identifier: String,
	/** 骨骼列表 */
	val bones: List<BedrockBone>
)

/**
 * 骨骼（相当于一个组）。
 * 可以包含子骨骼、立方体（cubes）。
 */
data class BedrockBone(
	/** 骨骼名称 */
	val name: String,
	/** 父骨骼名称，null 表示根骨 */
	val parent: String? = null,
	/** 轴点（旋转/缩放中心），单位像素 */
	val pivot: Vector3f = Vector3f(),
	/** 旋转角度 */
	val rotation: Vector3f = Vector3f(),
	/** 该骨骼包含的立方体 */
	val cubes: List<BedrockCube> = emptyList()
)

/**
 * 立方体。
 * 只有几何信息（位置、大小、轴点、旋转、膨胀/镜像），不包含 UV/纹理数据。
 */
data class BedrockCube(
	/** 原点坐标（最小角），单位像素 */
	val origin: Vector3f,
	/** 尺寸 */
	val size: Vector3f,
	/** 轴点 */
	val pivot: Vector3f = Vector3f(),
	/** 旋转角度 */
	val rotation: Vector3f = Vector3f(),
	/** 膨胀值（使模型稍微变大/缩小，不影响碰撞体积） */
	val inflate: Float = 0f,
	/** 是否镜像 */
	val mirror: Boolean = false
)

