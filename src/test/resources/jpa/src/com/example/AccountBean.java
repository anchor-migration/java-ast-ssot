package com.example;

@javax.persistence.Entity
@javax.persistence.Table(name = "ACCOUNT")
public class AccountBean {
    @javax.persistence.Id
    @javax.persistence.Column(name = "ACCOUNT_ID")
    private String accountId;

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }
}
