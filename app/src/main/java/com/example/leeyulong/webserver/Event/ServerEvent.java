package com.example.leeyulong.webserver.Event;

public class ServerEvent {

    String serverStatus = "";

    public ServerEvent(String status){
        this.serverStatus = status;
    }

    public String getServerStatus(){
        return serverStatus;
    }

}
