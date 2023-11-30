package com.zorsecyber.bouncer.webapp;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.jdbc.core.JdbcTemplate;

@Component
public class SchemaInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    public SchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        createSessions();
    }

    /**
     * Spring session management
     * To maintain session during application reload,without user getting logout.
     *
     */
    private void createSessions() {
        String createSessionSql = "CREATE TABLE IF NOT EXISTS SPRING_SESSION (" +
                "PRIMARY_ID CHAR(36) NOT NULL," +
                "SESSION_ID CHAR(36) NOT NULL," +
                "CREATION_TIME BIGINT NOT NULL," +
                "LAST_ACCESS_TIME BIGINT NOT NULL," +
                "MAX_INACTIVE_INTERVAL INT NOT NULL," +
                "EXPIRY_TIME BIGINT NOT NULL," +
                "PRINCIPAL_NAME VARCHAR(1000)," +
                "CONSTRAINT SPRING_SESSION_PK PRIMARY KEY (PRIMARY_ID))";

        jdbcTemplate.execute(createSessionSql);
        dropAndCreateIndex();
        String createSessionAttrSql = "CREATE TABLE IF NOT EXISTS SPRING_SESSION_ATTRIBUTES ( " +
                "SESSION_PRIMARY_ID CHAR(36) NOT NULL, " +
                "ATTRIBUTE_NAME VARCHAR(200) NOT NULL, " +
                "ATTRIBUTE_BYTES BLOB NOT NULL, " +
                "CONSTRAINT SPRING_SESSION_ATTRIBUTES_PK PRIMARY KEY " +
                "(SESSION_PRIMARY_ID, ATTRIBUTE_NAME), " +
                "CONSTRAINT SPRING_SESSION_ATTRIBUTES_FK FOREIGN KEY " +
                "(SESSION_PRIMARY_ID) REFERENCES SPRING_SESSION(PRIMARY_ID) ON DELETE " +
                "CASCADE)";
        jdbcTemplate.execute(createSessionAttrSql);
    }

    private void dropAndCreateIndex() {
        String checkIndexSql = "SELECT COUNT(*) FROM information_schema.statistics " +
                "WHERE table_schema = 'hauberk_detection' " +
                "AND table_name = 'SPRING_SESSION' " +
                "AND index_name = 'SPRING_SESSION_IX1'";

        int indexCount = jdbcTemplate.queryForObject(checkIndexSql, Integer.class);

        if (indexCount == 1) {
            String dropIndexSql = "ALTER TABLE SPRING_SESSION DROP INDEX SPRING_SESSION_IX1";
            jdbcTemplate.execute(dropIndexSql);
        }

//        MySql DB query for creating Index
//        String createIndexSql = "CREATE INDEX SPRING_SESSION_IX1 ON SPRING_SESSION (SESSION_ID)";

//        Maria DB query for creating Index
        String createIndexSql = "CREATE INDEX IF NOT EXISTS SPRING_SESSION_IX1 ON SPRING_SESSION (SESSION_ID)";
        jdbcTemplate.execute(createIndexSql);

        String checkIndexSql2 = "SELECT COUNT(*) FROM information_schema.statistics " +
                "WHERE table_schema = 'hauberk_detection' " +
                "AND table_name = 'SPRING_SESSION' " +
                "AND index_name = 'SPRING_SESSION_IX2'";

        int indexCount2 = jdbcTemplate.queryForObject(checkIndexSql2, Integer.class);

        if (indexCount2 == 1) {
            String dropIndexSql2 = "ALTER TABLE SPRING_SESSION DROP INDEX SPRING_SESSION_IX2";
            jdbcTemplate.execute(dropIndexSql2);
        }

//        MySql DB query for creating Index
//        String createIndexSql2 = "CREATE INDEX SPRING_SESSION_IX2 ON SPRING_SESSION (EXPIRY_TIME)";

//        Maria DB query for creating Index
        String createIndexSql2 = "CREATE INDEX IF NOT EXISTS SPRING_SESSION_IX2 ON SPRING_SESSION (EXPIRY_TIME)";
        jdbcTemplate.execute(createIndexSql2);
    }
}


