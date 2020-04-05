package okey.olga.peshkova;

import javax.mail.MessagingException;
import java.io.IOException;
import java.util.Arrays;

public class Mail {
    public static void main(String[] args) throws IOException, MessagingException {
        MailParser mailParser = new MailParser();
        MailParser.Message[] parse = mailParser.parse("imap.mail.ru", "denis_maksim@mail.ru", "samsungg910");
        System.out.println(Arrays.toString(parse));
    }
}