package com.emil.projectgps;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;

public class AddNewFriend extends AppCompatActivity {

    private static final String TAG = "AddNewFriend";


    String currentUsersUsername;
    String currentUserID;
    EditText nameText;
    Button addButton;
    // Access a Cloud Firestore instance from your Activity
    FirebaseAuth firebaseAuth;
    FirebaseFirestore db;
    FirebaseUser firebaseUser;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_new_friend);

        db = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();

        nameText = findViewById(R.id.nameEditText_add_new_friend);
        addButton = findViewById(R.id.addButton_add_new_friend);

        currentUserID = FirebaseAuth.getInstance().getCurrentUser().getUid();


        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                final String userName = nameText.getText().toString().trim();
                final CollectionReference usersRef = db.collection("users");
                final Query query = usersRef.whereEqualTo("Username", userName);

                query.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if(task.isSuccessful()){
                            for(DocumentSnapshot documentSnapshot : task.getResult()){
                                String user = documentSnapshot.getString("Username");

                                if (user.equals(userName) && documentSnapshot.getId().equals(currentUserID)){
                                    Toast.makeText(AddNewFriend.this, "You cant add yourself as a friend", Toast.LENGTH_LONG).show();
                                    break;
                                }

                                if(user.equals(userName)){
                                    Log.d(TAG, "User Exists");
                                    Toast.makeText(AddNewFriend.this, "Username exists, document id: "+documentSnapshot.getId(), Toast.LENGTH_LONG).show();
                                   addFriend(usersRef, user, documentSnapshot);
                                }
                            }
                        }
                        if(task.getResult().size() == 0 ){
                            Log.d(TAG, "User not Exists");
                            Toast.makeText(AddNewFriend.this, "Username doesn't exist", Toast.LENGTH_LONG).show();
                            //You can store new user information here

                        }
                    }
                });
            }
        });

    }

    public void addFriend(final CollectionReference cR, String username, final DocumentSnapshot dS){
        final String currentUserID= firebaseUser.getUid();
        //friend hashmap used for storing friend username in current user's Friend collection
        final Map<String, Object> friend = new HashMap<>();
        //currentuser hashmap used for storing current users username in friend's Friend collection
        final Map<String, Object> currentUserMap = new HashMap<>();
        final String friendUserID = dS.getId();
        friend.put("friend", username);

        Log.d(TAG, "Creating docsnap");

        // Gets user document from Firestore as reference
        DocumentReference docRef = db.collection("users").document(currentUserID);
        docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    Log.d(TAG, "Document exists");
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        Log.d(TAG, "Document exists");

                        currentUsersUsername = document.getString("Username");
                        Log.d(TAG, "Current users username: "+ currentUsersUsername);

                        //Create new document, with document ID as of friend
                        cR.document(currentUserID).collection("Friends").document(friendUserID).set(friend);
                        currentUserMap.put("friend", currentUsersUsername);
                        //Create new document, with document ID as of friend
                        cR.document(dS.getId()).collection("Friends").document(currentUserID).set(currentUserMap);
                    } else {
                        Log.d(TAG, "No such document");
                    }
                } else {
                    Log.d(TAG, "get failed with ", task.getException());
                }
            }
        });
        Log.d(TAG, "Docsnap created");
    }
}
