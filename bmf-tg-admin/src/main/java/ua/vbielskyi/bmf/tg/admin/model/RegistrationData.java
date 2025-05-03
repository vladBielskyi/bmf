package ua.vbielskyi.bmf.tg.admin.model;

import lombok.Data;

import java.io.Serializable;

/**
 * Data class to hold user registration information
 */
@Data
public class RegistrationData implements Serializable {
    private static final long serialVersionUID = 1L;

    private String fullName;
    private String email;
    private String phoneNumber;
    private String preferredLanguage;
    private Boolean confirmed;

    // Additional profile information
    private String companyName;
    private String country;
    private String city;
    private String timeZone;
}