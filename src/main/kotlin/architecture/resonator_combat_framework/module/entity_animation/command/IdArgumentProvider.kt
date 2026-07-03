package architecture.resonator_combat_framework.module.entity_animation.command

import architecture.resonator_combat_framework.module.entity_animation.registry.KeyframeAnimationRegistry
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.SuggestionProvider
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.SharedSuggestionProvider
import java.util.concurrent.CompletableFuture

/**
 * 动画 ID 补全提供者，为命令参数提供已注册关键帧动画 ID 的自动补全建议。
 * 建议内容为动画资源路径的 [ResourceLocation.path] 部分。
 */
object IdArgumentProvider : SuggestionProvider<CommandSourceStack> {
	override fun getSuggestions(
		ctx: CommandContext<CommandSourceStack>,
		builder: SuggestionsBuilder
	): CompletableFuture<Suggestions> {
		val animIds = KeyframeAnimationRegistry.findAll().keys
		return SharedSuggestionProvider.suggest(animIds.map { it.toString() }, builder)
	}
}
