package com.example.chat.Notification;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class Client {
    //nhan du lieu tu server qua restful api
    private static Retrofit retrofit = null;
    public  static Retrofit getClient (String url){
        if (retrofit == null){
            retrofit= new Retrofit.Builder()
                    .baseUrl(url)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return  retrofit;
    }
}
