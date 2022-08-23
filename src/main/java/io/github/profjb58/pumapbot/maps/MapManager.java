package io.github.profjb58.pumapbot.maps;

import io.github.cdimascio.dotenv.Dotenv;
import io.github.profjb58.pumapbot.PUMapBot;
import io.github.profjb58.pumapbot.db.DatabaseManager;
import io.github.profjb58.pumapbot.db.Statements;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.interactions.InteractionHook;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.*;

public class MapManager {

    private static final File MAPS_DIRECTORY = new File(System.getProperty("user.dir") + "/resources/data/maps");
    public static final Emoji DOWNLOADS_EMOJI = Emoji.fromUnicode("U+1F4BE");

    private static final Map<Long, MapPaginator> userPaginators;
    private static final WeakHashMap<Long, List<Long>> mapDeleteCache;

    private final DatabaseManager dbManager;
    private final Dotenv envConfig;
    private Set<Long> mapStorageChannelIds;

    public enum MapType {
        EDITABLE,
        PLAYABLE
    }

    public MapManager(PUMapBot instance) {
        this.dbManager = instance.getDBManager();
        this.envConfig = instance.getEnvConfig();
        this.mapStorageChannelIds = new HashSet<>();

        try {
            mapStorageChannelIds = instance.getDBManager().retrieveMapChannelIds();
        } catch (SQLException e) {
            throw new RuntimeException("Could not create the Map Manager. Failed to retrieve map channel ids for all available servers", e);
        }

        if(!MAPS_DIRECTORY.exists()) {
            boolean hasMadeDirectories = MAPS_DIRECTORY.mkdirs();
            if(!hasMadeDirectories)
                PUMapBot.LOGGER.error("Failed to a create directory for storing contest map data at: " + MAPS_DIRECTORY.getAbsolutePath());
        }
    }

    public void addMap(@Nonnull PUMap puMap) throws SQLException {
        dbManager.insertPUMap(puMap);
    }

    public void scheduleDeleteMaps(@Nonnull List<Long> storageMessageIds, Long userId) {
        mapDeleteCache.put(userId, storageMessageIds);
    }

    public void deleteMaps(List<Long> storageMessageIds, InteractionHook hook) throws SQLException {
        for(var messageId : storageMessageIds)
            dbManager.deleteMapRow(messageId);

        for(var channelId : mapStorageChannelIds) {
            TextChannel mapStorageChannel = hook.getJDA().getTextChannelById(channelId);

            if(mapStorageChannel != null) {
                var messageHistory = MessageHistory.getHistoryFromBeginning(mapStorageChannel);
                var messageHistoryFuture = messageHistory.submit();
                messageHistoryFuture.whenComplete((history, error) -> {
                    if (error != null) error.printStackTrace();

                    List<Message> messages = history.getRetrievedHistory();
                    int numDeletedMessages = 0;
                    for (var message : messages) {
                        if (storageMessageIds.contains(message.getIdLong())) {
                            message.delete().queue();
                            numDeletedMessages++;
                        }
                    }
                    if(numDeletedMessages == storageMessageIds.size()) // Number of deleted maps = expected number
                        hook.sendMessage("**Successfully removed " + storageMessageIds.size() + " map(s)!**").setEphemeral(true).queue();
                    else
                        hook.sendMessage("⚠️ Failed to remove all map(s) from the map storage channel.\n" +
                                "Please contact a moderator to manually remove these map(s)").setEphemeral(true).queue();
                });
            } else {
                hook.sendMessage("**⚠️ A database error occurred... Server has " + ).setEphemeral(true).queue();
            }
        }
    }

    public List<Long> search(@Nullable String name, @Nullable User author, @Nullable List<String> tags) throws SQLException {
        Set<Long> mapMsgIds = new HashSet<>(); // Hashset to ensure unique values
        if(name != null)
            mapMsgIds.addAll(dbManager.searchMatchingMapIDs(name, Statements.MapSearchQuery.NAME));
        if(author != null)
            mapMsgIds.addAll(dbManager.searchMatchingMapIDs(author.getIdLong(), Statements.MapSearchQuery.AUTHOR));
        if(tags != null && !tags.isEmpty()) {
            StringBuilder tagRegex = new StringBuilder();
            tagRegex.append(tags.get(0));
            for (int i = 1; i < tags.size(); i++)
                tagRegex.append("|").append(tags.get(i)); // Append an OR operator to the end
            mapMsgIds.addAll(dbManager.searchMatchingMapIDs(tagRegex.toString(), Statements.MapSearchQuery.TAGS));
        }
        return mapMsgIds.stream().toList();
    }

    public void listMaps(InteractionHook hook, Set<Long> mapMsgIds) {
        TextChannel mapStorageChannel = hook.getJDA().getTextChannelById(MAP_STORAGE_CHANNEL);
        if (mapStorageChannel != null && !mapMsgIds.isEmpty()) {
            List<PUMap> puMaps = new ArrayList<>();
            var messageHistory = MessageHistory.getHistoryFromBeginning(mapStorageChannel);
            var messageHistoryFuture = messageHistory.submit();

            messageHistoryFuture.whenComplete((history, error) -> {
                if(error != null) error.printStackTrace();

                List<Message> messages = history.getRetrievedHistory();
                for(var message : messages) {
                    if(mapMsgIds.contains(message.getIdLong())) {
                        // Get stripped message content and replace whitespace characters
                        String msgContent = message.getContentDisplay()
                                .replace(" ", "")
                                .replace("**", "");
                        String[] msgContentSplit = msgContent.lines().toArray(String[]::new);

                        // Map content
                        String name = null, description = null, tags = "N/A";
                        User author = hook.getJDA().getUserById(envConfig.get("BOT_ID"));
                        int downloads = 0;
                        double version = 1.0;
                        OffsetDateTime dateUpdated = message.getTimeEdited();

                        // Get download count
                        var reaction = message.getReaction(DOWNLOADS_EMOJI);
                        if (reaction != null && reaction.hasCount())
                            downloads = reaction.getCount(); // Get number of downloads from number of reactions of a specific emoji

                        // Cycle through each row of additional map content not stored in the database
                        for (var msgRow : msgContentSplit) {
                            String[] msgRowContent = msgRow.split(":"); // Split into key-value pairs
                            String messageKey = msgRowContent[0];
                            String messageValue = msgRowContent[1];

                            switch (messageKey) {
                                case "Description" -> description = messageValue;
                                case "Version" -> version = Double.parseDouble(messageValue);
                                case "Tags" -> tags = messageValue;
                                case "Name" -> name = messageValue;
                                case "AuthorID" -> {
                                    var nullableAuthor = hook.getJDA().getUserById(messageValue);
                                    if(nullableAuthor != null)
                                        author = nullableAuthor;
                                }
                            }
                        }

                        // Get attachments
                        if(name != null && author != null) {
                            var puFile = getMapFilePath(name, version, author, MapType.PLAYABLE);
                            var pumapFile = getMapFilePath(name, version, author, MapType.EDITABLE);

                            if(description != null && puFile.exists()) // Create the final PU map object
                                puMaps.add(new PUMap(message.getIdLong(), name, version, description, author, dateUpdated, tags, downloads, puFile, pumapFile));
                        }
                    }
                }
                if(!puMaps.isEmpty())
                    // Assign a paginator for the given user
                    userPaginators.put(hook.getInteraction().getUser().getIdLong(), new MapPaginator(puMaps, hook));
            });
        }
    }

    public File getMapFilePath(String name, double version, User author, MapType mapType) {
        String extension = (mapType == MapType.PLAYABLE) ? "pu" : "pumap";
        return new File(MAPS_DIRECTORY + "/" + name + "_v" + version + "." + extension);
    }

    @Nullable
    public MapPaginator getPaginator(@Nonnull User user) {
        return userPaginators.getOrDefault(user.getIdLong(), null);
    }

    @Nullable
    public List<Long> getMapDeleteCache(@Nonnull User user) { return mapDeleteCache.getOrDefault(user.getIdLong(), null);}

    public void clearMapDeleteCache(@Nonnull User user) { mapDeleteCache.remove(user.getIdLong()); }

    public void addMapStorageChannel(@Nonnull TextChannel channel) { mapStorageChannelIds.add(channel.getIdLong()); }

    public HashSet<Long> getMapStorageChannelIds() { return mapStorageChannelIds; }

    static {
        userPaginators = new WeakHashMap<>();
        mapDeleteCache = new WeakHashMap<>();
        mapStorageChannelIds = new HashSet<>();
    }
}
