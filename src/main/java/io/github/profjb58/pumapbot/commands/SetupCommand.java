package io.github.profjb58.pumapbot.commands;

import io.github.profjb58.pumapbot.db.DatabaseManager;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;

public class SetupCommand extends Command {

    private final DatabaseManager dbManager;

    public SetupCommand(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    @Override
    protected void onCommand(@NotNull SlashCommandInteractionEvent interaction) {
        var mapsChannel = interaction.getOption("map_channel", OptionMapping::getAsChannel);

        if(mapsChannel != null && interaction.getGuild() != null)
            try {
                dbManager.insertServerDetails(interaction.getGuild(), mapsChannel.getIdLong());
                interaction.getHook().sendMessage("✅ **Successfully setup PU-Mappugāru!**").queue();
            } catch (SQLException e) {
                interaction.getHook().sendMessage("⚠️ Failed to initialize bot. A database error occurred").queue();
                e.printStackTrace();
            }
        else
            interaction.getHook().sendMessage("⚠️ Please include a channel for storing maps").queue();
    }

    @Override
    public CommandData getCommandData() {
        return Commands.slash("setup", "Sets-up the PU Map Bot for the first time")
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MODERATE_MEMBERS))
                .addOption(OptionType.CHANNEL, "map_channel", "Channel to display maps in");
    }
}
