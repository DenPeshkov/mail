package mail.module.mail.request;

import mail.module.database.access.Database;

import javax.mail.MessagingException;
import java.util.Properties;

public class Mail {
  public static void main(String[] args) {
    Properties properties = System.getProperties();

    for (int i = 0; i < args.length; i++) {
      if ("--help".equals(args[i])) {
        System.err.println(
            "Usage: mail [-p port] [-T protocol] [-f mbox] [--delete] -H host -U user -P password --database databaseURL");
        System.exit(0);
      } else if ("-H".equals(args[i])) properties.setProperty("host", args[++i]);
      else if ("-U".equals(args[i])) properties.setProperty("user", args[++i]);
      else if ("-P".equals(args[i])) properties.setProperty("password", args[++i]);
      else if ("-p".equals(args[i]))
        properties.setProperty("port", String.valueOf(Integer.parseInt(args[++i])));
      else if ("-T".equals(args[i])) properties.setProperty("protocol", args[++i]);
      else if ("-f".equals(args[i])) properties.setProperty("mbox", args[++i]);
      else if ("--delete".equals(args[i])) properties.setProperty("delete", "true");
      else if ("--database".equals(args[i])) properties.setProperty("databaseURL", args[++i]);
    }

    if (!properties.containsKey("host")
        || !properties.containsKey("user")
        || !properties.containsKey("password")
        || !properties.containsKey("databaseURL")) {
      System.err.println(
          "You have to specify host, user and password and databaseURL:\n"
              + "Usage: mail [-p port] [-T protocol] [-f mbox] [--delete] -H host -U user -P password --database databaseURL");
      System.exit(0);
    }

    MailParser.MailMessage[] messages = new MailParser.MailMessage[0];
    try {
      messages = MailParser.parse(properties);
    } catch (MessagingException e) {
      e.printStackTrace(System.err);
      System.exit(1);
    }

    Database database = new Database(properties.getProperty("databaseURL"));
    database.insertMails(messages);
  }
}
