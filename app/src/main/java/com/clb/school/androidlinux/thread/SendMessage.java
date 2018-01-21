package com.clb.school.androidlinux.thread;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.clb.school.androidlinux.MainActivity;

import java.io.IOException;
import java.io.OutputStream;


/**
 * Created by Administrator on 2018/1/3.
 */

public class SendMessage extends Thread {

    private OutputStream outputStream;
    private Handler handler;
    private String msg;

    public SendMessage(OutputStream outputStream, Handler handler,String msg){
        this.outputStream = outputStream;
        this.handler = handler;
        this.msg = msg;
    }

    @Override
    public void run(){
        if(outputStream != null && msg != null){
            try{
                outputStream.write(msg.getBytes());
                Message message = Message.obtain();
                message.what = MainActivity.SEND_MSG_SUCCSEE;
                Log.w("SendMessage","发送成功");
                Bundle bundle = new Bundle();
                bundle.putString("SMG",msg);
                message.setData(bundle);
                handler.sendMessage(message);
            }catch (IOException e){
                e.printStackTrace();
                Message message = Message.obtain();
                message.what = MainActivity.SEND_MSG_ERROR;
                Bundle bundle = new Bundle();
                bundle.putString("MSG",msg);
                message.setData(bundle);
                handler.sendMessage(message);
            }
        }
    }

}
