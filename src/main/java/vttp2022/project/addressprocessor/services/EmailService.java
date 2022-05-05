package vttp2022.project.addressprocessor.services;

import java.net.MalformedURLException;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.UrlResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    
    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String from;
    
    public void sendEmailWithAttachment() throws MessagingException{

        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper messageHelper = new MimeMessageHelper(mimeMessage, true);
        messageHelper.setSubject("test OMAC test");
        messageHelper.setTo("omacapp@outlook.com");
        messageHelper.setText("here is your attachment", true);
        messageHelper.setFrom(from);

        try {
            messageHelper.addAttachment(
                "yourfilename.csv", 
                new UrlResource("https://bigcontainer.sgp1.digitaloceanspaces.com/OMAC/csv/0c93ea0d.csv"));
        } catch (MalformedURLException murle) {
            murle.printStackTrace();
        } 

        // messageHelper.addAttachment(
        //     "testfilename.csv", 
        //     new ClassPathResource("static\\test2 (large).csv"));

        try {
            mailSender.send(mimeMessage);
        } catch (MailException me) {
            me.printStackTrace();
        }
    }
}
