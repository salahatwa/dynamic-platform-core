package com.platform.service;

import com.platform.entity.Invitation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {
    
    private final JavaMailSender mailSender;
    
    @Value("${app.frontend.url:http://localhost:4200}")
    private String frontendUrl;
    
    @Value("${app.email.from:noreply@platform.com}")
    private String fromEmail;
    
    @Async
    public void sendInvitationEmail(Invitation invitation) {
        try {
            log.info("Attempting to send invitation email to: {}", invitation.getEmail());
            
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(invitation.getEmail());
            helper.setSubject("Invitation to join " + invitation.getCorporate().getName());
            
            String invitationUrl = frontendUrl + "/accept-invitation/" + invitation.getToken();
            
            String htmlContent = buildInvitationEmailHtml(
                invitation.getCorporate().getName(),
                invitation.getInvitedBy().getFirstName() + " " + 
                    invitation.getInvitedBy().getLastName(),
                invitationUrl,
                invitation.getExpiresAt().format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
            );
            
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
            
            log.info("✅ Invitation email sent successfully to: {}", invitation.getEmail());
            
        } catch (Exception e) {
            // Log the error but don't throw - email is optional
            log.warn("⚠️ Failed to send invitation email to: {} - Invitation created successfully but email not sent. Error: {}", 
                    invitation.getEmail(), e.getMessage());
            log.debug("Email error details:", e);
            // Don't throw exception - invitation should still be created
        }
    }
    
    private String buildInvitationEmailHtml(String corporateName, String inviterName, 
                                           String invitationUrl, String expiresAt) {
        return "<!DOCTYPE html>" +
            "<html>" +
            "<head>" +
            "<meta charset='UTF-8'>" +
            "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
            "</head>" +
            "<body style='margin: 0; padding: 0; font-family: Arial, sans-serif; background-color: #f4f4f4;'>" +
            "<table width='100%' cellpadding='0' cellspacing='0' style='background-color: #f4f4f4; padding: 20px;'>" +
            "<tr>" +
            "<td align='center'>" +
            "<table width='600' cellpadding='0' cellspacing='0' style='background-color: #ffffff; border-radius: 10px; box-shadow: 0 2px 4px rgba(0,0,0,0.1);'>" +
            "<tr>" +
            "<td style='padding: 40px 30px;'>" +
            "<h1 style='color: #333333; margin: 0 0 20px 0; font-size: 28px;'>You're Invited!</h1>" +
            "<p style='font-size: 16px; color: #555555; line-height: 1.6; margin: 0 0 20px 0;'>" +
            "<strong>" + inviterName + "</strong> has invited you to join <strong>" + corporateName + "</strong>." +
            "</p>" +
            "<p style='font-size: 14px; color: #777777; line-height: 1.6; margin: 0 0 30px 0;'>" +
            "Click the button below to accept the invitation and get started:" +
            "</p>" +
            "<table width='100%' cellpadding='0' cellspacing='0'>" +
            "<tr>" +
            "<td align='center'>" +
            "<a href='" + invitationUrl + "' style='display: inline-block; padding: 14px 40px; background-color: #6366f1; color: #ffffff; text-decoration: none; border-radius: 6px; font-weight: bold; font-size: 16px;'>Accept Invitation</a>" +
            "</td>" +
            "</tr>" +
            "</table>" +
            "<p style='font-size: 12px; color: #999999; line-height: 1.6; margin: 30px 0 10px 0;'>" +
            "This invitation expires on <strong>" + expiresAt + "</strong>." +
            "</p>" +
            "<p style='font-size: 12px; color: #999999; line-height: 1.6; margin: 0;'>" +
            "If you didn't expect this invitation, you can safely ignore this email." +
            "</p>" +
            "<hr style='border: none; border-top: 1px solid #eeeeee; margin: 30px 0;'>" +
            "<p style='font-size: 11px; color: #aaaaaa; line-height: 1.6; margin: 0;'>" +
            "If the button doesn't work, copy and paste this link into your browser:<br>" +
            "<a href='" + invitationUrl + "' style='color: #6366f1; word-break: break-all;'>" + invitationUrl + "</a>" +
            "</p>" +
            "</td>" +
            "</tr>" +
            "</table>" +
            "</td>" +
            "</tr>" +
            "</table>" +
            "</body>" +
            "</html>";
    }
}
