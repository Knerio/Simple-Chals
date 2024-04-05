package de.derioo.chals.server;

import com.destroystokyo.paper.brigadier.BukkitBrigadierCommandSource;
import com.destroystokyo.paper.event.brigadier.CommandRegisteredEvent;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import de.derioo.chals.server.api.ChalsAPI;
import de.derioo.chals.server.api.Unsafe;
import de.derioo.chals.server.api.types.Mod;
import net.kyori.adventure.text.Component;
import net.minecraft.commands.CommandSourceStack;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_20_R3.CraftServer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.InvalidPluginException;
import org.bukkit.plugin.java.JavaPlugin;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;

import java.io.IOException;
import java.util.function.Consumer;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

@DefaultQualifier(NonNull.class)
public final class ServerCore extends JavaPlugin implements Listener {
  @Override
  public void onEnable() {
    this.getServer().getPluginManager().registerEvents(this, this);

    Unsafe.setApi(new ChalsAPI());

    registerPluginBrigadierCommand("mod", (builder -> {
      RequiredArgumentBuilder<CommandSourceStack, String> modNameBuilder = argument("modName", StringArgumentType.word()).suggests((ctx, suggestionsBuilder) -> {
        for (Mod cachedMod : Unsafe.getApi().getCachedMods()) {
          suggestionsBuilder.suggest(cachedMod.getName());
        }
        return suggestionsBuilder.buildFuture();
      });
      builder.
        requires(stack -> stack.getBukkitSender().hasPermission("ss.manage"))
        .then(modNameBuilder.then(literal("start")).executes(commandContext -> {
          for (Mod mod : Unsafe.getApi().getCachedMods()) {
            if (!mod.getName().equalsIgnoreCase(commandContext.getArgument("modName", String.class))) continue;
            Bukkit.getScheduler().runTaskAsynchronously(getPlugin(getClass()), () -> {
              try {
                mod.load();
              } catch (IOException | InvalidPluginException | InvalidDescriptionException e) {
                commandContext.getSource().getBukkitSender().sendMessage(Component.text("Error occurred while loading mod"));
                throw new RuntimeException(e);
              }
              commandContext.getSource().getBukkitSender().sendMessage(Component.text("Loaded and started mod!"));
            });
          }
          return Command.SINGLE_SUCCESS;
        }));
    }));

  }


  public void registerPluginBrigadierCommand(final String label, final Consumer<LiteralArgumentBuilder<CommandSourceStack>> command) {
    final PluginBrigadierCommand pluginBrigadierCommand = new PluginBrigadierCommand(this, label, command);
    this.getServer().getCommandMap().register(this.getName(), pluginBrigadierCommand);
    ((CraftServer) this.getServer()).syncCommands();
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  @EventHandler
  public void onCommandRegistered(final CommandRegisteredEvent<BukkitBrigadierCommandSource> event) {
    if (!(event.getCommand() instanceof PluginBrigadierCommand pluginBrigadierCommand)) {
      return;
    }
    final LiteralArgumentBuilder<CommandSourceStack> node = literal(event.getCommandLabel());
    pluginBrigadierCommand.command().accept(node);
    event.setLiteral((LiteralCommandNode) node.build());
  }
}
