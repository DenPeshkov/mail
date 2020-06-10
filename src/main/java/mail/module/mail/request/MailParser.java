package mail.module.mail.request;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.search.FlagTerm;
import java.util.*;

public final class MailParser {

  private MailParser() {}

  public static class Message {
    private String[] from;
    private String subject;

    public Message() {}

    public Message(javax.mail.Message message) throws MessagingException {
      setFrom((InternetAddress[]) message.getFrom());
      setSubject(message.getSubject());
    }

    public String[] getFrom() {
      return from;
    }

    public void setFrom(InternetAddress[] from) {
      this.from = Arrays.stream(from).map(InternetAddress::getAddress).toArray(String[]::new);
    }

    public String getSubject() {
      return subject;
    }

    public void setSubject(String subject) {
      this.subject = subject;
    }

    @Override
    public String toString() {
      return "Message{" + "from=" + Arrays.toString(from) + ", subject='" + subject + '\'' + '}';
    }
  }

  @FunctionalInterface
  private interface ParseFunction {
    Message[] apply(Store store) throws MessagingException;
  }

  public static Message[] parse(Properties properties) throws MessagingException {

    return parse(
        properties,
        store -> {
          Message[] messages;
          try (Folder folder =
              Optional.ofNullable(store.getFolder(properties.getProperty("mbox", "INBOX")))
                  .filter(MailParser::folderExists)
                  .orElseThrow(() -> new NoSuchElementException("Folder is empty"))) {
            folder.open(Folder.READ_ONLY);
            MimeMessage[] messages1 =
                (MimeMessage[]) folder.search(new FlagTerm(new Flags(Flags.Flag.SEEN), false));

            FetchProfile fp = new FetchProfile();
            fp.add(FetchProfile.Item.ENVELOPE);

            folder.fetch(messages1, fp);

            messages =
                Arrays.stream(messages1)
                    .map(MailParser::mapMessage)
                    .limit(10)
                    .filter(Objects::nonNull)
                    .toArray(Message[]::new);
          }
          return messages;
        });
  }

  private static Message[] parse(Properties properties, ParseFunction function)
      throws MessagingException {
    Session session = Session.getInstance(properties, null);

    Store store = session.getStore(properties.getProperty("protocol", "imap"));

    store.connect(
        properties.getProperty("host"),
        Integer.parseInt(properties.getProperty("port", "143")),
        properties.getProperty("user"),
        properties.getProperty("password"));

    try (store) {
      return function.apply(store);
    }
  }

  private static Message mapMessage(MimeMessage message) {
    try {
      return new Message(message);
    } catch (MessagingException e) {
      e.printStackTrace(System.err);
    }
    return null;
  }

  private static boolean folderExists(Folder s) {
    try {
      return s.exists();
    } catch (MessagingException e) {
      e.printStackTrace(System.err);
    }
    return false;
  }
}
