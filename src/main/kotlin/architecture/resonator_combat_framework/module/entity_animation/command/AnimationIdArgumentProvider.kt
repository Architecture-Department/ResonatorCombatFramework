package architecture.resonator_combat_framework.module.entity_animation.command

import architecture.resonator_combat_framework.module.entity_animation.registry.KeyframeAnimationRegistry
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
		val animIds = KeyframeAnimationRegistry.getInstance(true).getAll().keys
		return SharedSuggestionProvider.suggest(animIds.map { it.path }, builder)
	}
}

