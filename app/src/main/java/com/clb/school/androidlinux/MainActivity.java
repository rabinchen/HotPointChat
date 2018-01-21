package com.clb.school.androidlinux;

import android.Manifest;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.annotation.RequiresApi;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.clb.school.androidlinux.adapter.ContentAdapter;
import com.clb.school.androidlinux.adapter.MyAdapter;
import com.clb.school.androidlinux.bean.Content;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity
        implements View.OnClickListener{

    private Toolbar toolbar;
    private RecyclerView wifiList;
    private RecyclerView contentListView;
    private TextView wifiState,apState;
    private Button closeAp,createAp,closeWifi,openWifi;
    private MyAdapter adapter;
    private ContentAdapter contentAdapter;
    private List<ScanResult> scanResults;
    private List<Content> contentList = new ArrayList<>();

    private WifiManager wifiManager;
    private WifiConfiguration config;
    private int wcgID;

    private ConnectThread connectThread;
    private ListenThread listenThread;

    Socket sendMSocket;

    /**
     * 热点名称
     */
    private static final String WIFI_HOTSPOT_SSID = "TEST";
    /**
     * 端口号
     */
    private static final int PORT = 8888;

    private boolean COUNT = false;
    private static final int WIFICIPHER_NOPASS = 1;
    private static final int WIFICIPHER_WEP = 2;
    private static final int WIFICIPHER_WPA = 3;

    public static final int DEVICE_CONNECTING = 1;//有设备正在连接热点
    public static final int DEVICE_CONNECTED = 2;//有设备连上热点
    public static final int SEND_MSG_SUCCSEE = 3;//发送消息成功
    public static final int SEND_MSG_ERROR = 4;//发送消息失败
    public static final int GET_MSG = 6;//获取新消息i
    public static final int RESTART = 7;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        CheckPermission();
        initBroadcastReceiver();
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
    }

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case DEVICE_CONNECTING:
                    toolbar.setTitle("设备连接中...");
                    connectThread = new ConnectThread(listenThread.getSocket(),handler,null);
                    connectThread.start();
                    break;
                case DEVICE_CONNECTED:
                    toolbar.setTitle("设备连接成功");
                    apState.setText("设备连接成功");
                    break;
                case SEND_MSG_SUCCSEE:
                    toolbar.setTitle("发送消息成功");
                    String sendData = msg.getData().getString("SMG").toString();
                    contentList.add(new Content(sendData,2));
                    contentAdapter.notifyDataSetChanged();
                    break;
                case SEND_MSG_ERROR:
                    toolbar.setTitle("发送消息失败");
                    break;
                case GET_MSG:
                    toolbar.setTitle("收到消息");
                    String getData = msg.getData().getString("GSG");
                    contentList.add(new Content(getData,1));
                    contentAdapter.notifyDataSetChanged();
                    break;
                case RESTART:
                    final Intent intent = getPackageManager().getLaunchIntentForPackage(getPackageName());
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    break;
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
    }

    private void CheckPermission(){
        boolean isAllowWriteSetting = true;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            isAllowWriteSetting = Settings.System.canWrite(this);
            if(isAllowWriteSetting){
                Toast.makeText(this, "允许写入设置权限", Toast.LENGTH_SHORT).show();
            }else{
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                    intent.setData(Uri.parse("package:" + this.getPackageName()));
                    startActivityForResult(intent, 101);
                } else {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_SETTINGS},2);
                }
            }
        }
        if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},1);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 101 && Settings.System.canWrite(this)){
            Log.d("TAG", "CODE_WRITE_SETTINGS_PERMISSION success");

        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] granteResults){
        Log.i("MainActivity","permissions:"+permissions.length);
        switch(requestCode){
            case 1:
                if(granteResults.length > 0 && granteResults[0] == PackageManager.PERMISSION_GRANTED){
                    return;
                }else{
                    Toast.makeText(this, "请允许授权使用位置权限", Toast.LENGTH_SHORT).show();
                }
                break;
            case 2:
                if(granteResults.length > 0 && granteResults[0] == PackageManager.PERMISSION_GRANTED){
                    return;
                }else{
                    Toast.makeText(this, "请允许授权使用写入设置权限", Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                break;
        }
    }

    private void initView(){
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        final FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        View view = navigationView.getHeaderView(0);
        closeAp = (Button)view.findViewById(R.id.close_ap);
        closeWifi = (Button)view.findViewById(R.id.close_wifi);
        createAp = (Button)view.findViewById(R.id.create_ap);
        openWifi = (Button)view.findViewById(R.id.search_wifi);
        openWifi.setOnClickListener(this);
        closeAp.setOnClickListener(this);
        closeWifi.setOnClickListener(this);
        createAp.setOnClickListener(this);
        wifiState = (TextView)view.findViewById(R.id.wifi_state);
        apState = (TextView)view.findViewById(R.id.ap_state);
        scanResults = new ArrayList<>();
        contentAdapter = new ContentAdapter(contentList);
        contentListView = (RecyclerView)findViewById(R.id.content_list);
        contentListView.setLayoutManager(new LinearLayoutManager(this));
        contentListView.setAdapter(contentAdapter);
        adapter = new MyAdapter(scanResults);
        adapter.setListener(new MyAdapter.ItemClickListener() {
            @Override
            public void onItemClick(int position) {
                if(scanResults.size() > 0){
                    final ScanResult scanResult = scanResults.get(position);
                    String capabilities = scanResult.capabilities;
                    int type = WIFICIPHER_WPA;
                    if (!TextUtils.isEmpty(capabilities)) {
                        if (capabilities.contains("WPA") || capabilities.contains("wpa")) {
                            type = WIFICIPHER_WPA;
                        } else if (capabilities.contains("WEP") || capabilities.contains("wep")) {
                            type = WIFICIPHER_WEP;
                        } else {
                            type = WIFICIPHER_NOPASS;
                        }
                    }
                    config = isExsits(scanResult.SSID);
                    if (config == null) {
                        if (type != WIFICIPHER_NOPASS) {//需要密码
                            final EditText editText = new EditText(MainActivity.this);
                            final int finalType = type;
                            new AlertDialog.Builder(MainActivity.this).setTitle("请输入Wifi密码").setIcon(
                                    android.R.drawable.ic_dialog_info).setView(
                                    editText).setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    config = createWifiInfo(scanResult.SSID, editText.getText().toString(), finalType);
                                    connect(config);
                                }
                            }).setNegativeButton("取消", null).show();
                            return;
                        } else {
                            config = createWifiInfo(scanResult.SSID, "", type);
                            connect(config);
                        }
                    } else {
                        connect(config);
                    }
                }
            }
        });
        wifiList = (RecyclerView)view.findViewById(R.id.wifi_list);
        wifiList.setLayoutManager(new LinearLayoutManager(this));
        wifiList.setAdapter(adapter);

        final EditText enterEdit = new EditText(this);
        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //解决父view和子view持续关联的bug
                if(enterEdit.getParent() != null){
                    ((ViewGroup)enterEdit.getParent()).removeView(enterEdit);
                }
                if(enterEdit.getText().toString().length() > 0){
                    enterEdit.setText("");
                }
                alertDialog.setTitle("请输入想要发送的内容!").setIcon(android.R.drawable.ic_popup_reminder).setView(enterEdit)
                        .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                if(listenThread!= null){
                                    if(listenThread.getSocket() != null){
                                        connectThread = new ConnectThread(listenThread.getSocket(),handler,enterEdit.getText().toString());
                                        connectThread.start();
                                    }
                                }else{
                                    connectThread = new ConnectThread(sendMSocket,handler,enterEdit.getText().toString());
                                    connectThread.start();
                                }

                            }
                        }).setNegativeButton("取消",null).show();
            }
        });
    }

    /**
     * 判断当前wifi是否有保存
     *
     * @param SSID
     * @return
     */
    private WifiConfiguration isExsits(String SSID) {
        List<WifiConfiguration> existingConfigs = wifiManager.getConfiguredNetworks();
        for (WifiConfiguration existingConfig : existingConfigs) {
            if (existingConfig.SSID.equals("\"" + SSID + "\"")) {
                return existingConfig;
            }
        }
        return null;
    }

    private void connect(WifiConfiguration config) {
        wcgID = wifiManager.addNetwork(config);
        wifiManager.enableNetwork(wcgID, true);
    }

    private void initBroadcastReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        intentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
//        intentFilter.addAction(WifiManager.RSSI_CHANGED_ACTION);

        registerReceiver(receiver, intentFilter);
    }

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                Log.w("BBB", "SCAN_RESULTS_AVAILABLE_ACTION");
                // wifi已成功扫描到可用wifi。
                scanResults = wifiManager.getScanResults();
                Log.w("BBB", "SCAN_RESULTS_AVAILABLE_ACTION"+scanResults.size());
                adapter.setScanResults(scanResults);
                adapter.notifyDataSetChanged();
            } else if (action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
                Log.w("BBB", "WifiManager.WIFI_STATE_CHANGED_ACTION");
                int wifiState = intent.getIntExtra(
                        WifiManager.EXTRA_WIFI_STATE, 0);
                switch (wifiState) {
                    case WifiManager.WIFI_STATE_ENABLED:
                        //获取到wifi开启的广播时，开始扫描
                        wifiManager.startScan();
                        break;
                    case WifiManager.WIFI_STATE_DISABLED:
                        scanResults.clear();
                        adapter.setScanResults(scanResults);
                        adapter.notifyDataSetChanged();
                        break;
                }
            } else if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                Log.w("BBB", "WifiManager.NETWORK_STATE_CHANGED_ACTION");
                NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                if (info.getState().equals(NetworkInfo.State.DISCONNECTED)) {
                    wifiState.setText("wifi断开连接");
                    toolbar.setTitle("wifi断开连接");
                } else if (info.getState().equals(NetworkInfo.State.CONNECTED)) {
                    WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                    final WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                    wifiState.setText("已连接到网络:" + wifiInfo.getSSID());
                    toolbar.setTitle("已连接到网络:" + wifiInfo.getSSID());
                    if (wifiInfo.getSSID().equals("\""+WIFI_HOTSPOT_SSID+"\"")) {
                        //如果当前连接到的wifi是热点,则开启连接线程
                        Toast.makeText(context, "当前连接到的wifi热点:"+wifiInfo.getSSID(), Toast.LENGTH_SHORT).show();
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    ArrayList<String> connectedIP = getConnectedIP();
                                    //当前链接的wifi的ip是使用.格式化后的
                                    for (String ip : connectedIP) {
                                        if (ip.contains(".")) {
                                            sendMSocket = new Socket(ip, PORT);
                                            connectThread = new ConnectThread(sendMSocket, handler,"我来自客户端");
                                            connectThread.start();
                                        }
                                    }

                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }).start();
                    }
                } else {
                    NetworkInfo.DetailedState state = info.getDetailedState();
                    if (state == state.CONNECTING) {
                        wifiState.setText("连接中...");
                        toolbar.setTitle("wifi连接中...");
                    } else if (state == state.AUTHENTICATING) {
                        wifiState.setText("正在验证身份信息...");
                        toolbar.setTitle("正在验证身份信息..");
                    } else if (state == state.OBTAINING_IPADDR) {
                        wifiState.setText("正在获取IP地址...");
                        toolbar.setTitle("正在获取IP地址...");
                    } else if (state == state.FAILED) {
                        wifiState.setText("wifi连接失败");
                        toolbar.setTitle("wifi连接失败");
                    }
                }

            }
           /* else if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                if (intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false)) {
                    text_state.setText("连接已断开");
                    wifiManager.removeNetwork(wcgID);
                } else {
                    WifiManager wifiManager = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
                    WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                    text_state.setText("已连接到网络:" + wifiInfo.getSSID());
                }
            }*/
        }
    };

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            if(contentList.size() > 0){
                contentList.clear();
                contentAdapter.notifyDataSetChanged();
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onClick(View view){
        switch (view.getId()){
            case R.id.close_ap:
                closeWifiHotspot();
                break;
            case R.id.close_wifi:
                CloseWifi();
                break;
            case R.id.create_ap:
                createWifiHotspot();
                break;
            case R.id.search_wifi:
                search();
                break;
        }
    }

    /**
     * 创建Wifi热点
     */
    private void createWifiHotspot() {
        if (wifiManager.isWifiEnabled()) {
            //如果wifi处于打开状态，则关闭wifi,
            wifiManager.setWifiEnabled(false);
        }
        WifiConfiguration config = new WifiConfiguration();
        config.SSID = WIFI_HOTSPOT_SSID;
        config.preSharedKey = "123456789";
        config.hiddenSSID = true;
        config.allowedAuthAlgorithms
                .set(WifiConfiguration.AuthAlgorithm.OPEN);//开放系统认证
        config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
        config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
        config.allowedPairwiseCiphers
                .set(WifiConfiguration.PairwiseCipher.TKIP);
        config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
        config.allowedPairwiseCiphers
                .set(WifiConfiguration.PairwiseCipher.CCMP);
        config.status = WifiConfiguration.Status.ENABLED;
        //通过反射调用设置热点
        try {
            Method method = wifiManager.getClass().getMethod(
                    "setWifiApEnabled", WifiConfiguration.class, Boolean.TYPE);
            boolean enable = (Boolean) method.invoke(wifiManager, config, true);
            if (enable) {
                apState.setText("热点已开启 SSID:" + WIFI_HOTSPOT_SSID + " password:123456789");
                toolbar.setTitle("热点开启");
                listenThread = new ListenThread(PORT,handler);
                listenThread.start();
            } else {
                apState.setText("创建热点失败");
            }
        } catch (Exception e) {
            e.printStackTrace();
            apState.setText("创建热点失败");
        }
    }

    /**
     * 关闭WiFi热点
     */
    public void closeWifiHotspot() {
        try {
            //通过反射获取方法
            Method method = wifiManager.getClass().getMethod("getWifiApConfiguration");
            method.setAccessible(true);
            WifiConfiguration config = (WifiConfiguration) method.invoke(wifiManager);
            Method method2 = wifiManager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
            method2.invoke(wifiManager, config, false);
            if(listenThread != null && listenThread.getServerSocket() != null){
                listenThread.getServerSocket().close();
            }
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }catch (IOException e){
            e.printStackTrace();
        }
        apState.setText("热点已关闭");
        toolbar.setTitle("热点关闭");
        wifiState.setText("wifi已关闭");
    }

    private void search() {
        if (!wifiManager.isWifiEnabled()) {
            //开启wifi
            wifiManager.setWifiEnabled(true);
        }
        wifiManager.startScan();
    }

    private void CloseWifi(){
        if(wifiManager.isWifiEnabled()){
            wifiManager.setWifiEnabled(false);
        }
    }


    /**
     * 获取连接到热点上的手机ip
     *
     * @return
     */
    private ArrayList<String> getConnectedIP() {
        ArrayList<String> connectedIP = new ArrayList<String>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(
                    "/proc/net/arp"));
            String line;
            while ((line = br.readLine()) != null) {
                String[] splitted = line.split(" +");
                if (splitted != null && splitted.length >= 4) {
                    String ip = splitted[0];
                    connectedIP.add(ip);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return connectedIP;
    }

    public WifiConfiguration createWifiInfo(String SSID, String password,
                                            int type) {
        Log.w("AAA", "SSID = " + SSID + "password " + password + "type ="
                + type);
        WifiConfiguration config = new WifiConfiguration();
        config.allowedAuthAlgorithms.clear();
        config.allowedGroupCiphers.clear();
        config.allowedKeyManagement.clear();
        config.allowedPairwiseCiphers.clear();
        config.allowedProtocols.clear();
        config.SSID = "\"" + SSID + "\"";
        if (type == WIFICIPHER_NOPASS) {
            config.wepKeys[0] = "\"" + "\"";
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            config.wepTxKeyIndex = 0;
        } else if (type == WIFICIPHER_WEP) {
            config.preSharedKey = "\"" + password + "\"";
            config.hiddenSSID = true;
            config.allowedAuthAlgorithms
                    .set(WifiConfiguration.AuthAlgorithm.SHARED);
            config.allowedGroupCiphers
                    .set(WifiConfiguration.GroupCipher.CCMP);
            config.allowedGroupCiphers
                    .set(WifiConfiguration.GroupCipher.TKIP);
            config.allowedGroupCiphers
                    .set(WifiConfiguration.GroupCipher.WEP40);
            config.allowedGroupCiphers
                    .set(WifiConfiguration.GroupCipher.WEP104);
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            config.wepTxKeyIndex = 0;
        } else if (type == WIFICIPHER_WPA) {
            config.preSharedKey = "\"" + password + "\"";
            config.hiddenSSID = true;
            config.allowedAuthAlgorithms
                    .set(WifiConfiguration.AuthAlgorithm.OPEN);
            config.allowedGroupCiphers
                    .set(WifiConfiguration.GroupCipher.TKIP);
            config.allowedKeyManagement
                    .set(WifiConfiguration.KeyMgmt.WPA_PSK);
            config.allowedPairwiseCiphers
                    .set(WifiConfiguration.PairwiseCipher.TKIP);
            // config.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
            config.allowedGroupCiphers
                    .set(WifiConfiguration.GroupCipher.CCMP);
            config.allowedPairwiseCiphers
                    .set(WifiConfiguration.PairwiseCipher.CCMP);
            config.status = WifiConfiguration.Status.ENABLED;
        } else {
            return null;
        }
        return config;
    }

//    private String getLocalIp(){
//        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
//        int ip = wifiInfo.getIpAddress();
//        String localIp = formatIp(ip);
//        Log.i("ip",localIp);
//        return localIp;
//    }

//    private String formatIp(int ip){
//        return (ip & 0xFF ) + "." +
//                ((ip >> 8 ) & 0xFF) + "." +
//                ((ip >> 16 ) & 0xFF) + "." +
//                ( ip >> 24 & 0xFF) ;
//    }

}
