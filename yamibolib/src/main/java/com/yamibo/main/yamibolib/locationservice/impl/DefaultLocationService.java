package com.yamibo.main.yamibolib.locationservice.impl;

import android.content.Context;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;

import com.yamibo.main.yamibolib.Utils.Log;
import com.yamibo.main.yamibolib.locationservice.LocationListener;
import com.yamibo.main.yamibolib.locationservice.LocationService;
import com.yamibo.main.yamibolib.locationservice.model.City;
import com.yamibo.main.yamibolib.locationservice.model.Location;
import com.yamibo.main.yamibolib.model.GPSCoordinate;

import java.util.ArrayList;
import java.util.List;

/**
 * 这个类与上层用户交谈，检查环境变量(system settings)，读取更新间隔等参数，管理切换定位服务，定义常量和计算函数。
 * Created by wangxiaoyan on 15/5/25.<br>
 * Clover: 将这个类实例化，对定位服务进行启动刷新,读取定位数据。<br>
 * 注意百度定位服务模式须在在主线程里使用。
 * <p/>
 * 基本使用方法 ：<br>
 * 在activity中，运行<br>
 * DefaultLocationService apiLocationService=new DefaultLocationService(getApplicationContext()),或单有参数的形式<br>
 * apiLocationService.start();<br>
 * apiLocationService.stop();<br>
 * <p/>
 *默认是单次更新请求。刷新监听请调用 refresh();<br>
 * <p/>
 * 可以将任何拥有LocationLisener接口的实例listener添加到队列:<br>
 * addListener(listener), removeListener(listener);
 * 帮助类BDLocationService和AndroidLocationService会建立一个相应的API定位监听器。
 * <p/>
 * 以下方法更改已注册的所有监听器的参数，并将作为下一次的监听器参数: <br>
 * resetServiceOption(update_interval,provider);<br>
 * 注解：百度定位模式下只能设置统一的监听参数。因此对AndroidAPI也作此简化处理。
 *
 */
public class DefaultLocationService implements LocationService {

    private Context mContext;

    /**
     * 监听器队列
     */
    private List<LocationListener> activeListeners = new ArrayList<>();

    /**
     * 用于实例化 百度 BDLocationClient 或 Android locationManager
     */
    private APILocationService apiLocationService = null;

    /**
     * 任意定位服务取得的上次程序定位的结果
     * TODO 可读取存储的信息
     */
    private Location lastKnownLocation = null;
    /**
     * 默认的serviceMode为百度定位（适用中国）或AndroidAPI定位（适用中国之外）
     */
   // private int serviceMode=BAIDU_MODE;
    private int serviceMode=ANDROID_API_MODE;
    /**
     * 是否允许程序根据定位结果自动选择定位服务
     */
    private boolean isAutoSwitchService =false;




    /**
     * 当更新时间小于1000ms时，为单次更新
     */
    private int updateInterval =-1;

    /**
     * 默认选择GPS and/or Network进行定位
     */
    private int providerChoice=PROVIDER_NETWORK;

 //No Use
//    private boolean isStarted=false;
//    private boolean isLocationDemand=false;


    private boolean isLocationReceived=false;



    /**
     * 自动更新启动时的默认更新间隔
     */
    public static final int DEFAULT_UPDATE_INTERVAL=10*60*1000;//default requestLocation time 10min


    public static final int BAIDU_MODE=0;
    public static final int ANDROID_API_MODE=1;
    /**
     * 同时用GPS和Network
     */
    public static final int PROVIDER_BEST=0;
    /**
     *   只用Network
     */
    public static final int PROVIDER_NETWORK=1;
    /**
     * 只用GPS
     */
    public static final int PROVIDER_GPS=2;


    /**
     * to be read by the textView for shown to mobile activity
     */
    public String debugMessage = null;
    /**
     * DEBUG_CODE, change the boolean flag to enable/disable Log.i message started with "DEBUG_"
     */
    private static final boolean IS_DEBUG_ENABLED = true;




    /**
     * Clover:
     * locationClient and Listener instantiated
     * link onReceived callback
     * listener not registered! service not started! use start();
     *
     * Creat manager for BAIDU location by default
     * @param context
     */
    public DefaultLocationService(Context context) {
        mContext = context;
        //TODO 根据 lastKnownLocation决定是否自动选择serviceMode
        switch (serviceMode) {
            case BAIDU_MODE:
                apiLocationService = new BDLocationService(mContext,updateInterval,providerChoice,this);
                debugLog("Baidu location mode selected.");
                break;
            case ANDROID_API_MODE:
                apiLocationService =new AndroidLocationService(mContext,updateInterval,providerChoice,this);
                debugLog("Android API location mode selected");
                break;
            default:
                debugLog("Unknown location mode selected!");
                return;
        }
    }
    public DefaultLocationService
            (Context context, int serviceMode,boolean isAutoSwitchService, int update_interval,int providerChoice) {
        mContext = context;
        this.isAutoSwitchService = isAutoSwitchService;
        this.updateInterval=update_interval;
        this.serviceMode=serviceMode;
        if(providerChoice==PROVIDER_BEST|| providerChoice==PROVIDER_NETWORK||providerChoice==PROVIDER_GPS)
            this.providerChoice=providerChoice;
        else{
            debugLog("Error input");
            return;
        }

        switch (serviceMode) {
            case BAIDU_MODE:
                apiLocationService = new BDLocationService(mContext,updateInterval,providerChoice,this);
                debugLog("Baidu location mode selected.");
                break;
            case ANDROID_API_MODE:
                apiLocationService =new AndroidLocationService(mContext,updateInterval,providerChoice,this);
                debugLog("Android API location mode selected");
                break;
            default:
                debugLog("Unknown location mode selected!");
                return;
        }
    }

    /**
     * 获取当前服务状态(交由具体API实例判断）
     * @return STATUS_LOCATED 表示当前定位已经完成。并可以持续获取可用的位置（定位服务可用）<br>
     *     <p/>
     *     STATUS_FAIL  表示当前状态为定位失败<br>
     *         <p/>
     *         STATUS_TRYING 表示当前定位服务正在尝试获取最新的位置：<br>
     *             当使用百度服务时，代表（初次或者再次刷新时）定位请求已经发送但尚未收到回应。<br>
     *             当使用Android API时，代表初次定位请求已经发送，但尚未收到回应。
     */
    @Override
    public int status() {
        return apiLocationService.status();
    }


    /**
     * 当有可用位置时返回true（仅包括最近一次取得的，和之前存储的）<br>
     * 这个位置不一定是最新的，请用requestLocation()来更新，
     * status()来确定当前实例有没有取得新位置。
     * @return
     */
    @Override
    public boolean hasLocation() {
        if(lastKnownLocation!=null)
            return true;
        else
            return false;
    }

    //TODO
    @Override
    public Location location() {
        return null;
    }


    /**
     * TODO
     * @return
     */
    @Override
    public GPSCoordinate realCoordinate() {
        return null;
    }

    /**
     * TODO
     */
    @Override
    public GPSCoordinate offsetCoordinate() {
        return null;
    }

    /**
     * TODO
     * @return
     */
    @Override
    public String address() {
        return null;
    }

    /**
     * TODO
     * @return
     */
    @Override
    public City city() {
        return null;
    }

    @Override
    /**
     *
     * 若具体API尚未开始进行定位工作（未调用过start()或者调用stop()之后），
     * 会创建一个新的监听器并开始定位。
     *
     */
    public boolean start() {
        if(apiLocationService ==null||!isLocationEnabled(mContext))
            return false;
        if(activeListeners.isEmpty()){
            LocationListener listener=new LocationListener() {
            @Override
            public void onLocationChanged(LocationService sender) {
                debugLog("A listener auto generated while service start() " +
                        "and the activeListeners arrays is empty");
                }
            };
            addListener(listener);
        }
        else {
            debugLog("Use existeing listeners");
        }

        return apiLocationService.start();
    }

    /**
     *
     * 删除所有监听器，停止定位功能<br>
     * 可多次调用
     */
    @Override
    public void stop() {
        apiLocationService.stop();
        for(LocationListener listener: activeListeners)
            removeListener(listener);
    }


    /**
     *  让所有已知监听器发送异步刷新当前位置的请求。可多次调用<br>
     * 如果当前系统定位开关未打开，会直接返回false<br>
     * 注意：百度的返回值由它的定位服务统一提供<br>
     *     AndroidAPI 至少一个listener获取位置时返回值为true
     */
    @Override
    public boolean refresh() {
        if(!isLocationEnabled(mContext))
            return false;
        resetFlag();
        return apiLocationService.refresh();
    }

    /**
     * 重置发送/接到 位置信息的flags
     */
    private void resetFlag() {
        isLocationReceived=false;
    }



    /**
     *
     * @param updateInterval 百度/Android API设置发起自动更新定位请求的间隔时间(ms)<br>
     *                        <1000 不会自动发送新请求。需要手动发送。
     * @param providerChoice PROVIDER_BEST 返回GPS和网络定位中最好的结果
     *              , PROVIDER_NETWORK 只使用网络和基站
     *              ,  PROVIDER_GPS 只用GPS模式<br>
     *让定位服务的所有已知监听器以新的参数连接并运行。
     */
    public void resetServiceOption(int updateInterval, int providerChoice){
        apiLocationService.resetServiceOption(updateInterval, providerChoice);
    }

    /**
     * @param listener 监听器使用DefaultListener class.<br>
     *
     * 注册监听器的同时与targetService绑定<br>
     *                 每个监听器至多只可以添加一次
     *     <p/>
     * 注：监听器将使用DefaultLocationService的参数。<br>
     *                 TODO test if Baidu client support hot add listener!
     */
    @Override
    public void addListener(LocationListener listener) {
        if(activeListeners.contains(listener)){
            debugLog("listener is already active and known by the service!");
            return;
        }
        activeListeners.add(listener);
        apiLocationService.addListener(listener);
    }

    /**
     * 删除监听器
     * @param listener
     */
    @Override
    public void removeListener(LocationListener listener)
    {
        apiLocationService.removeListener(listener);
        activeListeners.remove(listener);
    }


    @Override
    //TODO
    public void selectCoordinate(int type, GPSCoordinate coord) {
        return;
    }


    /**
     * 判断系统的定位服务设置是否开启
     * @param context
     * @return
     */
    public static boolean isLocationEnabled(Context context) {
        if (context == null)
            return false;

        int locationMode = 0;
        String locationProviders;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            try {
                locationMode = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE);

            } catch (Settings.SettingNotFoundException e) {
                e.printStackTrace();
            }
            return locationMode != Settings.Secure.LOCATION_MODE_OFF;

        } else {
            locationProviders = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
            return !TextUtils.isEmpty(locationProviders);
        }


    }


    /**
     * this message can be shown in the debut_text field on mobile by click the debug_button
     *
     * @param Message
     */
    private void debugShow(String Message) {
        if (IS_DEBUG_ENABLED)
            debugMessage = Message;
    }

    static void debugLog(String Message) {
        if (IS_DEBUG_ENABLED)
            Log.i("DefaultLocationSerivce", "DEBUG_" + Message);
    }

    /**
     * 当任何一个监听器收到位置信息时，运行监听器队列中的每一个监听器。<p/>
     * 实现方法注解：AndroidAPI情形：当一个监听器设置为处于单次更新模式时，用unregister来停止它的更新功能，仍将其保留在监听器列表中。
     * 下次refresh时它会被重新注册并尝试获取位置信息。
     *
     */
    void onReceiveLocation(Location LocationResult) {
//TODO delegate to each apiLocationService realization? need to require autoSwitchMode
        /*if (apiLocationService == null)
            return;
        */
        isLocationReceived=true;

        this.lastKnownLocation = LocationResult;
        debugLog("LocationService updated location from one listener");

        for (LocationListener listener : activeListeners) {
            listener.onLocationChanged(this);
        }
        debugLog("all activeListeners perform their own actions");

        if(isAutoSwitchService) {
            if (lastKnownLocation.getIsInCN() == Location.IN_CN && serviceMode != BAIDU_MODE)
                switchServiceMode(BAIDU_MODE);
            if (lastKnownLocation.getIsInCN() == Location.NOT_IN_CN && serviceMode==BAIDU_MODE)
                switchServiceMode(ANDROID_API_MODE);
        }
    }

    /**
     *
     * @param newServiceMode
     * 在百度/AndroidAPI服务间切换。切换后所有flags和已运行的监视器将消失。保留最后一次获取的位置。
     */
    void switchServiceMode(int newServiceMode) {
        if(newServiceMode==serviceMode){
            debugLog("Same location service, no need to switch");
            return;
        }
        debugLog("restart service with the new service mode");
        serviceMode=newServiceMode;
        restart();
    }

    private void restart() {
        stop();
        resetFlag();
        start();
    }
}

