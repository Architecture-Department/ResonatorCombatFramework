package architecture.resonator_combat_framework.util

import architecture.goldenboughs_lib.client.model.GeoModelExpand
import architecture.goldenboughs_lib.client.model.GeoModelExpand.Companion.texturePath
import architecture.resonator_combat_framework.core.Rcf.getSparkModuleRl
import architecture.resonator_combat_framework.core.RcfConstants
import net.minecraft.resources.ResourceLocation
import software.bernie.geckolib.animatable.GeoAnimatable
import software.bernie.geckolib.model.GeoModel

class ModelUtil {
	/**
	 * 模型构建器 - 使用建造者模式创建 GeoModel
	 */
	class ModelBuilder<T : GeoAnimatable>(
		private val namespace: String
	) {
		private var path: String? = null

		/**
		 * 设置路径
		 */
		fun path(path: String): ModelBuilder<T> {
			this.path = path
			return this
		}

		/**
		 * 设置饰品路径（自动添加 curio/ 前缀）
		 */
		fun curioPath(path: String): ModelBuilder<T> {
			this.path = "curio/$path"
			return this
		}

		/**
		 * 设置物品路径（自动添加 item/ 前缀）
		 */
		fun itemPath(path: String): ModelBuilder<T> {
			this.path = "item/$path"
			return this
		}

		/**
		 * 设置实体路径（自动添加 entity/ 前缀）
		 */
		fun entityPath(path: String): ModelBuilder<T> {
			this.path = "entity/$path"
			return this
		}

		/**
		 * 设置护甲路径（自动添加 armor/ 前缀）
		 */
		fun armorPath(path: String): ModelBuilder<T> {
			this.path = "armor/$path"
			return this
		}

		/**
		 * 构建 GeoModel
		 */
		fun build(): GeoModel<T> {
			checkNotNull(namespace) { "Namespace must be set before building" }
			checkNotNull(path) { "Path must be set before building" }

			return GeoModelExpand(
				this.modelsRl,
				this.texturePath,
				this.animationsRl
			)
		}

		val texturePath: ResourceLocation
			get() = texturePath(
				ResourceLocation.fromNamespaceAndPath(
					namespace,
					path!!
				)
			)

		val modelsRl: ResourceLocation
			get() = getSparkModuleRl(
				namespace,
				RcfConstants.MODELS,
				path!!
			)

		val animationsRl: ResourceLocation
			get() = getSparkModuleRl(
				namespace,
				RcfConstants.ANIMATIONS,
				"$path/$path"
			)
	}
}
