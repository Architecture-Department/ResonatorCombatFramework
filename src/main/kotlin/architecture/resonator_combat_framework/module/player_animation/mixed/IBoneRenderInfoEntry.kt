package architecture.resonator_combat_framework.module.player_animation.mixed

import io.github.tt432.eyelib.client.render.bone.BoneRenderInfoEntry

interface IBoneRenderInfoEntry {
	var renderScalaEmpty: Boolean
		get() = `resonator_combat_framework$isRenderScalaEmpty`()
		set(value) {
			`resonator_combat_framework$setRenderScalaEmpty`(value)
		}

	var renderPositionEmpty: Boolean
		get() = `resonator_combat_framework$isRenderPositionEmpty`()
		set(value) {
			`resonator_combat_framework$setRenderPositionEmpty`(value)
		}

	var renderRotationEmpty: Boolean
		get() = `resonator_combat_framework$isRenderRotationEmpty`()
		set(value) {
			`resonator_combat_framework$setRenderRotationEmpty`(value)
		}

	fun `resonator_combat_framework$isRenderScalaEmpty`(): Boolean

	fun `resonator_combat_framework$isRenderPositionEmpty`(): Boolean

	fun `resonator_combat_framework$isRenderRotationEmpty`(): Boolean

	fun `resonator_combat_framework$setRenderScalaEmpty`(value: Boolean)

	fun `resonator_combat_framework$setRenderPositionEmpty`(value: Boolean)

	fun `resonator_combat_framework$setRenderRotationEmpty`(value: Boolean)

	companion object {
		@JvmStatic
		fun of(boneRenderInfoEntry: BoneRenderInfoEntry): IBoneRenderInfoEntry =
			boneRenderInfoEntry as IBoneRenderInfoEntry
	}
}