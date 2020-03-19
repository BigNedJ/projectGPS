package com.emil.projectgps;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.TextView;

public class AboutUsActivity extends AppCompatActivity {
    TextView textViewAboutUs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about_us);

        textViewAboutUs=(TextView)findViewById(R.id.textAboutUs);
        textViewAboutUs.setText("This application was created by five students at HKR (Högskolan Kristianstad)." +
                "The idea of this this app is a mix of two applications, Snapchat/Google Maps, where the user " +
                "will be able to view the map and chat with friends. There will be a menu where you can" +
                "access the Add Friends, View Friends, About the App och sign out activities." +
                "" +
                "We hope that you like your application called PseudoMaps," +
                "Greetings " +
                "Nedim, Bamlak, Emil, Jimmie and Petar");

    }
}
