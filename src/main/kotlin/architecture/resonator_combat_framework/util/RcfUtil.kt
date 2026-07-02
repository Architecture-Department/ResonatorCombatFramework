package architecture.resonator_combat_framework.util

import architecture.goldenboughs_lib.util.LibUtil
import net.minecraft.core.Registry
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.neoforged.fml.loading.LoadingModList
import net.neoforged.neoforge.registries.DeferredRegister
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.jetbrains.annotations.Contract

/**
 * RCF 工具类 —— 提供 Mod ID、日志、资源位置创建、注册表快捷方法等通用工具。
 */
object RcfUtil {
	/** Mod ID */
	const val ID: String = "resonator_combat_framework"

	/** Mod 名称 */
	const val NAME: String = "ResonatorCombatFramework"

	@JvmStatic
	val IRSTPERSON_LOADED = LoadingModList.get().getModFileById("firstperson") != null

	@JvmStatic
	val GECKOLIB_LOADED = LoadingModList.get().getModFileById("geckolib") != null

	@JvmStatic
	val PARTICLESTORM_LOADED = LoadingModList.get().getModFileById("particlestorm") != null

	@JvmField
	val LOGGER: Logger = LogManager.getLogger(ID)

	@JvmStatic
	@Contract("_ -> new")
	fun modRl(name: String): ResourceLocation {
		return LibUtil.rlOf(ID, name)
	}

	@JvmStatic
	@Contract(pure = true)
	fun modRlText(name: String): String {
		return "$ID:$name"
	}

	@JvmStatic
	fun <T> modRegister(registry: Registry<T>): DeferredRegister<T> {
		return DeferredRegister.create<T>(registry, ID)
	}

	@JvmStatic
	fun <T> modRegister(registry: ResourceKey<Registry<T>>): DeferredRegister<T> {
		return DeferredRegister.create<T>(registry, ID)
	}

	@JvmStatic
	fun getSparkModuleRl(namespace: String, typeName: String, path: String): ResourceLocation {
		return LibUtil.rlOf(namespace, "spark_modules/$typeName/$path")
	}
}
