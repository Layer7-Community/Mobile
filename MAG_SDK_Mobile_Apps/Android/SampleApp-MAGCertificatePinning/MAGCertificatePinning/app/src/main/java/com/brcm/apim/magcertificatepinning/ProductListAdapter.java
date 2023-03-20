package com.brcm.apim.magcertificatepinning;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.brcm.apim.magcertificatepinning.model.Product;
import java.util.ArrayList;

public class ProductListAdapter extends RecyclerView.Adapter<ProductListAdapter.ViewHolder> {
    ArrayList<Product> mTestNameList;
    Context context;
    private ProductListAdapter.ItemClickListener mClickListener;

    // Constructor for initialization
    public ProductListAdapter(Context context, ArrayList<Product> arrTestName) {
        this.context = context;
        this.mTestNameList = arrTestName;
    }

    @NonNull
    @Override
    public ProductListAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflating the Layout(Instantiates list_item.xml
        // layout file into View object)
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.product_list, parent, false);

        // Passing view to ViewHolder
        return new ProductListAdapter.ViewHolder(context,view);
    }


    // Binding data to the into specified position
    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull ProductListAdapter.ViewHolder holder, int position) {
        // TypeCast Object to int type
        holder.productName.setText("Flight Name : "+mTestNameList.get(position).getName());
        holder.flightStatus.setText("Flight Status : "+mTestNameList.get(position).getArrivalTime());
    }

    @Override
    public int getItemCount() {
        // Returns number of items
        // currently available in Adapter
        return mTestNameList.size();
    }

    // convenience method for getting data at click position
    Product getItem(int id) {
        return mTestNameList.get(id);
    }

    // Initializing the Views
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView productName,flightStatus;

        public ViewHolder(Context context, View view) {
            super(view);
            productName = (TextView) view.findViewById(R.id.productName);
            flightStatus = (TextView) view.findViewById(R.id.flightStatus);
            view.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            Log.d("ProductList", "Product list Onclick called");
            if (mClickListener != null){
                Log.d("Testing", "Product list Onclick called :" + getLayoutPosition());
                mClickListener.onItemClick(v, getLayoutPosition());
            }
        }
    }

    // allows clicks events to be caught
    void setClickListener(ProductListAdapter.ItemClickListener itemClickListener) {
        Log.d("Testing", "setClickListener called");
        this.mClickListener = itemClickListener;
    }

    // parent activity will implement this method to respond to click events
    public interface ItemClickListener {
        void onItemClick(View view, int position);
    }
}