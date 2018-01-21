package com.clb.school.androidlinux;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by Administrator on 2018/1/3.
 */

public class ListenThread extends Thread {
    private ServerSocket serverSocket = null;
    private Handler handler;
    private int port;
    private Socket socket;

    public ListenThread(int port, Handler handler){
        setName("ListenerThread");
        this.port = port;
        this.handler = handler;
        try {
            serverSocket=new ServerSocket(port);//监听本机的12345端口
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void run() {
        while (true){
            try {
                if(serverSocket == null){
                    break;
                }
                if(serverSocket.isClosed()){
                    break;
                }
                //阻塞，等待设备连接
                Log.w("AAA","阻塞");
                socket = serverSocket.accept();
                Message message = Message.obtain();
                message.what = MainActivity.DEVICE_CONNECTING;
                handler.sendMessage(message);
            } catch (IOException e) {
                Log.w("AAA","error:"+e.getMessage());
                e.printStackTrace();
            }
        }
        try {
            if(socket != null){
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Socket getSocket() {
        return socket;
    }

    public ServerSocket getServerSocket(){
        return serverSocket;
    }
}

