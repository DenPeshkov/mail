package mail.module.mail.request;

import org.jsoup.Jsoup;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.search.FlagTerm;
import java.io.IOException;
import java.util.*;

public final class MailParser {

  private MailParser() {}

  public static class MailMessage {
    private String[] from;
    private String subject;
    private String text;

    public MailMessage() {}

    public MailMessage(javax.mail.Message message) throws MessagingException, IOException {
      setFrom((InternetAddress[]) message.getFrom());
      setSubject(message.getSubject());
      text = Jsoup.parse(getText(message)).body().text();
    }

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

  @FunctionalInterface
  private interface ParseFunction {
    MailMessage[] apply(Store store) throws MessagingException;
  }

  public static MailMessage[] parse(Properties properties) throws MessagingException {

    return parse(
        properties,
        store -> {
          MailMessage[] mailMessages;
          try (Folder folder =
              Optional.ofNullable(store.getFolder(properties.getProperty("mbox", "INBOX")))
                  .filter(MailParser::folderExists)
                  .orElseThrow(() -> new NoSuchElementException("Folder is empty"))) {
            folder.open(Folder.READ_WRITE);
            Message[] messages = folder.search(new FlagTerm(new Flags(Flags.Flag.SEEN), false));
            if (properties.getProperty("delete", "false").equals("true"))
              folder.setFlags(messages, new Flags(Flags.Flag.DELETED), true);

            FetchProfile fp = new FetchProfile();
            fp.add(FetchProfile.Item.ENVELOPE);

            folder.fetch(messages, fp);

            mailMessages =
                Arrays.stream(messages)
                    .map(MailParser::mapMessage)
                    .filter(Objects::nonNull)
                    .filter(message -> message.subject.toUpperCase().startsWith("ЗАЯВКА №"))
                    .toArray(MailMessage[]::new);
          }
          return mailMessages;
        });
  }

  private static MailMessage[] parse(Properties properties, ParseFunction function)
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

  private static MailMessage mapMessage(Message message) {
    try {
      return new MailMessage(message);
    } catch (MessagingException | IOException e) {
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
