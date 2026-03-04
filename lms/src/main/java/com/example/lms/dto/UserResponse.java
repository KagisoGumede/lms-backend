package com.example.lms.dto;

import com.example.lms.model.User;

public class UserResponse {

    public Long id;
    public String firstName;
    public String surname;
    public String emailAddress;
    public String department;
    public String role;
    public Long managerId;
    public String managerName;

    public static UserResponse from(User user) {
        UserResponse res = new UserResponse();
        res.id = user.getId();
        res.firstName = user.getFirstName();
        res.surname = user.getSurname();
        res.emailAddress = user.getEmailAddress();
        res.department = user.getDepartment();
        res.role = user.getRole() != null ? user.getRole().name() : null;
        if (user.getManager() != null) {
            res.managerId = user.getManager().getId();
            res.managerName = user.getManager().getFirstName() + " " + user.getManager().getSurname();
        }
        return res;
    }
}
