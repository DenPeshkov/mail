package mail.module.database.access;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

public class Database {
  public static void executeProcedure(
      String serverUrl, int port, String databaseName, String user, String password) {
    Properties properties = new Properties();
    properties.setProperty("mail/module/database/database", databaseName);
    properties.setProperty("user", user);
    properties.setProperty("password", password);
    properties.setProperty("encrypt", "true");
    properties.setProperty("trustServerCertificate", "false");
    properties.setProperty("loginTimeout", "30");

    try (Connection connection =
            getConnection("jdbc:sqlserver://" + serverUrl + ":" + port, properties);
        Statement statement = connection.createStatement()) {

    } catch (SQLException e) {
      printSqlException(e);
      System.exit(1);
    }
  }

  private static Connection getConnection(String url, Properties properties) {
    Connection connection = null;
    try {
      connection = DriverManager.getConnection(url, properties);
    } catch (SQLException e) {
      printSqlException(e);
      System.exit(1);
    }
    return connection;
  }

  private static void printSqlException(SQLException exception) {
    exception.printStackTrace(System.err);
    System.err.println("Error code: " + exception.getErrorCode());
    System.err.println("SQL state: " + exception.getSQLState());
    System.err.println("Message: " + exception.getMessage());
  }
}
