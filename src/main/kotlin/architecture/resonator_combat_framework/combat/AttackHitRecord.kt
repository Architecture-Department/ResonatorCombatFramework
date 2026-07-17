package architecture.resonator_combat_framework.combat

import architecture.resonator_combat_framework.init.RcfAttachmentTypes
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.LivingEntity
import java.util.*

/**
 * 攻击命中记录 —— 按实体隔离，按阶段分组。
 *
 * 每个实体通过 Attachment 持有自己的实例，替代原本 [AttackAnimationAction] 中的静态 [HitRecord]。
 * 每阶段（phaseIdx）独立记录 tried/hit 的实体 UUID，阶段结束时调用 [removeGroup] 清理。
 */
class AttackHitRecord {
	private val tried = mutableMapOf<ResourceLocation, HashMap<Int, MutableSet<UUID>>>()
	private val hit = mutableMapOf<ResourceLocation, HashMap<Int, MutableSet<UUID>>>()

	fun getTried(id: ResourceLocation, phaseIdx: Int): MutableSet<UUID> = tried[id]?.get(phaseIdx) ?: mutableSetOf()
	fun getHit(id: ResourceLocation, phaseIdx: Int): MutableSet<UUID> = hit[id]?.get(phaseIdx) ?: mutableSetOf()

	/** 获取该阶段的所有已尝试 entity，不存在则返回空集 */
	fun getTriedOrEmpty(id: ResourceLocation, phaseIdx: Int): Set<UUID> = tried[id]?.get(phaseIdx) ?: emptySet()
	fun getHitOrEmpty(id: ResourceLocation, phaseIdx: Int): Set<UUID> = hit[id]?.get(phaseIdx) ?: emptySet()

	/** 返回当前有记录的 phase 索引集合 */
	fun getActivePhases(id: ResourceLocation): Set<Int> = tried[id]?.keys ?: emptySet()

	/** 移除指定阶段的记录（阶段结束时调用） */
	fun removeGroup(id: ResourceLocation, phaseIdx: Int) {
		tried.remove(id)?.remove(phaseIdx)
		hit.remove(id)?.remove(phaseIdx)
	}

	fun clear() {
		tried.clear()
		hit.clear()
	}

	companion object {
		@JvmStatic
		fun of(entity: LivingEntity): AttackHitRecord {
			return entity.getData(RcfAttachmentTypes.ATTACK_HIT_RECORD)
		}

		@JvmStatic
		fun has(entity: LivingEntity): Boolean {
			return entity.hasData(RcfAttachmentTypes.ATTACK_HIT_RECORD)
		}

		@JvmStatic
		fun clear(entity: LivingEntity) {
			if (!has(entity)) return
			of(entity).clear()
		}
	}
}
