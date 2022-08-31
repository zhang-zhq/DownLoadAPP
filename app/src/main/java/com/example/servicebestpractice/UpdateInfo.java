package com.example.servicebestpractice;

import org.json.JSONObject;

public class UpdateInfo {

        private String code;
        private String msg; //返回状态码的描述
        private Data data;


        public String getCode() {
                return code;
        }

        public void setCode(String code) {
                this.code = code;
        }

        public String getMsg() {
                return msg;
        }


        public void setMsg(String msg) {
                this.msg = msg;
        }

        public Data getData() {
                return data;
        }

        public void setData(Data data) {
                this.data = data;
        }
}
