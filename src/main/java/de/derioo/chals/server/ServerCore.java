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
import eu.thesimplecloud.api.CloudAPI;
import net.kyori.adventure.text.Component;
import net.minecraft.commands.CommandSourceStack;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_20_R3.CraftServer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.InvalidPluginException;
import org.bukkit.plugin.java.JavaPlugin;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;

import java.io.File;
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
    registerModCommand();
    registerWordResetCommand();
  }

  private void registerWordResetCommand() {
    registerPluginBrigadierCommand("worldreset", builder -> {
      builder.requires(stack -> stack.getBukkitSender().hasPermission("world.reset"))
        .executes(ctx -> {
          clearDir(new File(CloudAPI.getInstance().getTemplateManager().getTemplateByName("Bukkit").getDirectory() + "/world"));
          clearDir(new File(CloudAPI.getInstance().getTemplateManager().getTemplateByName("Bukkit").getDirectory() + "/world_the_nether"));
          clearDir(new File(CloudAPI.getInstance().getTemplateManager().getTemplateByName("Bukkit").getDirectory() + "/world_the_end"));


          return Command.SINGLE_SUCCESS;
        });
    });
  }



  private void clearDir(File file) {
    if (file.isDirectory()) {
      for (File listFile : file.listFiles()) {
        clearDir(listFile);
      }
    }
    file.delete();
  }

  @EventHandler
  public void onWorldLoad(WorldLoadEvent event) {
    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "cloud copy Bukkit " + event.getWorld().getName());
  }


  private void registerModCommand() {
    registerPluginBrigadierCommand("mod", (builder -> {
      RequiredArgumentBuilder<CommandSourceStack, String> modNameBuilder = argument("modName", StringArgumentType.word()).suggests((ctx, suggestionsBuilder) -> {
        for (Mod cachedMod : Unsafe.getApi().mods()) {
          suggestionsBuilder.suggest(cachedMod.getName());
        }
        return suggestionsBuilder.buildFuture();
      });
      builder.
        requires(stack -> stack.getBukkitSender().hasPermission("sc.manage"))
        .then(modNameBuilder.then(literal("stop").executes(ctx -> {
          for (Mod mod : Unsafe.getApi().mods()) {
            if (!mod.getName().equalsIgnoreCase(ctx.getArgument("modName", String.class))) continue;
            Bukkit.getScheduler().runTaskAsynchronously(getPlugin(getClass()), () -> {
              mod.delete();

              ctx.getSource().getBukkitSender().sendMessage(Component.text("Unloaded mod"));
            });
          }


          return Command.SINGLE_SUCCESS;
        })).then(literal("start").executes(commandContext -> {
          for (Mod mod : Unsafe.getApi().mods()) {
            if (!mod.getName().equalsIgnoreCase(commandContext.getArgument("modName", String.class))) continue;
            Bukkit.getScheduler().runTaskAsynchronously(getPlugin(getClass()), () -> {
              mod.enable();

              commandContext.getSource().getBukkitSender().sendMessage(Component.text("Loaded and started mod!"));
            });
          }
          return Command.SINGLE_SUCCESS;
        })));
    }));
  }

  @Override
  public void onDisable() {
    Unsafe.getApi().mods().forEach(Mod::delete);
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
