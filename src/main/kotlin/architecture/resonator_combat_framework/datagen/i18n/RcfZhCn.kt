package architecture.resonator_combat_framework.datagen.i18n

import architecture.goldenboughs_lib.datagen.i18n.DatagenI18n
import architecture.resonator_combat_framework.config.RcfConfig
import architecture.resonator_combat_framework.core.RcfConstants
import net.minecraft.data.PackOutput
import net.minecraft.sounds.SoundEvent
import net.minecraft.world.effect.MobEffect
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.ai.attributes.Attribute
import net.minecraft.world.item.Item
import net.neoforged.fml.loading.FMLEnvironment
import org.jetbrains.annotations.ApiStatus
import java.util.function.Supplier

@ApiStatus.Internal
class RcfZhCn(output: PackOutput) : DatagenI18n(output, RcfConstants.ID, "zh_cn") {
	override fun addTranslations() {
		addPackDescription(RcfConstants.ID, "金枝")

		add("${RcfConstants.ID}.command.play_anim", "已给%s播放%s动画")
		add("${RcfConstants.ID}.command.stop_anim", "已停止%s的动画")

		add(RcfConfig.CLIENT.itemSwitchingAnimation, "物品切换动画")
	}

	companion object {
		@JvmStatic
		private val ITEMS: MutableMap<Supplier<out Item>, String> = HashMap()

		@JvmStatic
		private val MOB_EFFECT: MutableMap<Supplier<out MobEffect>, String> = HashMap()

		@JvmStatic
		private val ATTRIBUTE: MutableMap<Supplier<out Attribute>, String> = HashMap()

		@JvmStatic
		private val SOUND_EVENT: MutableMap<Supplier<out SoundEvent>, String> = HashMap()

		@JvmStatic
		private val ENTITY_TYPES: MutableMap<Supplier<out EntityType<*>>, String> = HashMap()

		@JvmStatic
		private val MAP: MutableMap<String, String> = HashMap()

		@JvmStatic
		fun addI18nText(zhCn: String, key: String) {
			if (!FMLEnvironment.production) {
				MAP[key] = zhCn
			}
		}

		@JvmStatic
		fun addI18nItemText(zhName: String, deferredItem: Supplier<out Item>) {
			if (!FMLEnvironment.production) {
				ITEMS[deferredItem] = zhName
			}
		}

		@JvmStatic
		fun addI18nMobEffectText(zhName: String, supplier: Supplier<out MobEffect>) {
			if (!FMLEnvironment.production) {
				MOB_EFFECT[supplier] = zhName
			}
		}

		@JvmStatic
		fun addI18nAttributeText(zhName: String, supplier: Supplier<out Attribute>) {
			if (!FMLEnvironment.production) {
				ATTRIBUTE[supplier] = zhName
			}
		}

		@JvmStatic
		fun addI18nSoundEventText(zhName: String, supplier: Supplier<out SoundEvent>) {
			if (!FMLEnvironment.production) {
				SOUND_EVENT[supplier] = zhName
			}
		}

		@JvmStatic
		fun addI18nEntityTypeText(zhName: String, supplier: Supplier<out EntityType<*>>) {
			if (!FMLEnvironment.production) {
				ENTITY_TYPES[supplier] = zhName
			}
		}
	}
}
