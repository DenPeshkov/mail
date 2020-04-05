package okey.olga.peshkova;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.search.FlagTerm;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class MailParser {

    private Store store;

    public static class Message {
        private String[] from;
        private String subject;

        public Message() {
        }

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
            return "Message{" +
                    "from=" + Arrays.toString(from) +
                    ", subject='" + subject + '\'' +
                    '}';
        }
    }

    public Message parse(Path filePath) throws IOException, MessagingException {
        Message message1 = null;

        try (InputStream inputStream = Files.newInputStream(filePath)) {
            MimeMessage message = new MimeMessage(Session.getDefaultInstance(new Properties()), inputStream);
            message1 = new Message();
            message1.setSubject(message.getSubject());
            message1.setFrom((InternetAddress[]) message.getFrom());
        }
        return message1;
    }

    public Message[] parse(String host, String user, String password) throws MessagingException {
        return parse(host, user, password, "INBOX");
    }

    public Message[] parse(String host, String user, String password, String mbox) throws MessagingException {
        return parse(host, user, password, mbox, "imap");
    }

    public Message[] parse(String host, String user, String password, String mbox, String protocol) throws MessagingException {
        Message[] messages;
        try (Folder folder = getFolder(host, user, password, mbox, protocol).orElseThrow(() -> new NoSuchElementException("Folder is empty"))) {
            folder.open(Folder.READ_ONLY);
            MimeMessage[] messages1 = (MimeMessage[]) folder.search(new FlagTerm(new Flags(Flags.Flag.SEEN), false));
            messages = Arrays.stream(messages1).map(MailParser::mapMessage).filter(Objects::nonNull).limit(100).toArray(Message[]::new);
        }
        return messages;
    }

    private Optional<Folder> getFolder(String host, String user, String password, String mbox, String protocol) throws MessagingException {
        Properties props = System.getProperties();
        props.setProperty("mail." + protocol + ".ssl.enable", "true");

        Session session = Session.getInstance(props, null);

        Store store = session.getStore(protocol);

        store.connect(host, user, password);

        return Optional.ofNullable(store.getFolder(mbox)).filter(MailParser::folderExists);
    }

    private static Message mapMessage(MimeMessage message) {
        try {
            return new Message(message);
        } catch (MessagingException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static boolean folderExists(Folder s) {
        try {
            return s.exists();
        } catch (MessagingException e) {
            e.printStackTrace();
        }
        return false;
    }
}