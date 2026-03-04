package com.example.lms.service;

import com.example.lms.model.LeaveRequest;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailNotificationService {

    private final JavaMailSender mailSender;

    public EmailNotificationService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendLeaveApproved(LeaveRequest leave) {
        String employeeName = leave.getEmployee().getFirstName();
        String employeeEmail = leave.getEmployee().getEmailAddress();
        String managerName = leave.getReviewedBy().getFirstName()
                + " " + leave.getReviewedBy().getSurname();

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(employeeEmail);
        message.setSubject("LMS - Your Leave Request Has Been Approved");
        message.setText(
            "Hi " + employeeName + ",\n\n" +
            "Great news! Your leave request has been approved.\n\n" +
            "━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
            "Leave Details:\n" +
            "━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
            "Type       : " + leave.getLeaveType() + "\n" +
            "From       : " + leave.getStartDate() + "\n" +
            "To         : " + leave.getEndDate() + "\n" +
            "Duration   : " + leave.getDuration() + " business day(s)\n" +
            "Status     : APPROVED\n" +
            "Reviewed by: " + managerName + "\n" +
            (leave.getManagerComments() != null && !leave.getManagerComments().isBlank()
                ? "Comments   : " + leave.getManagerComments() + "\n" : "") +
            "━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n" +
            "Enjoy your time off!\n\n" +
            "LMS Team"
        );
        trySend(message, "approval");
    }

    public void sendLeaveRejected(LeaveRequest leave) {
        String employeeName = leave.getEmployee().getFirstName();
        String employeeEmail = leave.getEmployee().getEmailAddress();
        String managerName = leave.getReviewedBy().getFirstName()
                + " " + leave.getReviewedBy().getSurname();

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(employeeEmail);
        message.setSubject("LMS - Your Leave Request Has Been Rejected");
        message.setText(
            "Hi " + employeeName + ",\n\n" +
            "Unfortunately, your leave request has been rejected.\n\n" +
            "━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
            "Leave Details:\n" +
            "━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
            "Type       : " + leave.getLeaveType() + "\n" +
            "From       : " + leave.getStartDate() + "\n" +
            "To         : " + leave.getEndDate() + "\n" +
            "Duration   : " + leave.getDuration() + " business day(s)\n" +
            "Status     : REJECTED\n" +
            "Reviewed by: " + managerName + "\n" +
            (leave.getManagerComments() != null && !leave.getManagerComments().isBlank()
                ? "Reason     : " + leave.getManagerComments() + "\n" : "") +
            "━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n" +
            "If you have questions, please contact your manager.\n\n" +
            "LMS Team"
        );
        trySend(message, "rejection");
    }

    public void sendLeaveSubmitted(LeaveRequest leave) {
        if (leave.getEmployee().getManager() == null) return;

        String managerName  = leave.getEmployee().getManager().getFirstName();
        String managerEmail = leave.getEmployee().getManager().getEmailAddress();
        String employeeName = leave.getEmployee().getFirstName()
                + " " + leave.getEmployee().getSurname();

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(managerEmail);
        message.setSubject("LMS - New Leave Request from " + employeeName);
        message.setText(
            "Hi " + managerName + ",\n\n" +
            employeeName + " has submitted a new leave request that requires your review.\n\n" +
            "━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
            "Leave Details:\n" +
            "━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
            "Employee  : " + employeeName + "\n" +
            "Type      : " + leave.getLeaveType() + "\n" +
            "From      : " + leave.getStartDate() + "\n" +
            "To        : " + leave.getEndDate() + "\n" +
            "Duration  : " + leave.getDuration() + " business day(s)\n" +
            "Reason    : " + leave.getReason() + "\n" +
            "━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n" +
            "Please log in to the LMS to review this request.\n\n" +
            "LMS Team"
        );
        trySend(message, "submission");
    }

    // ─── NEW: Welcome email sent when account is created ────────
    public void sendWelcomeEmail(String toEmail, String firstName,
                                  String role, String tempPassword) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("LMS - Welcome! Your Account Has Been Created");
        message.setText(
            "Hi " + firstName + ",\n\n" +
            "Welcome to the Leave Management System!\n\n" +
            "Your account has been created. Here are your login details:\n\n" +
            "━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
            "Account Details:\n" +
            "━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
            "Email         : " + toEmail + "\n" +
            "Temporary Pass: " + tempPassword + "\n" +
            "Role          : " + role + "\n" +
            "━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n" +
            "IMPORTANT: Please log in and reset your password immediately.\n\n" +
            "To reset your password:\n" +
            "1. Go to the LMS login page\n" +
            "2. Click 'Forgot Password'\n" +
            "3. Enter your email address\n" +
            "4. Follow the link sent to your email\n\n" +
            "LMS Team"
        );
        trySend(message, "welcome");
    }

    // ─── NEW: Email sent when user receives a notification ──────
    public void sendNotificationEmail(String toEmail, String firstName,
                                       String title, String messageBody) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("LMS - " + title);
        message.setText(
            "Hi " + firstName + ",\n\n" +
            "You have a new notification:\n\n" +
            "━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
            title + "\n" +
            "━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
            messageBody + "\n" +
            "━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n" +
            "Log in to the LMS to view more details.\n\n" +
            "LMS Team"
        );
        trySend(message, "notification");
    }

    // ─── Helper ──────────────────────────────────────────────────
    private void trySend(SimpleMailMessage message, String type) {
        try {
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Failed to send " + type + " email: " + e.getMessage());
        }
    }
}