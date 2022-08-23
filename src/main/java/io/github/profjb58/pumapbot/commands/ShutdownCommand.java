package io.github.profjb58.pumapbot.commands;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.jetbrains.annotations.NotNull;

public class ShutdownCommand extends Command {

    public ShutdownCommand() {
        this.deferReply = false;
        this.isEphemeral = false;
    }

    @Override
    protected void onCommand(@NotNull SlashCommandInteractionEvent interaction) {
        interaction.reply("🛑 Bot shutting down...").complete();
        interaction.getJDA().shutdown();
    }

    @Override
    public CommandData getCommandData() {
        return Commands.slash("shutdown", "Shutdown the PU Map bot")
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MODERATE_MEMBERS));
    }
}
