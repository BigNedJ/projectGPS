package com.emil.projectgps;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class AboutUsActivity extends AppCompatActivity {
    TextView textViewAboutUs;
    Button back;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about_us);

        textViewAboutUs=(TextView)findViewById(R.id.textAboutUs);
        back=(Button)findViewById(R.id.btnBack);

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(),MapsActivity.class));
            }
        });
        
        textViewAboutUs.setText("This application was created by five students at HKR (Högskolan Kristianstad)." +
                "The idea of this this app is a mix of two applications, Snapchat/Google Maps, where the user " +
                "will be able to view the map and chat with friends. There will be a menu where you can " +
                "access the Add Friends, View Friends, About the App and sign out activities." +
                "\n\n" +
                "We hope that you like our application called PseudoMaps," +
                "\n\n\nGreetings\n\n" +
                "Nedim, Bamlak, Emil, Jimmie and Petar");

    }
}
