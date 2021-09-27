package com.example.ev_android;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class MyAdapter extends RecyclerView.Adapter<MyAdapter.MyViewHolder> {

    Context context;

    ArrayList<FB_info> list;

    public MyAdapter(Context context, ArrayList<FB_info> list) {
        this.context = context;
        this.list = list;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item, parent, false);
        return new MyViewHolder(v);
    }

    @Override
    // HomeFragment에서 받아온 정보 리사이클러뷰에 저장
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {

        FB_info info = list.get(position);
        holder.battery.setText(setBattery(info.getBattery()));
        holder.in_time.setText(info.getIN_TIME());

        holder.using.setText(setUsing(info.getUSING()));

        holder.batterybar.setProgress(Integer.parseInt(info.getBattery()));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder{

        TextView battery, in_time, using;
        ProgressBar batterybar;


        // 리사이클러뷰에 들어갈 하위 뷰들
        public MyViewHolder(@NonNull View itemView) {
            super(itemView);

            battery = itemView.findViewById(R.id.tvBattery);
            in_time = itemView.findViewById(R.id.tvIn_time);
            using = itemView.findViewById(R.id.tvOut_time);

            batterybar = itemView.findViewById(R.id.tvbatterybar);
        }
    }

    public String setUsing(String use){
        if (use.equals("X")){
            return "NOT USING";
        }
        else {
            return "USING";
        }
    }

    public String setBattery(String batt){
        if (batt.equals("-1")){
            return "-";
        }
        else {
            return batt+ " %";
        }
    }
}