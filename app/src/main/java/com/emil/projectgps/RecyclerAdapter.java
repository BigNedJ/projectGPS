package com.emil.projectgps;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

//this class represents an adapter for tthe recycle view
public class RecyclerAdapter extends RecyclerView.Adapter<RecyclerAdapter.MyViewHolder> {


    private static List<UsernameAndID> list;
    private Context context;




    public RecyclerAdapter(List<UsernameAndID> list, Context context) {
        this.list = list;
        this.context = context;

    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.text_view_layout, parent, false);
        MyViewHolder myViewHolder = new MyViewHolder(view, context);

        return myViewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        holder.textView.setText(list.get(position).getUsername());
    }

    private static List<UsernameAndID> getList(){
        return list;
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        FirebaseFirestore firestore;

        TextView textView;
        Button button;
        View names;
        Context context;
        List<UsernameAndID> list = getList();
        public MyViewHolder(@NonNull View itemView, final Context context) {
            super(itemView);
            firestore = FirebaseFirestore.getInstance();
            button = itemView.findViewById(R.id.trackButton);
            textView =itemView.findViewById(R.id.listViewItem);
            names = itemView;
            this.context = context;
            itemView.setOnClickListener(this);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String name = getList().get(getAdapterPosition()).getUsername();
                    String userID = getList().get(getAdapterPosition()).getId();
                    //Toast.makeText(context, "Button pressed on "+name, Toast.LENGTH_LONG).show();
                    checkIfUserSharesPosition(userID, name, v);


                }
            });
        }

        @Override
        public void onClick(View v) {
           String id = getList().get(getAdapterPosition()).getId();

            Toast.makeText(context, "ID:"+id, Toast.LENGTH_LONG).show();
            Log.d("Recycler", "ID "+id);

        }
        private void checkIfUserSharesPosition(final String userID, final String name, final View v){
            DocumentReference dR = firestore.collection("users").document(userID);
            dR.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                @Override
                public void onSuccess(DocumentSnapshot documentSnapshot) {
                    boolean shareLocation = false;
                    if (documentSnapshot.contains("shareLocation")) {
                        shareLocation = documentSnapshot.getBoolean("shareLocation");
                    }
                    if (shareLocation){
                        Intent intent = new Intent(context, MapsActivity.class);
                        intent.putExtra("USER_NAME", name);
                        intent.putExtra("USER_ID",userID );
                        v.getContext().startActivity(intent);
                    } else{
                        Toast.makeText(context, name+" does not currently share position", Toast.LENGTH_LONG).show();
                    }

                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(context, "Error", Toast.LENGTH_LONG).show();
                }
            });
        }

    }



}
