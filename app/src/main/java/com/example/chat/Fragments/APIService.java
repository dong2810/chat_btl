package com.example.chat.Fragments;

import com.example.chat.Notification.MyResponse;
import com.example.chat.Notification.Sender;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface APIService {
        @Headers({
                "Content-Type:application/json",
                "Authorization:key=AAAAx21e1Gs:APA91bG-GWKVNMs5qR9R-cTPbDjt_parSgaStTzC4uawx0S80O462PDhZdMRTTxKXXAkMZNcz0ZD225Eq43HDksTisrpU6K0WkgZ3Qv-3qnzBh0TCGmmbznLxMAuZMk2X2l9vRUXoheD"
        })

    @POST("fcm/send")
    Call<MyResponse> sendNotification(@Body Sender body);
}
