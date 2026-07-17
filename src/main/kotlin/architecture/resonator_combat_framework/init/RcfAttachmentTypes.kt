package architecture.resonator_combat_framework.init

import architecture.resonator_combat_framework.combat.ActionHolder
import architecture.resonator_combat_framework.combat.AttackHitRecord
import architecture.resonator_combat_framework.event.definition.CreateActionHolderEvent
import architecture.resonator_combat_framework.event.definition.CreateEntityStateHolderEvent
import architecture.resonator_combat_framework.molang.MolangDataHolder
import architecture.resonator_combat_framework.molang.MolangDataHolder.Companion.initEntityQueries
import architecture.resonator_combat_framework.state.EntityStateHolder
import architecture.resonator_combat_framework.state.MobStateHolder
import architecture.resonator_combat_framework.state.PlayerStateHolder
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
	val MOLANG_DATA: DeferredHolder<AttachmentType<*>, AttachmentType<MolangDataHolder>> =
		REGISTRY.register("molang_data") { ->
			AttachmentType.builder { holder ->
				val data = MolangDataHolder()
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
	val ENTITY_STATE_HOLDER: DeferredHolder<AttachmentType<*>, AttachmentType<EntityStateHolder<*>>> =
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
	val ACTION_HOLDER: DeferredHolder<AttachmentType<*>, AttachmentType<ActionHolder<*>>> =
		REGISTRY.register("action_holder") { ->
			AttachmentType.builder { holder ->
				if (holder !is LivingEntity) {
					throw IllegalArgumentException("ActionHolder can only be attached to LivingEntity. Unsupported: ${holder?.javaClass}")
				}

				val function = CreateActionHolderEvent.getAll()[holder.type]
				if (function != null) {
					return@builder function(holder)
				}
				return@builder ActionHolder(holder)
			}.build()
		}

	@JvmField
	val ATTACK_HIT_RECORD: DeferredHolder<AttachmentType<*>, AttachmentType<AttackHitRecord>> =
		REGISTRY.register("attack_hit_record") { ->
			AttachmentType.builder { holder ->
				if (holder !is LivingEntity) {
					throw IllegalArgumentException("AttackHitRecord can only be attached to LivingEntity. Unsupported: ${holder?.javaClass}")
				}
				AttackHitRecord()
			}.build()
		}
}
