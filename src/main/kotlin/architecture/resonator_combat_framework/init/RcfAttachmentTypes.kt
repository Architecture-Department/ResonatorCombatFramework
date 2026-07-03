package architecture.resonator_combat_framework.init

import architecture.resonator_combat_framework.event.CreateEntityStateHolderEvent
import architecture.resonator_combat_framework.module.collision.CollisionEntityData
import architecture.resonator_combat_framework.module.animation.molang.MolangData
import architecture.resonator_combat_framework.module.animation.molang.MolangData.Companion.initEntityQueries
import architecture.resonator_combat_framework.module.state_machine.holder.EntityStateHolder
import architecture.resonator_combat_framework.module.state_machine.holder.MobStateHolder
import architecture.resonator_combat_framework.module.state_machine.holder.PlayerStateHolder
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
			AttachmentType.builder { holder ->
				if (holder !is LivingEntity) {
					throw IllegalArgumentException("StateHolder can only be attached to LivingEntity. Unsupported: ${holder?.javaClass}")
				}

				val function = CreateEntityStateHolderEvent.getAll()[holder.type]
				if (function != null) {
					return@builder function(holder)
				}

				when (holder) {
					is Player -> return@builder PlayerStateHolder(holder)
					is Mob -> return@builder MobStateHolder(holder)
					is LivingEntity -> return@builder EntityStateHolder(holder)
				}
			}.build()
		}

	@JvmField
	val ENTITY_COLLISION: DeferredHolder<AttachmentType<*>, AttachmentType<CollisionEntityData>> =
		REGISTRY.register("entity_collision") { ->
			AttachmentType.builder { holder ->
				if (holder !is Entity) throw IllegalArgumentException("CollisionEntity can only be attached to LivingEntity. Unsupported: ${holder?.javaClass}")
				return@builder CollisionEntityData(holder)
			}.build()
		}
}
