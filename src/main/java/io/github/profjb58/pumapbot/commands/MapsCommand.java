package io.github.profjb58.pumapbot.commands;

import io.github.profjb58.pumapbot.PUMapBot;
import io.github.profjb58.pumapbot.maps.MapManager;
import io.github.profjb58.pumapbot.maps.PUMap;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.io.File;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class MapsCommand extends Command {

    private final int MAP_DOWNLOAD_TIMEOUT_SECS = 5;
    private final MapManager mapManager;

    public MapsCommand(MapManager mapManager) {
        this.mapManager = mapManager;
        this.isEphemeral = false;
    }

    @Override
    protected void onCommand(@NotNull SlashCommandInteractionEvent event) {
        String subCommandName = event.getSubcommandName();

        if(subCommandName != null && event.getGuild() != null) {
            switch(subCommandName) {
                case "search" -> onSearchMaps(event);
                case "add" -> onAddMap(event);
                case "remove" -> onRequestRemoveMap(event);
            }
        }
    }

    private void onRequestRemoveMap(@NotNull SlashCommandInteractionEvent event) {
        var name = event.getOption("name", OptionMapping::getAsString);
        if(name != null) {
            try {
                var mapIds = mapManager.search(name, null, null);
                if(!mapIds.isEmpty()) {
                    mapManager.scheduleDeleteMaps(mapIds, event.getUser().getIdLong());
                    event.getHook().setEphemeral(true).sendMessage(">>> This action will permanently remove " + mapIds.size() + " map(s). \n" +
                            "Press **Confirm** only if you are sure you want complete this action")
                            .addActionRow(Button.success("confirm_delete_map", "Confirm")).queue();
                }
            } catch (SQLException e) {
                event.getHook().setEphemeral(true).sendMessage("⚠️ A database error occurred. Failed to search for map " + name).queue();
                e.printStackTrace();
            }
        }
    }

    private void onSearchMaps(@NotNull SlashCommandInteractionEvent event) {
        String name = event.getOption("name", OptionMapping::getAsString);
        User author = event.getOption("author", OptionMapping::getAsUser);
        String tags = event.getOption("tags", OptionMapping::getAsString);

        // Format tags
        List<String> tagsList = new ArrayList<>();
        if(tags != null) {
            tags = tags.replaceAll("\\s",""); // Remove whitespace characters
            tagsList = List.of(tags.split(",")); // Seperate tags out
        }

        try {
            var mapMsgIds = mapManager.search(name, author, tagsList);
            if(!mapMsgIds.isEmpty())
                mapManager.listMaps(event.getHook(), new HashSet<>(mapMsgIds));
            else
                event.getHook().sendMessage("No results found...").setEphemeral(true).queue();
        } catch (SQLException e) {
            PUMapBot.LOGGER.error("There was an issue searching for a valid PU map" , e);
            event.getHook().sendMessage("Failed to search for a map by this criteria...").setEphemeral(true).queue();
        }
    }

    private void onAddMap(@NotNull SlashCommandInteractionEvent event) {
        // Grab command information
        String name = event.getOption("name", OptionMapping::getAsString);
        String description = event.getOption("description", OptionMapping::getAsString);
        User author = event.getOption("author", event.getUser(), OptionMapping::getAsUser);
        String tags = event.getOption("tags", "N/A", OptionMapping::getAsString);
        double version = event.getOption("version", 1.0, OptionMapping::getAsDouble);
        Message.Attachment puAttachment = event.getOption("pu_file", OptionMapping::getAsAttachment);
        Message.Attachment pumapAttachment = event.getOption("pumap_file", OptionMapping::getAsAttachment);

        // Check the map at least has a name description and playable map attachment
        if(name != null && description != null && puAttachment != null) {

            // Exit early if the map file extensions don't match
            if(hasIncorrectExtension(puAttachment, "pu", event.getHook())) return;
            if(pumapAttachment != null)
                if(hasIncorrectExtension(pumapAttachment, "pumap", event.getHook())) return;

            try {
                // Download attached files
                File puFile = puAttachment.getProxy().downloadToFile(mapManager.getMapFilePath(name, version, author, MapManager.MapType.PLAYABLE))
                        .get(MAP_DOWNLOAD_TIMEOUT_SECS, TimeUnit.SECONDS);
                File pumapFile = null;
                if(pumapAttachment != null)
                    pumapFile = pumapAttachment.getProxy().downloadToFile(mapManager.getMapFilePath(name, version, author, MapManager.MapType.EDITABLE))
                            .get(MAP_DOWNLOAD_TIMEOUT_SECS, TimeUnit.SECONDS);

                // Store the map in the dedicated map storage channels
                var mapStorageChannelIds = mapManager.getMapStorageChannelIds();
                for(var channelId : mapStorageChannelIds) {
                    var mapStorageChannel = event.getJDA().getChannelById(TextChannel.class, channelId);

                    if(mapStorageChannel != null) {
                        var authorId = author.getId();
                        var mapMessage = String.format("""
                        **Name**: %s
                        **Version**: %.2f
                        **Author ID**: %s
                        **Description**: %s
                        **Tags**: %s 
                        """, name, version, authorId, description, tags);

                        // Add file attachments
                        var messageAction = mapStorageChannel.sendMessage(mapMessage).addFile(puFile);
                        if(pumapFile != null)
                            messageAction = messageAction.addFile(pumapFile);

                        // Submit response
                        final File finalPumapFile = pumapFile;
                        messageAction.submit().whenComplete((message, error) -> {
                            if(error != null) {
                                event.getHook().sendMessage("⚠️ Failed to add map. Message could not be sent to the map storage channel").setEphemeral(true).queue();
                                error.printStackTrace();
                            } else {
                                try {
                                    mapManager.addMap(new PUMap(message.getIdLong(), name, version, description, author, message.getTimeCreated(),
                                            tags, 0, puFile, finalPumapFile));
                                    event.getHook().sendMessage("📁 Successfully added map: **" + name + "**!").setEphemeral(true).queue();
                                } catch (SQLException e) {
                                    event.getHook().setEphemeral(true).sendMessage("⚠️ Failed to add map. Error storing map into the servers database").queue();
                                    e.printStackTrace();
                                }
                            }
                        });
                    } else {
                        event.getHook().setEphemeral(true).sendMessage("⚠️ Failed to add map. A channel is not assigned to store maps").queue();
                    }
                }
            } catch (Exception e) {
                PUMapBot.LOGGER.warn("Failed to download and store map files for map: " + name, e);
                event.getHook().setEphemeral(true).sendMessage("⚠️ Failed to add map. Map files could not be downloaded and stored").queue();
            }
        } else {
            event.getHook().setEphemeral(true).sendMessage("Failed to add map. Please include a name, description and file").queue();
        }
    }

    private boolean hasIncorrectExtension(@Nonnull Message.Attachment attachment, String targetExtension, InteractionHook hook) {
        if(attachment.getFileExtension() != null && attachment.getFileExtension().equals(targetExtension))
            return false;
        else {
            hook.setEphemeral(true).sendMessage("⚠️ Failed to add map. Attached file did not have the correct extension: `." + targetExtension +
                    "` and is therefore not a valid Project University map").queue();
            return true;
        }
    }

    @Override
    public CommandData getCommandData() {
        return Commands.slash("maps", "Project University (sub)maps")
                .addSubcommands(
                        new SubcommandData("search", "Search for a specific PU (sub)map")
                                .addOption(OptionType.STRING, "name", "Search by name")
                                .addOption(OptionType.USER, "author", "Search by author")
                                .addOption(OptionType.STRING, "tags", "Search by tags. Separate tags using commas"),
                        new SubcommandData("add", "Adds a new PU (sub)map")
                                .addOption(OptionType.STRING, "name", "Map name")
                                .addOption(OptionType.STRING, "description", "maps description")
                                .addOption(OptionType.USER, "author", "Maps original author")
                                .addOption(OptionType.NUMBER, "version", "Maps current version")
                                .addOption(OptionType.STRING, "tags", "Tags to help find the map. Separate tags using a comma")
                                .addOption(OptionType.ATTACHMENT, "pu_file", "Playable PU map file")
                                .addOption(OptionType.ATTACHMENT, "pumap_file", "Editable PU map file"),
                        new SubcommandData("remove", "Remove a PU (sub)map")
                                .addOption(OptionType.STRING, "name", "Existing maps name"),
                        new SubcommandData("update", "Updates an existing PU (sub)map")
                                .addOption(OptionType.STRING, "name", "Existing maps name")
                                .addOption(OptionType.NUMBER, "version", "New version number")
                );
    }
}
