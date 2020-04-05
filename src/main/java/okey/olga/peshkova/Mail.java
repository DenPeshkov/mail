package okey.olga.peshkova;

import javax.mail.MessagingException;
import java.util.Arrays;

public class Mail {
    public static void main(String[] args) throws MessagingException {
        MailParser.Message[] parse = MailParser.parse("imap.mail.ru", "denis_maksim@mail.ru", "samsungg910");
        System.out.println(Arrays.toString(parse));
    }
}