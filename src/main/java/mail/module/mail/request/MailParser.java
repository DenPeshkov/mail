package mail.module.mail.request;

import org.jsoup.Jsoup;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.search.FlagTerm;
import javax.mail.search.SubjectTerm;
import java.io.IOException;
import java.util.*;

/** Класс для чтения писем из почты */
public final class MailParser {

  private MailParser() {}

  /** Класс для представления одного письма */
  public static class MailMessage {
    private String[] from; // отправители
    private String subject; // тема
    private String text; // текст письма

    public MailMessage() {}

    /**
     * Заполняем поля письма
     *
     * @param message письмо с данными
     * @throws MessagingException
     * @throws IOException
     */
    public MailMessage(javax.mail.Message message) throws MessagingException, IOException {
      setFrom((InternetAddress[]) message.getFrom());
      setSubject(message.getSubject());
      text = Jsoup.parse(getText(message)).body().text();
    }

    /**
     * Получаем текст письма
     *
     * @param p письмо
     * @return
     * @throws MessagingException
     * @throws IOException
     */
    private String getText(Part p) throws MessagingException, IOException {
      if (p.isMimeType("text/*")) {
        return (String) p.getContent();
      }

      if (p.isMimeType("multipart/alternative")) {
        // prefer html text over plain text
        Multipart mp = (Multipart) p.getContent();
        String text = null;
        for (int i = 0; i < mp.getCount(); i++) {
          Part bp = mp.getBodyPart(i);
          if (bp.isMimeType("text/plain")) {
            if (text == null) text = getText(bp);
          } else if (bp.isMimeType("text/html")) {
            String s = getText(bp);
            if (s != null) return s;
          } else {
            return getText(bp);
          }
        }
        return text;
      } else if (p.isMimeType("multipart/*")) {
        Multipart mp = (Multipart) p.getContent();
        for (int i = 0; i < mp.getCount(); i++) {
          String s = getText(mp.getBodyPart(i));
          if (s != null) return s;
        }
      }

      return "";
    }

    /**
     * Получаем отправителей
     *
     * @return массив отправителей
     */
    public String[] getFrom() {
      return from;
    }

    /**
     * Устанавливаем отправителей
     *
     * @param from массив email адресов отправителей
     */
    public void setFrom(InternetAddress[] from) {
      this.from = Arrays.stream(from).map(InternetAddress::getAddress).toArray(String[]::new);
    }

    /**
     * Получаем тему письма
     *
     * @return тема письма
     */
    public String getSubject() {
      return subject;
    }

    /**
     * Устанавливаем тему письма
     *
     * @param subject тема письма
     */
    public void setSubject(String subject) {
      this.subject = subject;
    }

    /**
     * Получаем текст письма
     *
     * @return текст письма
     */
    public String getText() {
      return text;
    }

    /**
     * Устанавливаем текст письма
     *
     * @param text текст письма
     */
    public void setText(String text) {
      this.text = text;
    }

    /**
     * Возвращает содержимое письма в удобочитаемом формате (отправительи, тема, текст)
     *
     * @return Строку с содержимым письма
     */
    @Override
    public String toString() {
      return "Message{"
          + "from="
          + Arrays.toString(from)
          + ", subject=\""
          + subject
          + "\", text=\""
          + text
          + '\"'
          + '}';
    }
  }

  /** Интерфейс для лямбда выражения (функция как класс) */
  @FunctionalInterface
  private interface ParseFunction {
    /**
     * Функция для работы с почтовым ящиком (чтение писем)
     *
     * @param store почтовый ящик
     * @return массив писем
     * @throws MessagingException
     */
    MailMessage[] apply(Store store) throws MessagingException;
  }

  /**
   * Функция для чтения соощений из почты
   *
   * @param properties настройки для подключения к почте и работе с ней
   * @return массив писем
   * @throws MessagingException
   */
  public static MailMessage[] parse(Properties properties) throws MessagingException {

    // вызов фукции для чтения писем с передачей лямбда выражения
    return parse(
        properties, // параметры
        store -> { // лямбда выражение
          MailMessage[] mailMessages;
          // Подключаемся к папке (входящие) для чтения писем
          try (Folder folder =
              Optional.ofNullable(store.getFolder(properties.getProperty("mbox", "INBOX")))
                  .filter(MailParser::folderExists)
                  .orElseThrow(() -> new NoSuchElementException("Folder is empty"))) {
            // открываем папку на чтения и запись
            folder.open(Folder.READ_WRITE);

            // ищем письма, которые еще не прочитаны (не работает при использовании POP3)
            Message[] messages = folder.search(new FlagTerm(new Flags(Flags.Flag.SEEN), false));

            // ищем письма, содержащие "ЗАЯВКА №" в теме письма
            messages = folder.search(new SubjectTerm("ЗАЯВКА №"), messages);

            // елси стоит флаг --delete удаляем прочитанные письма
            if (properties.getProperty("delete", "false").equals("true"))
              folder.setFlags(messages, new Flags(Flags.Flag.DELETED), true);

            FetchProfile fp = new FetchProfile();
            fp.add(FetchProfile.Item.ENVELOPE);

            // читаем письма bulk operation
            folder.fetch(messages, fp);

            // записываем письма в массив
            mailMessages =
                Arrays.stream(messages)
                    .map(MailParser::mapMessage)
                    .filter(Objects::nonNull)
                    .toArray(MailMessage[]::new);
          }
          return mailMessages;
        });
  }

  /**
   * Функция для чтения писем из почты
   *
   * @param properties настройки
   * @param function функция для чтения писем
   * @return массив писем
   * @throws MessagingException
   */
  private static MailMessage[] parse(Properties properties, ParseFunction function)
      throws MessagingException {
    // получаем сессию
    Session session = Session.getInstance(properties, null);

    // получаем почтовый ящик
    // используем протокол из командной строки, или imap если протокол не был задан
    Store store = session.getStore(properties.getProperty("protocol", "imap"));

    // подключаемся к почтовому ящику
    store.connect(
        properties.getProperty("host"), // email адрес
        Integer.parseInt(properties.getProperty("port", "143")), // порт, 143 если не задан
        properties.getProperty("user"), // имя пользователя
        properties.getProperty("password")); // пароль

    try (store) {
      return function.apply(store); // вызываем функцию для работы с письмами
    }
  }

  /**
   * Функция для преобразоваия писем
   *
   * @param message письмо
   * @return письмо в преобразованном формате
   */
  private static MailMessage mapMessage(Message message) {
    try {
      return new MailMessage(message);
    } catch (MessagingException | IOException e) {
      e.printStackTrace(System.err);
    }
    return null;
  }

  /**
   * Функция для проверки ссуществования папки (входяшие)
   *
   * @param s папка
   * @return существует ли папка
   */
  private static boolean folderExists(Folder s) {
    try {
      return s.exists();
    } catch (MessagingException e) {
      e.printStackTrace(System.err);
    }
    return false;
  }
}
