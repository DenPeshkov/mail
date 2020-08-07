package mail.module.mail.request;

import mail.module.database.access.Database;

import javax.mail.MessagingException;
import java.util.Properties;

/** Основной класс для запуска приложения */
public class Mail {

  /**
   * Метод для запуска приложения
   *
   * @param args параметры командной строки для настройки приложения
   */
  public static void main(String[] args) {
    // получаем параметры из системы, например переменных окружения
    Properties properties = System.getProperties();

    // считываем параметры из командной строки и заполняем ими пропертис
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

    // если нет необходимых параметров выдаем ошибку и завершаем работу приложения
    if (!properties.containsKey("host")
        || !properties.containsKey("user")
        || !properties.containsKey("password")
        || !properties.containsKey("databaseURL")) {
      System.err.println(
          "You have to specify host, user and password and databaseURL:\n"
              + "Usage: mail [-p port] [-T protocol] [-f mbox] [--delete] -H host -U user -P password --database databaseURL");
      System.exit(0);
    }

    // массив писем
    MailParser.MailMessage[] messages = new MailParser.MailMessage[0];
    try {
      // заполняем массив
      messages = MailParser.parse(properties);
    } catch (MessagingException e) { // если ошибка выходим
      e.printStackTrace(System.err);
      System.exit(1);
    }

    // подключаемся к базе данных
    Database database = new Database(properties.getProperty("databaseURL"));

    // записываем в базу данных письма
    database.insertMails(messages);
  }
}
