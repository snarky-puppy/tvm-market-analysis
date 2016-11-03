package com.tvm.crunch;


import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;


/**
 * Created by horse on 18/09/15.
 */
public class Gmailer {

    private static final Logger logger = LogManager.getLogger(Gmailer.class);

    private static Session getSession() {
        final String username = "mwlazlo@gmail.com";
        final String password = readPassword();

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        Session session = Session.getInstance(props,
                new Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(username, password);
                    }
                });
        return session;
    }

    private static String readPassword() {
        try {
            List<String> lines = Files.readAllLines(Paths.get(System.getProperty("user.home"), ".gmail_password"));
            if(lines.size() > 0) {
                String s = lines.get(0);
                if(s != null && s.length() > 0) {
                    logger.error("pwd:"+s);
                    return s;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        logger.error("Failed to read gmail password.");
        return null;
    }

    public static void main(String[] args) throws IOException, MessagingException {



        // Build a new authorized API client service.
        Session session = getSession();

        StringBuilder builder = new StringBuilder();
        builder.append("DailyTriggerReport possible error condition " + Integer.toString(DateUtil.today()));

        String fileName = "/Users/horse/mbox";

        sendMessage(session, "me",
                createEmailWithAttachment(session,
                        builder.toString(),
                        builder.toString(),
                        "/"+ FilenameUtils.getPath(fileName),
                        FilenameUtils.getName(fileName)));



    }

    /**
     * Create a MimeMessage using the parameters provided.
     *
     *
     * @param session
     * @param from Email address of the sender, the mailbox account.
     * @param subject Subject of the email.
     * @param bodyText Body text of the email.
     * @return MimeMessage to be used to send email.
     * @throws MessagingException
     */
    private static MimeMessage createEmail(Session session, String from, String subject,
                                           String bodyText) throws MessagingException {

        MimeMessage email = new MimeMessage(session);

        email.setFrom(new InternetAddress(from));
        email.addRecipient(Message.RecipientType.TO,
                new InternetAddress("vinniekeoghs@gmail.com"));
        email.addRecipient(Message.RecipientType.TO,
                new InternetAddress("chamberlaintimothy61@gmail.com"));
        email.addRecipient(Message.RecipientType.TO,
                new InternetAddress("mwlazlo@gmail.com"));

        email.setSubject(subject);
        email.setText(bodyText);
        return email;
    }


    /**
     * Create a Message from an email
     *
     * @param email Email to be set to raw of message
     * @return Message containing base64url encoded email.
     * @throws IOException
     * @throws MessagingException
     */
    /*
    private static Message createMessageWithEmail(MimeMessage email)
            throws IOException, javax.mail.MessagingException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        email.writeTo(bytes);
        String encodedEmail = Base64.encodeBase64URLSafeString(bytes.toByteArray());
        Message message = new Message();
        message.setRaw(encodedEmail);
        return message;
    }*/

    /**
     * Send an email from the user's mailbox to its recipient.
     *
     * @param service Authorized Gmail API instance.
     * @param userId User's email address. The special value "me"
     * can be used to indicate the authenticated user.
     * @param email Email to be sent.
     * @throws MessagingException
     * @throws IOException
     */
    private static void sendMessage(Session service, String userId, MimeMessage email)
            throws IOException, MessagingException {
        //Message message = createMessageWithEmail(email);

        Transport.send(email);
        System.out.println(email.toString());
    }

    /**
     * Create a MimeMessage using the parameters provided.
     *
     * @param subject Subject of the email.
     * @param bodyText Body text of the email.
     * @param fileDir Path to the directory containing attachment.
     * @param filename Name of file to be attached.
     * @return MimeMessage to be used to send email.
     * @throws MessagingException
     */
    private static MimeMessage createEmailWithAttachment(Session session, String subject, String bodyText, String fileDir, String filename) throws MessagingException, IOException {
        String from = "mwlazlo@gmail.com";

        MimeMessage email = new MimeMessage(session);

        InternetAddress fAddress = new InternetAddress(from);

        email.setFrom(fAddress);
        email.addRecipient(Message.RecipientType.TO,
                new InternetAddress("vinniekeoghs@gmail.com"));
        email.addRecipient(Message.RecipientType.TO,
                new InternetAddress("chamberlaintimothy61@gmail.com"));

        email.addRecipient(Message.RecipientType.TO,
                new InternetAddress("mwlazlo@gmail.com"));
        email.setSubject(subject);

        MimeBodyPart mimeBodyPart = new MimeBodyPart();
        mimeBodyPart.setContent(bodyText, "text/plain");
        mimeBodyPart.setHeader("Content-Type", "text/plain; charset=\"UTF-8\"");

        Multipart multipart = new MimeMultipart();
        multipart.addBodyPart(mimeBodyPart);

        mimeBodyPart = new MimeBodyPart();
        DataSource source = new FileDataSource(fileDir + filename);

        mimeBodyPart.setDataHandler(new DataHandler(source));
        mimeBodyPart.setFileName(filename);
        String contentType = Files.probeContentType(FileSystems.getDefault()
                .getPath(fileDir, filename));
        mimeBodyPart.setHeader("Content-Type", contentType + "; name=\"" + filename + "\"");
        mimeBodyPart.setHeader("Content-Transfer-Encoding", "base64");

        multipart.addBodyPart(mimeBodyPart);

        email.setContent(multipart);

        return email;
    }

    public static void sendNotification(int size, String fileDir, String filename) throws MessagingException, IOException {
        // Build a new authorized API client service.
        Session session = getSession();

        StringBuilder builder = new StringBuilder();
        builder.append("Report for "+Integer.toString(DateUtil.today()));
        builder.append(": " + Integer.toString(size) + " triggers detected today.\n");

        if(size == 0) {
            sendMessage(session, "me",
                    createEmail(session,
                            "mwlazlo@gmail.com",
                            "TVM Trigger Report ["+Integer.toString(size)+"]",
                            builder.toString()));
        } else {
            sendMessage(session, "me",
                    createEmailWithAttachment(session,
                            "TVM Trigger Report ["+Integer.toString(size)+"]",
                            builder.toString(),
                            fileDir,
                            filename));
        }
    }
}
