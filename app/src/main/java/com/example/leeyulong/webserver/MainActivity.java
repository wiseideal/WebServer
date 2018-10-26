package com.example.leeyulong.webserver;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.pm.PermissionInfoCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.blankj.utilcode.util.PermissionUtils;
import com.blankj.utilcode.util.ShellUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.example.leeyulong.webserver.Event.ServerEvent;
import com.example.leeyulong.webserver.util.IPUtil;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    TextView infoView;

    String[] permissions = new String[]{
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CALL_PHONE
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onResume() {
        super.onResume();
        infoView = findViewById(R.id.txtview);
        infoView.post(new Runnable() {
            @Override
            public void run() {
                getPermission();
            }
        });
        findViewById(R.id.btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String ip = IPUtil.getIPAddress(MainActivity.this);
                int port = 8081;
                startServer(ip, port);
                infoView.setText("本地访问地址: "+ip+":"+port);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onServerEvent(ServerEvent event){
        String serverStatus = event.getServerStatus();
        if("on".equals(serverStatus)){
            excuteFRP();
        }else{
            ToastUtils.showShort(serverStatus);
            infoView.setText("服务器异常");
        }
    }

    private void excuteFRP(){
        ToastUtils.showShort("准备穿透");
        copyFrp();

        try {
            String rootPath = getFilesDir().getAbsolutePath().toString() + File.separator;
            String startFrmCmd = rootPath+"frpc -c "+rootPath+"frpc_android.ini";
            //  /data/user/0/com.example.leeyulong.webserver/files/frpc -c /data/user/0/com.example.leeyulong.webserver/files/frpc_android.ini
            Log.e("CMD", startFrmCmd);
            execCmd(startFrmCmd);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void copyFrp(){
        AssetManager am = getAssets();
        try {
            String frpBinary = "frpc";
            String frpIni = "frpc_android.ini";
            InputStream frpIns = am.open(frpBinary);
            byte[] buffer = new byte[1024];
            int len = 0;
            String dataPath = getFilesDir().getAbsolutePath().toString();
            File binaryFile = new File(dataPath + File.separator + frpBinary);
            ///data/user/0/com.example.leeyulong.webserver/files/frpc
            FileOutputStream fos = new FileOutputStream(binaryFile);
            while ((len = frpIns.read(buffer)) != -1){
                fos.write(buffer,0, len);
            }
            fos.flush();
            frpIns.close();
            fos.close();
            if(binaryFile.exists()){
                binaryFile.setExecutable(true);
            }
            frpIns = am.open(frpIni);
            File iniFile = new File(dataPath + File.separator + frpIni);
            fos = new FileOutputStream(iniFile);
            while((len = frpIns.read(buffer)) != -1){
                fos.write(buffer, 0, len);
            }
            fos.flush();
            frpIns.close();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void getPermission() {
        List<String> granteds = PermissionUtils.getPermissions();
        PermissionUtils.permission(permissions).callback(new PermissionUtils.FullCallback(){
            @Override
            public void onGranted(List<String> permissionsGranted) {
                for(String permissin : permissionsGranted){
                    ToastUtils.showLong(permissin + "granted");
                }
            }

            @Override
            public void onDenied(List<String> permissionsDeniedForever, List<String> permissionsDenied) {
                for(String permissin : permissionsDenied){
                    ToastUtils.showLong(permissin + "permissionsDenied");
                }
                for(String permissin : permissionsDeniedForever){
                    ToastUtils.showLong(permissin + "permissionsDeniedForever");
                }
            }
        }).request();
        Log.e("","");
    }


    ServerManager serverManager;
    private void startServer(final String ip, final int port) {
        if(serverManager != null){
            return;
        }

        new Thread(){
            @Override
            public void run() {
                super.run();
                serverManager = new ServerManager(ip, port);
                serverManager.startServer();
            }
        }.start();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0){ //安全写法，如果小于0，肯定会出错了
                    for (int i = 0; i < grantResults.length; i++) {

                        int grantResult = grantResults[i];
                        if (grantResult == PackageManager.PERMISSION_DENIED){ //这个是权限拒绝
                            String s = permissions[i];
                            Toast.makeText(this,s+"权限被拒绝了",Toast.LENGTH_SHORT).show();
                        }else{ //授权成功了
                            //do Something
                        }
                    }
                }
                break;
            default:
                break;
        }
    }

    private void execCmd(String cmd) throws IOException {
        //清空日志
        Runtime runtime = Runtime.getRuntime();
        Process process = runtime.exec(cmd);
        InputStream is = process.getInputStream();
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);
        String line = null;
        while (null != (line = br.readLine())) {
            Log.e("########", line);
            Message message=new Message();
            message.what=1;//标志是哪个线程传数据
            myHandler.sendMessage(message);//发送message信息
        }
        /*
        try {
          //  process.waitFor();
        } catch (InterruptedException e) {

            e.printStackTrace();
        }*/
    }

    Handler myHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if(msg.what == 1){

            }
        }
    };
}
