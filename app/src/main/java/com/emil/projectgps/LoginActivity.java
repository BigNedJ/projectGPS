package com.emil.projectgps;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    public static final String TAG = "LoginActivity";

    EditText mEmail,mPassword;
    Button button;
    TextView textView;
    ProgressBar progressBar;
    FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mEmail=(EditText)findViewById(R.id.email);
        mPassword=(EditText)findViewById(R.id.password);
        button=(Button)findViewById(R.id.btnLogin);
        textView=(TextView)findViewById(R.id.textViewRegister);
        progressBar=(ProgressBar)findViewById(R.id.progressBar2);
        progressBar.setVisibility(View.INVISIBLE);
        firebaseAuth = FirebaseAuth.getInstance();

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String mail=mEmail.getText().toString().trim();
                String lösenord=mPassword.getText().toString().trim();

                if (TextUtils.isEmpty(mail)){
                    mEmail.setError("Email is required.");
                    return;
                }
                if (TextUtils.isEmpty(lösenord)){
                    mPassword.setError("Password is required.");
                    return;
                }
                if (lösenord.length()<6){
                    mPassword.setError("Password must be at least 6 chars");
                    return;
                }

                progressBar.setVisibility(View.INVISIBLE);




                firebaseAuth.signInWithEmailAndPassword(mail,lösenord).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {

                        if (task.isSuccessful()){
                            Toast.makeText(LoginActivity.this,"Successful Login",Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(getApplicationContext(),MapsActivity.class));
                            progressBar.setVisibility(View.VISIBLE);

                        }else {
                            progressBar.setVisibility(View.INVISIBLE);
                            Toast.makeText(LoginActivity.this,"Error: "+task.getException().getMessage(),Toast.LENGTH_SHORT).show();
                        }

                    }
                });
                progressBar.setVisibility(View.INVISIBLE);

            }
        });

        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(),RegisterUserActivity.class));
            }
        });
    }
}
