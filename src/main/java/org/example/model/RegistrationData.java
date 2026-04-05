package org.example.model;

import java.io.Serializable;
import java.util.List;

public class RegistrationData implements Serializable {
    private String fullname;
    private String email;
    private String phone;
    private String gender;
    private String institute;
    private String city;
    private String state;
    private String medcouncil;
    private String registration;
    private List<String> workshops;
    private String accompany;
    private String txnid;
    private String txndate;
    private String totalAmount;
    private String delegateId;

    // Constructors
    public RegistrationData() {
    }

    public RegistrationData(String fullname, String email, String phone, String gender,
                            String institute, String city, String state, String medcouncil,
                            String registration, List<String> workshops, String accompany,
                            String txnid, String txndate, String totalAmount, String delegateId) {
        this.fullname = fullname;
        this.email = email;
        this.phone = phone;
        this.gender = gender;
        this.institute = institute;
        this.city = city;
        this.state = state;
        this.medcouncil = medcouncil;
        this.registration = registration;
        this.workshops = workshops;
        this.accompany = accompany;
        this.txnid = txnid;
        this.txndate = txndate;
        this.totalAmount = totalAmount;
        this.delegateId = delegateId;
    }

    // Getters and Setters
    public String getFullname() {
        return fullname;
    }

    public void setFullname(String fullname) {
        this.fullname = fullname;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getInstitute() {
        return institute;
    }

    public void setInstitute(String institute) {
        this.institute = institute;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getMedcouncil() {
        return medcouncil;
    }

    public void setMedcouncil(String medcouncil) {
        this.medcouncil = medcouncil;
    }

    public String getRegistration() {
        return registration;
    }

    public void setRegistration(String registration) {
        this.registration = registration;
    }

    public List<String> getWorkshops() {
        return workshops;
    }

    public void setWorkshops(List<String> workshops) {
        this.workshops = workshops;
    }

    public String getAccompany() {
        return accompany;
    }

    public void setAccompany(String accompany) {
        this.accompany = accompany;
    }

    public String getTxnid() {
        return txnid;
    }

    public void setTxnid(String txnid) {
        this.txnid = txnid;
    }

    public String getTxndate() {
        return txndate;
    }

    public void setTxndate(String txndate) {
        this.txndate = txndate;
    }

    public String getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(String totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getDelegateId() {
        return delegateId;
    }

    public void setDelegateId(String delegateId) {
        this.delegateId = delegateId;
    }
}

