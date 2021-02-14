package com.plaid.donate.porcelain.models;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.plaid.donate.porcelain.R;
import com.plaid.donate.porcelain.db.DBManager;

import java.util.ArrayList;

public class ItemAdapter extends BaseAdapter {

    private int layout;
    private ArrayList<Items> recordList;
    private Context context;
    private DBManager dbManager;

    public ItemAdapter(Context context, int layout, ArrayList<Items> recordList) {
        this.context = context;
        this.recordList = recordList;
        this.layout = layout;
    }

    public int getCount() {
        return recordList.size();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    public Object getItems(int position) {
        return recordList.get(position);
    }

    public long getItemsId(int position) {
        return position;
    }

    private class ViewHolder {

        ImageView itemImageView;
        TextView  itemNameTextView;
        TextView  itemTypeTextView;
        Button    claimButton;
    }


    public View getView(int position, View convertView, ViewGroup parent) {

        View v = convertView;
        ViewHolder holder = new ViewHolder();
        if (v == null) {
            LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = layoutInflater.inflate(layout, parent, false);
            holder.itemImageView = v.findViewById(R.id.item_images);
            holder.itemNameTextView = v.findViewById(R.id.item_names);
            holder.itemTypeTextView = v.findViewById(R.id.item_types);
            holder.claimButton = v.findViewById(R.id.claim_btn);
            holder.claimButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    dbManager = new DBManager(context);
                    dbManager.open();
                    Cursor data = dbManager.getData("SELECT * FROM DONATION");
                    while (data.moveToNext()) {
                        double getTotalDonate = data.getDouble(4);
                        if(getTotalDonate != 15.00){
                            View customLayout = LayoutInflater.from(context).inflate(R.layout.activity_claim_dialog, null);
                            @SuppressLint("ResourceType") AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.CustomDialogTheme);
                            builder.setView(customLayout);
                            AlertDialog claimAlert=builder.create();
                            claimAlert.show();

                            Button okBtn = customLayout.findViewById(R.id.okay_btn);
                            okBtn.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    claimAlert.dismiss();
                                }
                            });
                        }else{
                            Toast.makeText(context, "Claim clicked", Toast.LENGTH_SHORT).show();
                        }
                    }
                    data.close();
                }
            });

            v.setTag(holder);
        } else {
            holder = (ViewHolder) v.getTag();
        }


        Items datalist = recordList.get(position);
        holder.itemNameTextView.setText(datalist.getName());
        holder.itemTypeTextView.setText(datalist.getTypeName());

        byte[] recordImage = datalist.getImage();
        Bitmap bitmap = BitmapFactory.decodeByteArray(recordImage, 0, recordImage.length);
        holder.itemImageView.setImageBitmap(bitmap);


        return v;
    }
}