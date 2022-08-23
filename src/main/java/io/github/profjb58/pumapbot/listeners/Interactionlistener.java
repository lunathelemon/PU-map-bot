package io.github.profjb58.pumapbot.listeners;

import io.github.profjb58.pumapbot.PUMapBot;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.io.File;
import java.sql.SQLException;

public class Interactionlistener extends ListenerAdapter {

    private final PUMapBot instance;

    public Interactionlistener(PUMapBot instance) {
        this.instance = instance;
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        String buttonId = event.getButton().getId();
        var mapPaginator = instance.getMapManager().getPaginator(event.getUser());

        if(buttonId != null) {
            // Map paginator action buttons
            if(mapPaginator != null) {
                switch (buttonId) {
                    case "nav_forwards" -> mapPaginator.switchPage(event, event.getMessageIdLong(), true);
                    case "nav_backwards" -> mapPaginator.switchPage(event, event.getMessageIdLong(), false);
                    case "download_playable_map" -> sendMapFileDownload(event, mapPaginator.getPlayableMapFile());
                    case "download_editable_map" -> sendMapFileDownload(event, mapPaginator.getEditableMapFile());
                }
            }
            // Map delete confirmation button
            if(buttonId.equals("confirm_delete_map")) {
                var mapManager = instance.getMapManager();
                var mapsToDelete = mapManager.getMapDeleteCache(event.getUser());

                if(mapsToDelete != null && !mapsToDelete.isEmpty()) {
                    try {
                        event.deferReply(true).queue();
                        mapManager.deleteMaps(mapsToDelete, event.getHook());
                    } catch (SQLException e) {
                        event.getHook().sendMessage("⚠️ Failed to remove all map(s) from the database. \n" +
                                "Please contact a moderator to manually remove these map(s)").setEphemeral(true).queue();
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private void sendMapFileDownload(@NotNull ButtonInteractionEvent event, @Nullable File mapFile) {
        if(mapFile != null) {
            event.deferReply(true).queue();
            event.getHook().sendFile(mapFile).queue();
        }
    }
}
