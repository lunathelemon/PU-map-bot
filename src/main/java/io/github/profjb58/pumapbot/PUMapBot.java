package io.github.profjb58.pumapbot;

import io.github.cdimascio.dotenv.Dotenv;
import io.github.profjb58.pumapbot.commands.CommandManager;
import io.github.profjb58.pumapbot.config.ConfigHandler;
import io.github.profjb58.pumapbot.db.DatabaseManager;
import io.github.profjb58.pumapbot.listeners.GuildListener;
import io.github.profjb58.pumapbot.listeners.Interactionlistener;
import io.github.profjb58.pumapbot.maps.MapManager;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.security.auth.login.LoginException;

public class PUMapBot {

    public static final Logger LOGGER = LogManager.getLogger(PUMapBot.class);

    private final CommandManager commandManager;
    private final Dotenv envConfig;
    private final ConfigHandler configHandler;
    private final DatabaseManager dbManager;
    private final MapManager mapManager;

    public PUMapBot() throws LoginException {
        envConfig = Dotenv.configure().load();
        configHandler = new ConfigHandler();
        dbManager = new DatabaseManager(envConfig);

        JDA jda = JDABuilder.createDefault(envConfig.get("BOT_TOKEN"))
                .setStatus(OnlineStatus.ONLINE)
                .setMemberCachePolicy(MemberCachePolicy.ALL) // Save online members into the cache
                .enableIntents(GatewayIntent.GUILD_PRESENCES, GatewayIntent.GUILD_MEMBERS, GatewayIntent.DIRECT_MESSAGES)
                .setChunkingFilter(ChunkingFilter.ALL)
                .enableCache(CacheFlag.ONLINE_STATUS, CacheFlag.ROLE_TAGS, CacheFlag.GUILD_SCHEDULED_EVENTS) // Cache whenever the online status changes
                .build();

        commandManager = new CommandManager(this);
        mapManager = new MapManager(this);

        // Register listeners
        jda.addEventListener(new Interactionlistener(this));
        jda.addEventListener(new GuildListener(this));
        jda.addEventListener(commandManager);
    }

    public Dotenv getEnvConfig() {
        return envConfig;
    }

    public ConfigHandler getConfigHandler() { return configHandler; }

    public DatabaseManager getDBManager() { return dbManager; }

    public CommandManager getCommandManager() { return commandManager; }

    public MapManager getMapManager() { return mapManager; }

    public static void main(String[] args) {
        try {
            new PUMapBot();
        } catch (LoginException le) {
            LOGGER.error("Failed to login. Bot Token failed...");
        }
    }
}
