package io.github.profjb58.pumapbot.db;

public class Statements {

    public static final String MAPS_TABLE_ID = "pu_maps";
    public static final String SERVERS_TABLE_ID = "pu_servers";

    // Tables
    static final String SERVERS_TABLE = String.format("""
            CREATE TABLE IF NOT EXISTS %s (
              `server_id` BIGINT(20) UNSIGNED NOT NULL,
              `map_channel_id` BIGINT(20) UNSIGNED NOT NULL,
              PRIMARY KEY (`server_id`)
            ) ENGINE=InnoDB;
            """, SERVERS_TABLE_ID);

    static final String MAPS_TABLE = String.format("""
            CREATE TABLE IF NOT EXISTS %s (
            	`discord_message_id` bigint unsigned NOT NULL,
            	`name` varchar(100) NOT NULL,
                `tags` varchar(500),
                `author` bigint unsigned,
                `date_updated` date,
                `downloads` int unsigned,
                PRIMARY KEY (`discord_message_id`)
            ) ENGINE=InnoDB;
            """, MAPS_TABLE_ID);

    static final String GET_ALL_SERVERS = String.format("""
            SELECT * FROM %s
            """, SERVERS_TABLE_ID);

    static final String INSERT_INTO_SERVERS_TABLE = String.format("""
            INSERT INTO %s VALUES (?, ?)
            """, SERVERS_TABLE_ID);

    static final String INSERT_INTO_MAPS_TABLE = String.format("""
            INSERT INTO %s VALUES (?, ?, ?, ?, ?)
            """, MAPS_TABLE_ID);

    static final String DELETE_SERVERS_ROW = String.format("""
            DELETE FROM %s WHERE server_id = ?
            """, SERVERS_TABLE_ID);

    static final String DELETE_MAP_ROW = String.format("""
            DELETE FROM %s WHERE discord_message_id = ?
            """, MAPS_TABLE_ID);

    public enum MapSearchQuery {
        AUTHOR(String.format("SELECT * FROM %s WHERE author = ?", MAPS_TABLE_ID)), // Matches a specific value
        NAME("SELECT * FROM " + MAPS_TABLE_ID + " WHERE name LIKE CONCAT('%',?,'%')"), // Matches any value
        TAGS(String.format("SELECT * FROM %s WHERE tags REGEXP ?", MAPS_TABLE_ID)); // Matches a regex expression

        private final String statement;

        MapSearchQuery(String statement) {
            this.statement = statement;
        }

        public String getStatement() {
            return statement;
        }
    }
}
