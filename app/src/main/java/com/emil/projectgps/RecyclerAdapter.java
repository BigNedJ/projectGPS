package com.emil.projectgps;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

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

        TextView textView = (TextView) LayoutInflater.from(parent.getContext()).inflate(R.layout.text_view_layout, parent, false);
        MyViewHolder myViewHolder = new MyViewHolder(textView, context);

        return myViewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
    holder.names.setText(list.get(position).getUsername());
    }

    private static List<UsernameAndID> getList(){
        return list;
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{

        TextView names;
        Context context;
        List<UsernameAndID> list = getList();
        public MyViewHolder(@NonNull TextView itemView, Context context) {
            super(itemView);
            names = itemView;
            this.context = context;
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
           String id = getList().get(getAdapterPosition()).getId();

            Toast.makeText(context, "ID:"+id, Toast.LENGTH_LONG).show();
            Log.d("Recycler", "ID "+id);

        }
    }

}
