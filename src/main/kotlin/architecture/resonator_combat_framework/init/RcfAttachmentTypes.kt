package architecture.resonator_combat_framework.init

import architecture.resonator_combat_framework.module.entity_animation.animation.molang.MolangData
import architecture.resonator_combat_framework.module.entity_animation.animation.molang.MolangData.Companion.initEntityQueries
import architecture.resonator_combat_framework.module.entity_state_machine.holder.EntityStateHolder
import architecture.resonator_combat_framework.module.entity_state_machine.holder.MobStateHolder
import architecture.resonator_combat_framework.module.entity_state_machine.holder.PlayerStateHolder
import architecture.resonator_combat_framework.util.RcfUtil.modRegister
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.neoforged.neoforge.attachment.AttachmentType
import net.neoforged.neoforge.registries.DeferredHolder
import net.neoforged.neoforge.registries.DeferredRegister
import net.neoforged.neoforge.registries.NeoForgeRegistries

object RcfAttachmentTypes {
	@JvmField
	val REGISTRY: DeferredRegister<AttachmentType<*>> = modRegister(NeoForgeRegistries.ATTACHMENT_TYPES)

	@JvmField
	val MOLANG_DATA: DeferredHolder<AttachmentType<*>, AttachmentType<MolangData>> =
		REGISTRY.register("molang_data") { ->
			AttachmentType.builder { holder ->
				val data = MolangData()
				if (holder is Entity) {
					data.initEntityQueries(holder)
					return@builder data
				}
				if (holder is Level) {
					return@builder data
				}
				throw IllegalArgumentException("MolangData can only be attached to Level or Entity. Unsupported holder type: ${holder?.javaClass}")
			}.build()
		}

	@JvmField
	val STATE_HOLDER: DeferredHolder<AttachmentType<*>, AttachmentType<EntityStateHolder<*>>> =
		REGISTRY.register("entity_state_holder") { ->
			AttachmentType.serializable { holder ->
				when (holder) {
					// TODO 应用事件来自定义注册
					is Player -> return@serializable PlayerStateHolder(holder)
					is Mob -> return@serializable MobStateHolder(holder)
					is LivingEntity -> return@serializable EntityStateHolder(holder)
					else -> throw IllegalArgumentException("StateHolder can only be attached to LivingEntity. Unsupported: ${holder?.javaClass}")
				}
			}.copyOnDeath().build()
		}
}
