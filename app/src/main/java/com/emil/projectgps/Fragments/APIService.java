package com.emil.projectgps.Fragments;

import com.emil.projectgps.Notifications.MyResponse;
import com.emil.projectgps.Notifications.Sender;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface APIService {
    @Headers(
            {
                    "Content-Type:application/json",
                    "Authorization:key=AAAAQ8QHOBU:APA91bGt60Cg1zJLDkszbUzm48d0aTOsUzha6ZJbkxVtxuQpNM6csWq4_9VHhcSjUz-oP7hwOIAKvt77uo3Aqu2qPEWOIgVDWvYkAOI3aS163SWsatHTdMoA2c3kVc6wdS0ozpMXGYxi"
                    //firebase > settings >cloud messaging > token server key
            }
    )

    @POST("fcm/send")
    Call<MyResponse> sendNotification(@Body Sender body);
}
