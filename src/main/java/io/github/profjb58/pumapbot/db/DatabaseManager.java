package io.github.profjb58.pumapbot.db;

import io.github.cdimascio.dotenv.Dotenv;
import io.github.profjb58.pumapbot.maps.PUMap;
import net.dv8tion.jda.api.entities.Guild;

import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DatabaseManager {

    private final Dotenv envConfig;

    public DatabaseManager(Dotenv envConfig) {
        this.envConfig = envConfig;
        createTables();
    }

    private void createTables() {
        try(var connection = getConnection()) {
            var mapsTableStatement = connection.prepareStatement(Statements.MAPS_TABLE);
            var serversTableStatement = connection.prepareStatement(Statements.SERVERS_TABLE);
            mapsTableStatement.execute();
            serversTableStatement.execute();
        } catch(SQLException e) {
            throw new RuntimeException("Failed to create all the initial tables for the SQL database", e);
        }
    }

    public void insertServerDetails(Guild guild, long mapsChannelId) throws SQLException {
        try(var connection = getConnection();
            var preparedStatement = connection.prepareStatement(Statements.INSERT_INTO_SERVERS_TABLE)) {

            preparedStatement.setLong(1, guild.getIdLong());
            preparedStatement.setLong(2, mapsChannelId);
            preparedStatement.execute();
        }
    }

    public Set<Long> retrieveMapChannelIds() throws SQLException {
        try(var connection = getConnection();
            var preparedStatement = connection.prepareStatement(Statements.DELETE_SERVERS_ROW);
            var resultsSet = preparedStatement.executeQuery()) {

            Set<Long> mapChannelIds = new HashSet<>();
            while(resultsSet.next())
                mapChannelIds.add(resultsSet.getLong(2));
            return mapChannelIds;
        }
    }

    public void deleteServerDetails(Guild guild) throws SQLException {
        try(var connection = getConnection();
            var preparedStatement = connection.prepareStatement(Statements.DELETE_SERVERS_ROW))
        {
            preparedStatement.setLong(1, guild.getIdLong());
            preparedStatement.execute();
        }
    }

    public void insertPUMap(PUMap puMap) throws SQLException {
        try(var connection = getConnection();
            var preparedStatement = connection.prepareStatement(Statements.INSERT_INTO_MAPS_TABLE)) {

            preparedStatement.setLong(1, puMap.discordMessageId());
            preparedStatement.setString(2, puMap.name());
            preparedStatement.setString(3, puMap.tags());
            preparedStatement.setLong(4, puMap.author().getIdLong());
            preparedStatement.setDate(5, Date.valueOf(puMap.dateUpdated().toLocalDate()));
            preparedStatement.execute();
        }
    }

    public List<Long> searchMatchingMapIDs(Object value, Statements.MapSearchQuery searchType) throws SQLException {
        try(var connection = getConnection();
            var preparedStatement = connection.prepareStatement(searchType.getStatement())) {
            preparedStatement.setObject(1, value);

            try (var resultsSet = preparedStatement.executeQuery()) {
                List<Long> matchingMapIds = new ArrayList<>();
                while (resultsSet.next())
                    matchingMapIds.add(resultsSet.getLong(1));
                return matchingMapIds;
            }
        }
    }

    public void deleteMapRow(Long mapMessageId) throws SQLException {
        try(var connection = getConnection();
            var preparedStatement = connection.prepareStatement(Statements.DELETE_MAP_ROW))
        {
            preparedStatement.setLong(1, mapMessageId);
            preparedStatement.execute();
        }
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:mysql://" + envConfig.get("SQL_HOST") + "/" + envConfig.get("DB_LOCATION"),
                envConfig.get("SQL_USER"), envConfig.get("SQL_PASSWORD"));
    }
}
