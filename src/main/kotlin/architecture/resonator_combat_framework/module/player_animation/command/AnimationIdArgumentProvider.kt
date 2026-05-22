package architecture.resonator_combat_framework.module.player_animation.command

import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.SuggestionProvider
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import io.github.tt432.eyelib.Eyelib
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.SharedSuggestionProvider
import java.util.concurrent.CompletableFuture

// 从 AnimationManager 获取所有已加载的动画 ID 作为命令补全
object AnimationIdArgumentProvider : SuggestionProvider<CommandSourceStack> {
	override fun getSuggestions(
		ctx: CommandContext<CommandSourceStack>,
		builder: SuggestionsBuilder
	): CompletableFuture<Suggestions> {
		val anims = Eyelib.getAnimationManager().allData
		return SharedSuggestionProvider.suggest(anims.keys, builder)
	}
}
