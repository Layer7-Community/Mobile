package com.brcm.apim.magcertificatepinning;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;

public class TestListAdapter extends RecyclerView.Adapter<TestListAdapter.ViewHolder> {
    ArrayList<String> mTestNameList;
    Context context;
    private ItemClickListener mClickListener;

    // Constructor for initialization
    public TestListAdapter(Context context, ArrayList<String> arrTestName) {
        this.context = context;
        this.mTestNameList = arrTestName;
        Log.d("Testing", "Size::  "+mTestNameList.size());
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflating the Layout(Instantiates list_item.xml
        // layout file into View object)
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.test_list_item, parent, false);

        // Passing view to ViewHolder
        return new ViewHolder(context,view);
    }


    // Binding data to the into specified position
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        // TypeCast Object to int type
        holder.mTestName.setText((String) mTestNameList.get(position));
    }

    @Override
    public int getItemCount() {
        // Returns number of items
        // currently available in Adapter
        return mTestNameList.size();
    }

    // convenience method for getting data at click position
    String getItem(int id) {
        return mTestNameList.get(id);
    }

    // Initializing the Views
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView mTestName;

        public ViewHolder(Context context, View view) {
            super(view);
            mTestName = (TextView) view.findViewById(R.id.test_name);
            view.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            Log.d("Testing", "Onclick called");
            if (mClickListener != null){
                Log.d("Testing", "Onclick called 222");
                mClickListener.onItemClick(v, getLayoutPosition());
            }
        }
    }

    // allows clicks events to be caught
    void setClickListener(ItemClickListener itemClickListener) {
        Log.d("Testing", "setClickListener called");
        this.mClickListener = itemClickListener;
    }

    // parent activity will implement this method to respond to click events
    public interface ItemClickListener {
        void onItemClick(View view, int position);
    }
}