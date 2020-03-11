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
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterUserActivity extends AppCompatActivity {
    public static final String TAG="TAG";
    EditText userName, email, password;
    Button btnRegister;
    TextView textView;
    FirebaseFirestore fStore;
    private FirebaseAuth fAuth;
    String userID;
    ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_user);

        userName=(EditText)findViewById(R.id.userName);
        email=(EditText) findViewById(R.id.email);
        password=(EditText)findViewById(R.id.password);
        btnRegister=(Button)findViewById(R.id.btnRegister);
        progressBar=(ProgressBar)findViewById(R.id.progressBar);
        textView=(TextView)findViewById(R.id.textViewLogin);

        fAuth=FirebaseAuth.getInstance();
        fStore=FirebaseFirestore.getInstance();

        if (fAuth.getCurrentUser() != null){
            startActivity(new Intent(getApplicationContext(),Login.class));
            finish();
        }

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String mail=email.getText().toString().trim();
                String lösenord=password.getText().toString().trim();
                final String mUsername=userName.getText().toString().trim();

                if (TextUtils.isEmpty(mail)){
                    email.setError("Email is required.");
                }
                if (TextUtils.isEmpty(lösenord)){
                    password.setError("Password is required.");
                }
                if (TextUtils.isEmpty(mUsername)){
                    userName.setError("Username is required.");
                }
                if (lösenord.length()<6){
                    password.setError("Password must be at least 6 chars");
                }

                progressBar.setVisibility(View.VISIBLE);

                fAuth.createUserWithEmailAndPassword(mail,lösenord).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()){
                            Toast.makeText(RegisterUserActivity.this,"User Registered",Toast.LENGTH_SHORT).show();
                            userID=fAuth.getCurrentUser().getUid();
                            DocumentReference documentReference=fStore.collection("users").document(userID);
                            final Map<String,Object> user=new HashMap<>();
                            user.put("Username",mUsername);
                            user.put("Email",mail);

                            documentReference.set(user).addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    Log.d(TAG, "onSuccess: user Profile created for " + userID);
                                }
                            }).addOnFailureListener(new OnFailureListener() {

                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Log.d(TAG,"onFailture: "+e.toString());
                                }

                            });

                            startActivity(new Intent(getApplicationContext(),Login.class));

                        }else {
                            Toast.makeText(RegisterUserActivity.this,"Registration Failed: "+ task.getException().getMessage(),Toast.LENGTH_SHORT).show();
                            progressBar.setVisibility(View.GONE);
                        }
                    }
                });
            }
        });

        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(),Login.class));
            }
        });
    }
}
