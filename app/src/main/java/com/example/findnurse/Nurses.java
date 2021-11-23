package com.example.findnurse;

public class Nurses {
    public String nurse_name, document_id, FCM_token, auth_id;

    public Nurses(String nurseName, String documentId, String FCM_token, String auth_id) {
        this.nurse_name = nurseName;
        this.document_id = documentId;
        this.FCM_token = FCM_token;
        this.auth_id = auth_id;
    }
}