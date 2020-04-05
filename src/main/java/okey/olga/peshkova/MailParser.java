package okey.olga.peshkova;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.search.FlagTerm;
import java.util.*;

public final class MailParser {

    private MailParser() {

    }

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

    @FunctionalInterface
    private interface ParseFunction {
        Message[] apply(Store store) throws MessagingException;
    }

    static public Message[] parse(String host, String user, String password) throws MessagingException {
        return parse(host, user, password, "INBOX");
    }

    static public Message[] parse(String host, String user, String password, String mbox) throws MessagingException {
        return parse(host, user, password, mbox, "imap");
    }

    static public Message[] parse(String host, String user, String password, String mbox, String protocol) throws MessagingException {

        return parse(host, user, password, mbox, protocol, store -> {
            Message[] messages;
            try (Folder folder = Optional.ofNullable(store.getFolder(mbox)).filter(MailParser::folderExists).orElseThrow(() -> new NoSuchElementException("Folder is empty"))) {
                folder.open(Folder.READ_ONLY);
                MimeMessage[] messages1 = (MimeMessage[]) folder.search(new FlagTerm(new Flags(Flags.Flag.SEEN), false));
                messages = Arrays.stream(messages1).map(MailParser::mapMessage).filter(Objects::nonNull).toArray(Message[]::new);
            }
            return messages;
        });
    }

    static private Message[] parse(String host, String user, String password, String mbox, String protocol, ParseFunction function) throws MessagingException {
        Properties props = System.getProperties();
        props.setProperty("mail." + protocol + ".ssl.enable", "true");

        Session session = Session.getInstance(props, null);

        Store store = session.getStore(protocol);

        store.connect(host, user, password);

        try (store) {
            return function.apply(store);
        }
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