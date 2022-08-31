package com.example.servicebestpractice;

public class Data {
    //发布包下载地址
    private String url;
    private String version;
    //更新策略
    private String upPolicy;
    //提示多少次
    private Number hintNum;
    //发布包md5
    private String md5;
    //发布包大小
    private Number size;
    //版本升级提示信息
    private String noticeMsg;
    //任务描述
    private String desc;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getUpPolicy() {
        return upPolicy;
    }

    public void setUpPolicy(String upPolicy) {
        this.upPolicy = upPolicy;
    }

    public Number getHintNum() {
        return hintNum;
    }

    public void setHintNum(Number hintNum) {
        this.hintNum = hintNum;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public Number getSize() {
        return size;
    }

    public void setSize(Number size) {
        this.size = size;
    }

    public String getNoticeMsg() {
        return noticeMsg;
    }

    public void setNoticeMsg(String noticeMsg) {
        this.noticeMsg = noticeMsg;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }
}
