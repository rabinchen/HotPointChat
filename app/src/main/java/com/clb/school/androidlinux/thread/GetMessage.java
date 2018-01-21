package com.clb.school.androidlinux.thread;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.clb.school.androidlinux.MainActivity;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by Administrator on 2018/1/3.
 */

public class GetMessage extends Thread {

    private InputStream inputStream;
    private Handler handler;

    public GetMessage(InputStream InputStream,Handler handler){
        this.inputStream = InputStream;
        this.handler = handler;
    }

    @Override
    public void run(){
        byte[] buffer = new byte[1024];
        int bytes;
        try{
            while(true){
                //读取输入的数据
                bytes = inputStream.read(buffer);
                if(bytes > 0){
                    final byte[] msg = new byte[bytes];
                    System.arraycopy(buffer, 0, msg, 0, bytes);
                    Message message = Message.obtain();
                    message.what = MainActivity.GET_MSG;
                    Bundle bundle = new Bundle();
                    bundle.putString("GSG",new String(msg));
                    message.setData(bundle);
                    handler.sendMessage(message);
                }
            }
        }catch (IOException e){

        }
    }

}
