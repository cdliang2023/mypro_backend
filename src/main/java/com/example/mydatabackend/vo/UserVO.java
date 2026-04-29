package com.example.mydatabackend.vo;

public class UserVO {

    private String usrName;
    private String qqnum;
    private String address;
    private String avatar;
    private Integer downloadRight;

    public String getUsrName() {
        return usrName;
    }

    public void setUsrName(String usrName) {
        this.usrName = usrName;
    }

    public String getQqnum() {
        return qqnum;
    }

    public void setQqnum(String qqnum) {
        this.qqnum = qqnum;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public Integer getDownloadRight() {
        return downloadRight;
    }

    public void setDownloadRight(Integer downloadRight) {
        this.downloadRight = downloadRight;
    }
}