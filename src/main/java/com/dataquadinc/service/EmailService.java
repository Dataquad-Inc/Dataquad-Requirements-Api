package com.dataquadinc.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender javaMailSender;


    public void sendEmail(String to, String subject, String text)  {

        MimeMessage mimeMessage=javaMailSender.createMimeMessage();
        try{
            MimeMessageHelper helper=new MimeMessageHelper(mimeMessage,true,"UTF-8");

            helper.setTo(to); // recipient's email
            helper.setSubject(subject); // email subject
            helper.setText(text,true); // email body
            helper.setFrom("notifications@adroitinnovative.com"); // 👈 force sender

            javaMailSender.send(mimeMessage);
        }
         catch (MessagingException e){
             e.printStackTrace();

             throw new RuntimeException("Failed to send email ",e);
         }
    }
}
