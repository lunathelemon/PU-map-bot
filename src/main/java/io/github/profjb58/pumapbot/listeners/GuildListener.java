package io.github.profjb58.pumapbot.listeners;

import io.github.profjb58.pumapbot.PUMapBot;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;

public class GuildListener extends ListenerAdapter {

    private final PUMapBot instance;

    public GuildListener(PUMapBot instance) {
        this.instance = instance;
    }

    @Override
    public void onGuildReady(@NotNull GuildReadyEvent event) {
        instance.getCommandManager().addCommands(event); // Add commands
    }

    @Override
    public void onGuildJoin(@NotNull GuildJoinEvent event) {
        instance.getCommandManager().addCommands(event); // Add commands

        var guildOwner = event.getGuild().getOwner();
        if(guildOwner != null) {
            var privateChannel = guildOwner.getUser().openPrivateChannel().complete();
            privateChannel.sendMessage("""
            **Thank you for adding PU-Mappugāru (PU Map Bot)**
            To setup the bot type `/setup` from within the server and specify the channel you want to display the maps
            """).queue();
        }
    }

    @Override
    public void onGuildLeave(@NotNull GuildLeaveEvent event) {
        try {
            instance.getDBManager().deleteServerDetails(event.getGuild());
        } catch (SQLException e) {
            PUMapBot.LOGGER.error("Failed to delete database server entry for guild with name: " + event.getGuild().getName() + " and id: " + event.getGuild().getIdLong(), e);
        }
    }
}
