package vttp2022.project.addressprocessor.services;

import java.net.MalformedURLException;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.UrlResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

//contains all the email business logics
@Service
public class EmailService {
    
    @Autowired private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String from;
    
    public void sendEmailWithAttachment(String toEmail, String fileName) throws MessagingException{
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper messageHelper = new MimeMessageHelper(mimeMessage, true);
        messageHelper.setSubject("OMAC Query Results " + LocalDate.now() + " " + LocalTime.now());
        messageHelper.setTo(toEmail);
        messageHelper.setText("Thank you for using OMAC, here is your file attached!", true);
        messageHelper.setFrom(from);

        try {
            messageHelper.addAttachment(
                fileName +".csv", 
                new UrlResource("https://bigcontainer.sgp1.digitaloceanspaces.com/OMAC/csv/" + fileName + ".csv")
            );
        } catch (MalformedURLException murle) {
            murle.printStackTrace();
        }
        
        mailSender.send(mimeMessage);
    }
}
