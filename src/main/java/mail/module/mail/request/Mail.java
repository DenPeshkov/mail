package mail.module.mail.request;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import javax.mail.MessagingException;
import java.util.Arrays;
import java.util.Properties;

public class Mail {
  public static void main(String[] args) {
    Properties properties = System.getProperties();

    for (int i = 0; i < args.length; i++) {
      if ("--help".equals(args[i])) {
        System.err.println(
            "Usage: mail [-p port] [-T protocol] [-f mbox] [--delete] -H host -U user -P password");
        System.exit(0);
      } else if ("-H".equals(args[i])) properties.setProperty("host", args[++i]);
      else if ("-U".equals(args[i])) properties.setProperty("user", args[++i]);
      else if ("-P".equals(args[i])) properties.setProperty("password", args[++i]);
      else if ("-p".equals(args[i]))
        properties.setProperty("port", String.valueOf(Integer.parseInt(args[++i])));
      else if ("-T".equals(args[i])) properties.setProperty("protocol", args[++i]);
      else if ("-f".equals(args[i])) properties.setProperty("mbox", args[++i]);
      else if ("--delete".equals(args[i])) properties.setProperty("delete", "true");
    }

    if (!properties.containsKey("host")
        || !properties.containsKey("user")
        || !properties.containsKey("password")) {
      System.err.println(
          "You have to specify host user and password:\n"
              + "Usage: mail [-p port] [-T protocol] [-f mbox] [--delete] -H host -U user -P password");
      System.exit(0);
    }

    MailParser.MailMessage[] parse = new MailParser.MailMessage[0];
    try {
      parse = MailParser.parse(properties);
    } catch (MessagingException e) {
      e.printStackTrace(System.err);
    }

    System.out.println(Arrays.toString(parse));
  }
}
