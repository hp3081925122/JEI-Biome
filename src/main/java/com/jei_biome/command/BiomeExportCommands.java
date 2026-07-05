package com.jei_biome.command;

import com.jei_biome.Jei_biome;
import com.jei_biome.export.BiomeBlockIndexExporter;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.nio.file.Path;

public final class BiomeExportCommands {

    private BiomeExportCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("jei_biome")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("export")
                        .executes(BiomeExportCommands::export)));
    }

    private static int export(CommandContext<CommandSourceStack> context) {
        try {
            Path path = BiomeBlockIndexExporter.export(context.getSource().getServer());
            context.getSource().sendSuccess(() -> Component.translatable("jei_biome.command.export.success", path.toString()), true);
            return 1;
        } catch (Exception exception) {
            Jei_biome.LOGGER.error("JEI Biome export failed", exception);
            String message = exception.getMessage();
            context.getSource().sendFailure(Component.translatable("jei_biome.command.export.failure", exception.getClass().getSimpleName() + (message == null ? "" : ": " + message)));
            return 0;
        }
    }
}
