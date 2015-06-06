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
 * Created by wangxiaoyan on 15/5/25.<br>
 * Clover: 将这个类实例化，对定位服务进行启动刷新,读取定位数据。<br>
 * 注意百度定位服务模式须在在主线程里使用。
 */
public class DefaultLocationService implements LocationService {

    private Context mContext;
    /**
     * FUTURE 可储存上次程序定位的结果
     */
    private Location lastKnownLocation = null;
    /**
     * 默认的serviceMode为百度定位
     */
    private int serviceMode=BAIDU_MODE;
    //private int serviceMode=ANDROID_API_MODE;
    /**
     * 是否允许程序根据定位结果自动选择定位服务
     */
    private boolean autoSwitchService=true;



    public static final int BAIDU_MODE=0;
    public static final int ANDROID_API_MODE=1;
    public static final int PROVIDER_BEST=0;
    public static final int PROVIDER_NETWORK=1;
    public static final int PROVIDER_GPS=2;


    /**
     * to be read by the textView for shown to mobile activity
     */
    public String debugMessage = null;

    private LocManager locManager = null;


    /**
     * DEBUG_CODE, change the boolean flag to enable/disable Log.i message started with "DEBUG_"
     */
    private static final boolean IS_DEBUG_ENABLED = true;

    private List<LocationListener> listeners = new ArrayList<>();


    //Use the following two flags to track the working status of location application
    private boolean isLocationDemand=false;
    private boolean isLocationReceived=false;


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
        //TODO 根据 lastKnownLocation决定是否切换serviceMode
        switch (serviceMode) {
            case BAIDU_MODE:
                locManager = new BDLocationManager(mContext);
                debugLog("Baidu location mode selected.");
                break;
            case ANDROID_API_MODE:
                locManager=new AndroidLocationManager(mContext);
                debugLog("Android API location mode selected");
                break;
            default:
                debugLog("Unknown location mode selected!");
                return;
        }
    }

    /**
     * 选择使用的定位模式。选择后需重启定位服务。
     * @param choice BAIDU_MODE, ANDROID_API_MODE
     */
    public void selectServiceMode(int choice, boolean autoSwitchService){
        this.autoSwitchService=autoSwitchService;
        switch(choice) {
            case BAIDU_MODE:
                serviceMode = BAIDU_MODE;
                break;
            case ANDROID_API_MODE:
                serviceMode = ANDROID_API_MODE;
                debugLog("Android API location not implemented yet");
                break;
            default:
                debugLog("Unknown location mode selected!");
                return;
        }

    }

    /**
     * 获取当前服务状态
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
        int mStatus;
        if (isLocationReceived)
            mStatus = LocationService.STATUS_LOCATED;
        else {
            if (isLocationDemand)
                mStatus = LocationService.STATUS_TRYING;
            else
                mStatus = LocationService.STATUS_FAIL;
        }
        return mStatus;
    }

    @Override
    public boolean hasLocation() {
        if (lastKnownLocation != null)
            return true;
        return false;
    }

    @Override
    //TODO
    public Location location() {
        return null;
    }

    @Override
    //TODO
    public GPSCoordinate realCoordinate() {
        return null;
    }

    @Override
    //TODO
    public GPSCoordinate offsetCoordinate() {
        return null;
    }

    @Override
    //TODO
    public String address() {
        //if (hasLocation())
          //  return (lastKnownLocation.getAddrStr());
        return null;
    }

    @Override
    //TODO
    public City city() {
        return null;
    }

    @Override
    /**
     *
     * 注册监听器（androidAPI即开始正常工作）<br>
     * 更新client设置（仅百度）<br>
     * client启动（仅百度）<br>
     * 发出定位请求（仅百度）<br>
     *
     */
    public boolean start() {
        if (locManager == null) {
            return false;
        }
        if (isLocationEnabled(mContext)) {
            DefaultLocationListener listener=new DefaultLocationListener();
            addListener(listener);
            locManager.applyOption();
            locManager.start();
            isLocationDemand= locManager.requestLocation();

            debugLog("location service starts");
            return true;
        } else {
            debugLog("system location setting not enabled");
            return false;
        }
    }


    @Override
    /**
     * Clover:
     * 注销监听器 (baidu sample 似乎略过这个步骤）
     * 停止client (仅百度）
     *
     */
    public void stop() {
        if (locManager == null)
            return;

        //reset flags
        resetFlag();
        for (LocationListener listener: listeners)
            removeListener(listener);
        locManager.stop();

        debugLog("location service stops");
    }


    /**
     *
     *  刷新当前位置<br>
     * 如果当前系统定位开关未打开，会直接返回false<br>
     * 单次更新模式下要重新注册所有AndroidAPI listener
     * asynchronous, return true if the demand has been sent<p/>
     * 仅实现百度部分
     */
    @Override
    public boolean refresh() {
        if(!isLocationEnabled(mContext))
            return false;
        if (locManager == null)
            return false;

        //TODO check type is DefaultLocationListener or not
        for (LocationListener listener:listeners)
            if(!((DefaultLocationListener)listener).isAutoRequestUpdate)
                locManager.addListener(listener);
        return locManager.requestLocation();
    }

    /**
     * reset location demand/receive flags to false
     */
    private void resetFlag() {
        isLocationDemand=false;
        isLocationReceived=false;
    }



    /**
     * @param timeMS 设置发起定位请求的间隔时间为>=1000 (ms) 时为循环更新
     *               default value -1 means no automatic update.
     *               to TEST: 热切换
     */

    /**
     *
     * @param updateInterval 百度/Android API设置发起自动更新定位请求的间隔时间(ms)<br>
     *                        <1000 不会自动发送新请求。需要手动发送。
     * @param providerChoice PROVIDER_BEST 返回GPS和网络定位中最好的结果
     *              , PROVIDER_NETWORK 只使用网络和基站
     *              ,  PROVIDER_GPS 只用GPS模式<br>
     * should set for each listener?!!<p/>
     * 从程序上看，似乎百度的option设置与client捆绑。
     *                       androidAPI的option设置与每次监听器的使用捆绑（监听器可以重复利用）
     */
    public void setServiceOption(int updateInterval, int providerChoice){
        locManager.setUpdateInterval(updateInterval);
        locManager.setProvider(providerChoice);
        locManager.applyOption();//ONLY needed by Baidu
    }

    /**
     * 这里的监听器使用DefaultListener class.
     * 注册监听器的同时与targetService绑定
     * androidAPI到此完全开始运作。
     * @param listener
     */
    @Override
    public void addListener(LocationListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            locManager.addListener(listener);
            listeners.add(listener);
            ((DefaultLocationListener)listener).targetService=this;
        }
    }

    /**
     * 删除监听器，androidAPI到此结束功能。
     * @param listener
     */
    @Override
    public void removeListener(LocationListener listener)
    {
        if (listener != null && listeners.contains(listener)) {
            locManager.removeListener(listener);
            listeners.remove(listener);
        }
    }


    @Override
    //TODO
    public void selectCoordinate(int type, GPSCoordinate coord) {

    }


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
     *
     *
     * 运行监听器队列中的每一个监听器。
     * 当系统处于单次更新模式的选项时，停止自动更新位置。
     *
     */
    public void onReceiveLocation(Location locationResult) {

        if (locManager == null)
            return;

        isLocationReceived=true;
        this.lastKnownLocation = locationResult;
        debugLog("LocationService receive location from one listener");
        // TODO
        // 初始化这里LocationService所有的变量,包括location,city,等等等等
        for (LocationListener listener : listeners) {
            listener.onLocationChanged(this);
        }
        debugLog("all listeners perform their own actions");

        // 如果是AndroidAPI单次更新模式，每个Listener注销自己？

        switchServiceMode();
    }

    /**
     * 根据country值判断是否要切换serviceMode为baidu或Android API
     * TODO
     */
    private void switchServiceMode() {
    }
}
