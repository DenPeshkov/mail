package mail.request;

import javax.mail.MessagingException;
import java.util.Arrays;

public class Mail {
  public static void main(String[] args) throws MessagingException {
    MailParser.Message[] parse = null;

    switch (args.length) {
      case 3:
        {
          parse = MailParser.parse(args[0], args[1], args[2]);
          break;
        }
      case 4:
        {
          parse = MailParser.parse(args[0], args[1], args[2], args[3]);
          break;
        }
      case 5:
        {
          parse = MailParser.parse(args[0], args[1], args[2], args[3], args[4]);
          break;
        }
      default:
        {
          System.err.println("parameters: host, user, password, [mbox], [protocol]");
          System.exit(1);
        }
    }

    System.out.println(Arrays.toString(parse));
  }
}
