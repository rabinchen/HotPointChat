package com.clb.school.androidlinux.bean;

/**
 * Created by Administrator on 2018/1/3.
 */

public class Content {

    public String content;//信息内容
    public int type;//信息类型

    public Content(String content,int type){
        this.content = content;
        this.type = type;
    }

    public String getContent(){
        return content;
    }

    public int getType(){
        return type;
    }

}
