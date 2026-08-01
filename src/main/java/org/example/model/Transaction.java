package org.example.model;

import java.io.Serializable;

public class Transaction implements Serializable {
    private String txnid;
    private String txndate;
    private String paymentimg;
    private String totalAmount;
    private String regstatus;

    public Transaction() {
    }

    public Transaction(String txnid, String txndate, String paymentimg, String totalAmount, String regstatus) {
        this.txnid = txnid;
        this.txndate = txndate;
        this.paymentimg = paymentimg;
        this.totalAmount = totalAmount;
        this.regstatus = regstatus;
    }

    public String getTxnid() { return txnid; }
    public void setTxnid(String txnid) { this.txnid = txnid; }

    public String getTxndate() { return txndate; }
    public void setTxndate(String txndate) { this.txndate = txndate; }

    public String getPaymentimg() { return paymentimg; }
    public void setPaymentimg(String paymentimg) { this.paymentimg = paymentimg; }

    public String getTotalAmount() { return totalAmount; }
    public void setTotalAmount(String totalAmount) { this.totalAmount = totalAmount; }

    public String getRegstatus() { return regstatus; }
    public void setRegstatus(String regstatus) { this.regstatus = regstatus; }
}
