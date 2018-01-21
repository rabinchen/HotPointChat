package com.clb.school.androidlinux;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.clb.school.androidlinux.thread.GetMessage;
import com.clb.school.androidlinux.thread.SendMessage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * Created by Administrator on 2018/1/3.
 */

public class ConnectThread extends Thread {
    private final Socket socket;
    private Handler handler;
    private InputStream inputStream;
    private OutputStream outputStream;
    private String SMD;
    public static final int SENDDATA = 1;

    public ConnectThread(Socket socket, Handler handler,String data){
        setName("ConnectThread");
        this.socket = socket;
        this.handler = handler;
        SMD = data;
    }

    @Override
    public void run() {
        Log.w("AAA","链接线程");
        if(socket==null){
            Log.w("AAA","socket null");
            return;
        }
        handler.sendEmptyMessage(MainActivity.DEVICE_CONNECTED);
        try {
            //获取数据流
            inputStream = socket.getInputStream();
            new GetMessage(inputStream,handler).start();
            outputStream = socket.getOutputStream();
            if(SMD != null){
                new SendMessage(outputStream,handler,SMD).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Socket getSocket(){
        return socket;
    }

}

