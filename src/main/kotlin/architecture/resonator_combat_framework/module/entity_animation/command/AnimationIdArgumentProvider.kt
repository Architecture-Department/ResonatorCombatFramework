// 命令补全提供器。为 /player_anim 命令提供已加载的 Bedrock 动画 ID 列表作为 tab 补全
package architecture.resonator_combat_framework.module.entity_animation.command

// 命令补全提供器。为 /player_anim 命令提供已加载的 Bedrock 动画 ID 列表作为 tab 补全

import architecture.resonator_combat_framework.module.entity_animation.registry.BedrockAnimationRegistry
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.SuggestionProvider
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.SharedSuggestionProvider
import java.util.concurrent.CompletableFuture

object AnimationIdArgumentProvider : SuggestionProvider<CommandSourceStack> {
	override fun getSuggestions(
		ctx: CommandContext<CommandSourceStack>,
		builder: SuggestionsBuilder
	): CompletableFuture<Suggestions> {
		val animIds = BedrockAnimationRegistry.getInstance(true).getAllAnimIds()
		return SharedSuggestionProvider.suggest(animIds, builder)
	}
}

