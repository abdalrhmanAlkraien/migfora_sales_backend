package com.migfora.sales.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 02/06/2026
 * @Time: 4:26 AM
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {


    private final SesClient sesClient;
    private final TemplateEngine templateEngine;

    @Value("${aws.ses.from-email}")
    private String fromEmail;

    public void sendHtml(String toEmail, String subject, String htmlBody) {
        try {
            SendEmailRequest request = SendEmailRequest.builder()
                    .source(fromEmail)
                    .destination(Destination.builder()
                            .toAddresses(toEmail)
                            .build())
                    .message(Message.builder()
                            .subject(Content.builder()
                                    .data(subject)
                                    .charset("UTF-8")
                                    .build())
                            .body(Body.builder()
                                    .html(Content.builder()
                                            .data(htmlBody)
                                            .charset("UTF-8")
                                            .build())
                                    .build())
                            .build())
                    .build();

            SendEmailResponse response = sesClient.sendEmail(request);
            log.info("Email sent | to={} subject={} messageId={}",
                    toEmail, subject, response.messageId());

        } catch (Exception ex) {
            log.error("Failed to send email | to={} error={}", toEmail, ex.getMessage());
        }
    }

    public String renderTemplate(String templateName, Context context) {
        return templateEngine.process(templateName, context);
    }

}
