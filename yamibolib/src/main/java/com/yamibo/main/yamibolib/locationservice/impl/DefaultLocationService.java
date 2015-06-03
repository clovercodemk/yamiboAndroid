package com.yamibo.main.yamibolib.locationservice.impl;

import android.content.Context;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;

import com.baidu.location.LocationClientOption;
import com.yamibo.main.yamibolib.Utils.Log;
import com.yamibo.main.yamibolib.locationservice.LocationListener;
import com.yamibo.main.yamibolib.locationservice.LocationService;
import com.yamibo.main.yamibolib.locationservice.model.City;
import com.yamibo.main.yamibolib.locationservice.model.Location;
import com.yamibo.main.yamibolib.model.GPSCoordinate;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by wangxiaoyan on 15/5/25.
 * Clover: implemented on 01/06/25, use BDLocationManager class based on Baidu sample, member variables are added correspondingly
 */
public class DefaultLocationService implements LocationService {

    private Context mContext;


    /**
     * to be read by the textView for shown to mobile activity
     */
    public String debugMessage = null;

    //Baidu service
    // client and listener are in the BDLocationManager's member field
    private LocManager locManager = null;
    private Location locationResult = null;


    /**
     * DEBUG_CODE, change the boolean flag to enable/disable Log.i message started with "DEBUG_"
     */
    private static final boolean IS_DEBUG_ENABLED = true;

    private List<LocationListener> mListeners = new ArrayList<>();


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
        locManager = new BDLocationManager(mContext);
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
        if (locationResult != null)
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
    public String address() {
        //if (hasLocation())
          //  return (locationResult.getAddrStr());
        return null;
    }

    @Override
    //TODO
    public City city() {
        return null;
    }

    @Override
    /**
     * Clover:
     *
     * register listener, init option, start service, requestLocation
     */
    public boolean start() {
        if (locManager == null) {
            return false;
        }
        if (isLocationEnabled(mContext)) {
            addListener(new DefaultLocationListener());
            locManager.initLocation();
            locManager.start();
            isLocationDemand= locManager.requestLocation();

            debugLog("location service starts");
            debugShow("location service starts");
            return true;
        } else {
            return false;
        }
    }


    @Override
    /**
     * Clover:
     * unregister listener and stop client
     * in Baidu service sample, listener is not removed when client stops?
     */
    public void stop() {
        if (locManager == null)
            return;

        //reset flags
        resetFlag();
        for (LocationListener listener:mListeners)
            removeListener(listener);
        locManager.stop();

        debugLog("location service stops");
    }


    /**
     *
     *  刷新当前位置<br>
     * 如果当前系统定位开关未打开，会直接返回false<br>
     * asynchronous, return true if the demand has been sent
     */
    @Override
    public boolean refresh() {
        if (locManager == null)
            return false;
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
    public void newUpdateTime(int timeMS) {
        //locManager.setSpan(timeMS);
        locManager.initLocation();
    }

    /**
     * @param isNeedAddress to TEST: 热切换
     */
    public void newAddressAppearance(boolean isNeedAddress) {
        //locManager.setIsNeedAddress(isNeedAddress);
        locManager.initLocation();
    }

    /**
     * @param input LocationClientOption.LocationMode.Hight_Accuracy 高精度模式
     *              , Battery_Saving 低功耗模式
     *              , Device_Sensors 仅设备(Gps)模式
     *              热切换 in demo sample
     */
    public void newLocationMode(LocationClientOption.LocationMode input) {
        //locManager.setLocationMode(input);
        locManager.initLocation();
    }

    /**
     * @param input choose "gcj02","bd09ll","bd09"
     * @return 热切换 in demo
     */
    public void newCoordMode(String input) {

        //locManager.setCoordMode(input);
    }



    @Override
    /**
     * NEED to be changed: LocationListener is not a parameter for BD listener servive;
     * not used here
     * maybe overload with no parameter?
     */
    public void addListener(LocationListener listener) {
        if (listener != null && !mListeners.contains(listener)) {
            mListeners.add(listener);
        }
        //TODO add this listener to the manager
    }


    /**
     * NEED to be changed: LocationListener is not a parameter for BD listener servive;
     * not used here
     * maybe overload with no parameter?
     */
    @Override
    public void removeListener(LocationListener listener)
    {
        mListeners.remove(listener);
        //TODO remove this listener from the manager
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
     * Baidu Location Listener has just returned a location data, or
     * Android API detects a location change and has just returned the new location
     *
     */
    public void onReceiveLocation(Location locationResult) {

        if (locManager == null)
            return;

        isLocationReceived=true;
        this.locationResult = locationResult;
        // TODO
        // 初始化这里LocationServier所有的变量,包括locaion,city,等等等等
        for (LocationListener listener : mListeners) {
            listener.onLocationChanged(this);
        }

        debugLog("LocationService receive location from BDLocation");
        //debugShow(BDLocationManager.toStringOutput(locationResult));
    }
}
