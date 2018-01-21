package com.clb.school.androidlinux.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.clb.school.androidlinux.R;
import com.clb.school.androidlinux.bean.Content;

import java.util.List;

/**
 * Created by Administrator on 2018/1/3.
 */

public class ContentAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    List<Content> contentList;

    public static int LEFT = 1;
    public static int RIGHT = 2;

    public ContentAdapter(List<Content> contentList){
        this.contentList = contentList;
    }

    @Override
    public int getItemViewType(int position) {
        if(contentList.get(position).getType() == LEFT){
            return LEFT;
        }else{
            return RIGHT;
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if(viewType == LEFT){
            return new ContentLeftVH(LayoutInflater.from(parent.getContext()).inflate(R.layout.content_left_item,parent,false));
        }else{
            return new ContentRightVH(LayoutInflater.from(parent.getContext()).inflate(R.layout.content_right_item,parent,false));
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if(holder instanceof ContentLeftVH){
            ((ContentLeftVH)holder).textView.setText(contentList.get(position).getContent());
        }else{
            ((ContentRightVH)holder).textView.setText(contentList.get(position).getContent());
        }
    }

    @Override
    public int getItemCount() {
        return contentList.size();
    }

    class ContentLeftVH extends RecyclerView.ViewHolder{

        public TextView textView;

        public ContentLeftVH(View view){
            super(view);
            textView = (TextView)view.findViewById(R.id.send_content);
        }
    }

    class ContentRightVH extends RecyclerView.ViewHolder{

        public TextView textView;

        public ContentRightVH(View view){
            super(view);
            textView = (TextView)view.findViewById(R.id.send_content);
        }

    }

}
