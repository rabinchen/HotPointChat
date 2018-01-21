package com.clb.school.androidlinux.adapter;

import android.net.wifi.ScanResult;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.clb.school.androidlinux.R;

import java.util.List;

/**
 * Created by Administrator on 2018/1/3.
 */

public class MyAdapter extends RecyclerView.Adapter<MyAdapter.MyViewHolder> implements View.OnClickListener{

    List<ScanResult> scanResults;
    ItemClickListener listener;

    public interface ItemClickListener{
        void onItemClick(int position);
    }

    public MyAdapter(List<ScanResult> scanResults){
        this.scanResults = scanResults;
    }

    public void setScanResults(List<ScanResult> scanResults){
        this.scanResults = scanResults;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.wifi_item,parent,false);
        view.setOnClickListener(this);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        holder.wifiName.setText(scanResults.get(position).SSID);
        int level = scanResults.get(position).level;
        if(level <= 0 && level > -50){
            holder.wifiSignal.setText("信号极好");
        }else if(level < -50 && level >= -70){
            holder.wifiSignal.setText("信号较好");
        }else if(level < -70 && level >= -80){
            holder.wifiSignal.setText("信号一般");
        }else if(level < -80 && level >= -100){
            holder.wifiSignal.setText("信号较差");
        }
        holder.itemView.setTag(position);
    }

    @Override
    public int getItemCount() {
        return scanResults.size();
    }

    class MyViewHolder extends RecyclerView.ViewHolder{

        public TextView wifiName;
        public TextView wifiSignal;

        public MyViewHolder(View view){
            super(view);
            wifiName = (TextView)view.findViewById(R.id.wifi_name);
            wifiSignal = (TextView)view.findViewById(R.id.wifi_signal);
        }
    }

    public void setListener(ItemClickListener listener){
        this.listener = listener;
    }

    @Override
    public void onClick(View view){
        listener.onItemClick((int)view.getTag());
    }
}
