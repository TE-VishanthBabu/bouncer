package com.nteligen.hq.dhs.siaft.dal;

import com.nteligen.hq.dhs.siaft.dao.FileAttribute;
import com.nteligen.hq.dhs.siaft.exceptions.DatabaseConnectionException;
import com.nteligen.hq.dhs.siaft.exceptions.PersistenceException;
import org.hsqldb.cmdline.SqlFile;
import org.hsqldb.cmdline.SqlToolError;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class SanitizeDALTest
{
  public static final String INSERT_SANITIZE_ENGINE_SQL = "INSERT INTO SanitizeEngines"
          + "(PK_SanitizeEngine_ID, EngineName, Date_Created) VALUES(1000, 'MetaDefender'"
          + ", CURRENT_TIMESTAMP);";

  /**
   * initializes the database.
   * @throws SQLException if there was a sql exception
   * @throws IOException if there was an IOexception
   * @throws SqlToolError if there was a SQLToolException
   */
  @BeforeClass
  public static void init() throws SQLException, ClassNotFoundException, IOException, SqlToolError
  {
    Class.forName("org.hsqldb.jdbcDriver");
    // initialize database
    initDatabase();
  }

  /**
   * Destroys the in memeory database.
   * @throws SQLException if there was a sql exception
   * @throws IOException if there was an IOexception
   * @throws SqlToolError if there was a SQLToolException
   */
  @AfterClass
  public static void destroy() throws SQLException, IOException, SqlToolError
  {
    try (Connection connection = getConnection())
    {
      try (InputStream inputStream = SanitizeDALTest.class.getClassLoader()
              .getResourceAsStream("prov/delete_tables.sql"))
      {
        SqlFile sqlFile = new SqlFile(new InputStreamReader(inputStream), "init", System.out,
                "UTF-8", false, new File("."));
        sqlFile.setConnection(connection);
        sqlFile.execute();
      }
      connection.commit();
    }
  }

  /**
   * Database initialization for testing i.e.
   * <ul>
   * <li>Creating Table</li>
   * <li>Inserting record</li>
   * </ul>
   *
   * @throws SQLException if there was a sql exception
   */
  private static void initDatabase() throws SQLException, IOException, SqlToolError
  {
    try (Connection connection = getConnection();
         Statement statement = connection.createStatement();)
    {
      try (InputStream inputStream = SanitizeDALTest.class.getClassLoader()
              .getResourceAsStream("prov/create_tables.sql"))
      {
        SqlFile sqlFile = new SqlFile(new InputStreamReader(inputStream), "init", System.out,
                "UTF-8", false, new File("."));
        sqlFile.setConnection(connection);
        sqlFile.execute();
      }
      connection.commit();
      statement.executeUpdate(INSERT_SANITIZE_ENGINE_SQL);
      connection.commit();
    }
  }

  /**
   * Create a connection.
   *
   * @return connection object
   * @throws SQLException if there was a sql exception
   */
  private static Connection getConnection() throws SQLException
  {
    return DriverManager.getConnection("jdbc:hsqldb:mem:siaftunittest",
            "sa",
            "");
  }

  @Test
  public void testSanitizeDAL() throws DatabaseConnectionException, PersistenceException
  {
    FileAttributeDAL attributeDal = new FileAttributeDAL();
    FileAttribute fileAttribute = attributeDal.createNewFileAttribute("foo.pdf", 0,
        "pdf", "d3b07384d113edec49eaa6238ad5ff00", "someSHA256",
            "fdf997ee-1771-4ded-9672-4abdbb818433");

    long fileAttributeId = fileAttribute.getFileAttributeId();

    SanitizeDAL sanitizeDal = new SanitizeDAL();
    sanitizeDal.createNewSanitize(fileAttributeId, 1000, "pass",
                                  "d3b07384d113edec49eaa6238ad5ff00", "pdf");
  }
}

