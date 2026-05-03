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
    private String designation;
    private String medcouncil;
    private String medcouncilregnum;
    private boolean isattendworkshop;
    private List<String> workshops;
    private long accompanycount;
    private List<String> txndetails;
    private String delegateId;
    private String pgbonafideimg;
    private String synopsis;

    // Legacy fallback fields – present in Firestore docs saved before the nerconTrx split
    private String txnid;
    private String txndate;
    private String totalAmount;
    private String regstatus;
    private String paymentimg;

    // Constructors
    public RegistrationData() {
    }

    public RegistrationData(String fullname, String email, String phone, String gender,
                            String institute, String city, String state, String designation,
                            String medcouncil, String medcouncilregnum,
                            boolean isattendworkshop, List<String> workshops, long accompanycount,
                            List<String> txndetails,
                            String delegateId, String pgbonafideimg) {
        this.fullname = fullname;
        this.email = email;
        this.phone = phone;
        this.gender = gender;
        this.institute = institute;
        this.city = city;
        this.state = state;
        this.designation = designation;
        this.medcouncil = medcouncil;
        this.medcouncilregnum = medcouncilregnum;
        this.isattendworkshop = isattendworkshop;
        this.workshops = workshops;
        this.accompanycount = accompanycount;
        this.txndetails = txndetails;
        this.delegateId = delegateId;
        this.pgbonafideimg = pgbonafideimg;
    }

    // Getters and Setters
    public String getFullname() { return fullname; }
    public void setFullname(String fullname) { this.fullname = fullname; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getInstitute() { return institute; }
    public void setInstitute(String institute) { this.institute = institute; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }

    public String getMedcouncil() { return medcouncil; }
    public void setMedcouncil(String medcouncil) { this.medcouncil = medcouncil; }

    public String getMedcouncilregnum() { return medcouncilregnum; }
    public void setMedcouncilregnum(String medcouncilregnum) { this.medcouncilregnum = medcouncilregnum; }

    public boolean isAttendworkshop() { return isattendworkshop; }
    public void setAttendworkshop(boolean isattendworkshop) { this.isattendworkshop = isattendworkshop; }

    public List<String> getWorkshops() { return workshops; }
    public void setWorkshops(List<String> workshops) { this.workshops = workshops; }

    public long getAccompanycount() { return accompanycount; }
    public void setAccompanycount(long accompanycount) { this.accompanycount = accompanycount; }

    public List<String> getTxndetails() { return txndetails; }
    public void setTxndetails(List<String> txndetails) { this.txndetails = txndetails; }

    public String getDelegateId() { return delegateId; }
    public void setDelegateId(String delegateId) { this.delegateId = delegateId; }

    public String getPgbonafideimg() { return pgbonafideimg; }
    public void setPgbonafideimg(String pgbonafideimg) { this.pgbonafideimg = pgbonafideimg; }

    public String getSynopsis() { return synopsis; }
    public void setSynopsis(String synopsis) { this.synopsis = synopsis; }

    public String getTxnid() { return txnid; }
    public void setTxnid(String txnid) { this.txnid = txnid; }

    public String getTxndate() { return txndate; }
    public void setTxndate(String txndate) { this.txndate = txndate; }

    public String getTotalAmount() { return totalAmount; }
    public void setTotalAmount(String totalAmount) { this.totalAmount = totalAmount; }

    public String getRegstatus() { return regstatus; }
    public void setRegstatus(String regstatus) { this.regstatus = regstatus; }

    public String getPaymentimg() { return paymentimg; }
    public void setPaymentimg(String paymentimg) { this.paymentimg = paymentimg; }
}

