package architecture.resonator_combat_framework.module.player_animation.command

import architecture.resonator_combat_framework.module.player_animation.util.EyeLibUtil
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.SuggestionProvider
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.SharedSuggestionProvider
import net.neoforged.fml.loading.FMLEnvironment
import java.util.concurrent.CompletableFuture

// 从 AnimationManager 获取所有已加载的动画 ID 作为命令补全
object AnimationIdArgumentProvider : SuggestionProvider<CommandSourceStack> {
	override fun getSuggestions(
		ctx: CommandContext<CommandSourceStack>,
		builder: SuggestionsBuilder
	): CompletableFuture<Suggestions> {
		val anims = EyeLibUtil.getAnimationManager(FMLEnvironment.dist.isClient).allData
		return SharedSuggestionProvider.suggest(anims.keys, builder)
	}
}
