package com.hamada.android.talktome;

public class AllUserContacts {

    public String user_name,user_image,user_thumb_image;

    public AllUserContacts() {
    }

    public AllUserContacts(String user_name, String user_image,String user_thumb_image) {
        this.user_name = user_name;
        this.user_image = user_image;
        this.user_thumb_image=user_thumb_image;
    }

    public String getUser_name() {
        return user_name;
    }

    public void setUser_name(String user_name) {
        this.user_name = user_name;
    }

    public String getUser_image() {
        return user_image;
    }

    public void setUser_image(String user_image) {
        this.user_image = user_image;
    }

    public String getUser_thumb_image() {
        return user_thumb_image;
    }

    public void setUser_thumb_image(String user_thumb_image) {
        this.user_thumb_image = user_thumb_image;
    }
}
