package architecture.resonator_combat_framework.init

import architecture.resonator_combat_framework.event.CreateEntityStateHolderEvent
import architecture.resonator_combat_framework.module.animation.molang.MolangData
import architecture.resonator_combat_framework.module.animation.molang.MolangData.Companion.initEntityQueries
import architecture.resonator_combat_framework.module.combat.AttackHitRecord
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

/**
 * RCF Attachment 类型注册 —— 注册所有附加到实体/世界的自定义数据。
 *
 * 每个 Attachment 通过 [DeferredRegister] 懒加载注册，在首次访问时自动构造实例。
 * 支持的 Attachment：
 * - [MOLANG_DATA]：MoLang 表达式上下文数据，附加到 Entity 或 Level
 * - [STATE_HOLDER]：实体战斗状态持有者，附加到 LivingEntity
 * - [ATTACK_HIT_RECORD]：攻击命中记录，附加到 LivingEntity
 */
object RcfAttachmentTypes {
	@JvmField
	val REGISTRY: DeferredRegister<AttachmentType<*>> = modRegister(NeoForgeRegistries.ATTACHMENT_TYPES)

	/** MoLang 表达式上下文数据 —— 附加到 Entity 或 Level，用于动画表达式求值 */
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

	/** 实体战斗状态持有者 —— 附加到 LivingEntity，管理动作控制器和状态标志 */
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

	/** 攻击命中记录 —— 附加到 LivingEntity，按阶段记录已尝试/已命中的实体 UUID */
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
