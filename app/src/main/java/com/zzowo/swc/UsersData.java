package com.zzowo.swc;

public class UsersData {
    String email, FullName;

    public UsersData(String email, String name) {
        this.email = email;
        this.FullName = name;
    }

    public UsersData() {
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return FullName;
    }

    public void setFullName(String fullName) {
        this.FullName = fullName;
    }

}
