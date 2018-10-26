package com.example.leeyulong.webserver;

import android.util.Log;

import com.example.leeyulong.webserver.Event.ServerEvent;
import com.yanzhenjie.andserver.AndServer;
import com.yanzhenjie.andserver.Server;

import org.greenrobot.eventbus.EventBus;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

public class ServerManager {

    public static final String TAG = "ServerManager";

    private Server server;

    public ServerManager(String ip, int port){
        try {
            InetAddress inetAddress = InetAddress.getByName(ip);
            server = AndServer.serverBuilder()
                    .inetAddress(inetAddress)
                    .port(port)
                    .timeout(20, TimeUnit.SECONDS)
                    .listener(new Server.ServerListener() {
                        @Override
                        public void onStarted() {
                            EventBus.getDefault().post(new ServerEvent("on"));
                            Log.e(TAG,"Server onStarted");
                        }

                        @Override
                        public void onStopped() {
                            Log.e(TAG,"Server onStopped");
                            EventBus.getDefault().post(new ServerEvent("off"));
                        }

                        @Override
                        public void onException(Exception e) {
                            Log.e(TAG,"Server onException");
                            Log.e(TAG, e.getMessage().toString());
                            EventBus.getDefault().post(new ServerEvent("exception: "+e.getMessage().toString()));

                        }
                    })
                    .build();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }


    public void startServer(){
        if(server != null){
            if(!server.isRunning()){
                server.startup();
            }
        }
    }

    public void stopServer(){
        if(server != null){
            if(server.isRunning()) {
               server.shutdown();
            }
        }
    }



}
