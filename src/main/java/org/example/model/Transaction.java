package org.example.model;

import java.io.Serializable;

public class Transaction implements Serializable {
    private String txnid;
    private String txndate;

    public Transaction() {
    }

    public Transaction(String txnid, String txndate) {
        this.txnid = txnid;
        this.txndate = txndate;
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
}
