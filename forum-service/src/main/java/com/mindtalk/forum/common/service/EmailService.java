package com.mindtalk.forum.common.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EmailService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.from:noreply@mindtalk.example.com}")
    private String from;

    @Async
    public void sendSimpleEmail(String to, String subject, String body) {
        if (mailSender == null) {
            log.debug("[邮件] 邮件服务未配置，跳过发送 to={}", to);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("[邮件] 发送成功 to={} subject={}", to, subject);
        } catch (Exception e) {
            log.error("[邮件] 发送失败 to={} subject={} error={}", to, subject, e.getMessage());
        }
    }
}
