package mail.module.database.access;

import mail.module.mail.request.MailParser;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Properties;

public class Database {
  private final String connectionUrl;

  public Database(String connectionUrl) {
    this.connectionUrl = connectionUrl;
  }

  public void insertMails(MailParser.MailMessage[] messages) {
    String insertString =
        "insert into user_mail_inbox (subject,comments,sender) values ('?','?','?')";
    try (Connection connection = DriverManager.getConnection(connectionUrl);
        PreparedStatement statement = connection.prepareStatement(insertString)) {
      for (MailParser.MailMessage message : messages) {
        statement.setString(1, message.getSubject());
        statement.setString(2, message.getText());
        statement.setString(3, message.getFrom()[0]);
        statement.executeUpdate();
      }
    } catch (SQLException e) {
      printSqlException(e);
      System.exit(1);
    }
  }

  private static void printSqlException(SQLException exception) {
    exception.printStackTrace(System.err);
    System.err.println("Error code: " + exception.getErrorCode());
    System.err.println("SQL state: " + exception.getSQLState());
    System.err.println("Message: " + exception.getMessage());
  }
}
