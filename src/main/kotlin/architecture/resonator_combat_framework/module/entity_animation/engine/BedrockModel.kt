package architecture.resonator_combat_framework.module.entity_animation.engine

data class BedrockModel(
	val identifier: String,
	val bones: List<BedrockModelBone> = emptyList()
)

data class BedrockModelBone(
	val name: String,
	val pivot: List<Float> = listOf(0f, 0f, 0f),
	val rotation: List<Float> = listOf(0f, 0f, 0f)
)
