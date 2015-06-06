package com.yamibo.main.yamibolib.locationservice.impl;

import com.baidu.location.BDLocation;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.yamibo.main.yamibolib.locationservice.LocationListener;
import com.yamibo.main.yamibolib.locationservice.model.Location;

import android.content.Context;

import static com.yamibo.main.yamibolib.locationservice.impl.DefaultLocationService.debugLog;

/**
 * Clover on 2015-06-01
 * helper class to control BDLocationClient.<br>
 * modified from default BD location service sample, which is an Application classuse getApplication() in activity to control this, and register its name in manifest.xml
 */
class BDLocationManager implements LocManager{
    DefaultLocationService targetService=null;

    private LocationClient mLocationClient;
    private DefaultLocationListener mDefaultListener;

    private LocationClientOption.LocationMode locationMode = LocationClientOption.LocationMode.Hight_Accuracy;
    private int span=-1;//default requestLocation not auto update

    /**
     * 返回街道名称
     */
    private static final boolean IS_NEED_ADDRESS =true;
    /**
     * 统一设定为三种模式 "gcj02","bd09ll","bd09"
     * 这里设定为百度经纬度
     */
    private static final String COORD_MODE ="bd09ll";

    /**
     * toggle debug function output on
     */
    private static final boolean IS_DEBUG_ENABLED=true;



    /**
     *
     * @param providerChoice 使用GPS/Network定位
     */
    public void setProvider(int providerChoice){
        switch(providerChoice){
            case DefaultLocationService.PROVIDER_BEST:
                locationMode=LocationClientOption.LocationMode.Hight_Accuracy;
                break;
            case DefaultLocationService.PROVIDER_NETWORK:
                locationMode=LocationClientOption.LocationMode.Battery_Saving;
                break;
            case DefaultLocationService.PROVIDER_GPS:
                locationMode=LocationClientOption.LocationMode.Device_Sensors;
                break;
            default:
                debugLog("Unknown provider mode!");
                return;
        }
    }


    /**
     *
     * @param input
     * 设置发起定位请求的间隔时间为>=1000 (ms) 时为循环更新
     * default value -1 means no automatic update.
     */
    public void setUpdateInterval(int input){
        span =input;
    }

    /**
     * instantiate client
     * @param context
     */
    public BDLocationManager(Context context) {
        mLocationClient = new LocationClient(context);
    }


    /**
     *
     * @param location
     * @return its String format for Output
     */
    public static String bdLocationToString(BDLocation location){
        StringBuffer sb = new StringBuffer(256);
        sb.append("time : ");
        sb.append(location.getTime());
        sb.append("\nerror code : ");
        sb.append(location.getLocType());
        sb.append("\nlatitude : ");
        sb.append(location.getLatitude());
        sb.append("\nlontitude : ");
        sb.append(location.getLongitude());
        sb.append("\nradius : ");
        sb.append(location.getRadius());
        if (location.getLocType() == BDLocation.TypeGpsLocation) {
            sb.append("\nspeed : ");
            sb.append(location.getSpeed());
            sb.append("\nsatellite : ");
            sb.append(location.getSatelliteNumber());
            sb.append("\ndirection : ");
            sb.append("\naddr : ");
            sb.append(location.getAddrStr());
            sb.append(location.getDirection());
        } else if (location.getLocType() == BDLocation.TypeNetWorkLocation) {
            sb.append("\naddr : ");
            sb.append(location.getAddrStr());
            //运营商信息
            sb.append("\noperationers : ");
            sb.append(location.getOperators());
        }
        return sb.toString();
    }

    /**Baidu sample, set option for the client?
     * TODO
     *should set for each listener???!
     */
    public void applyOption() {
        LocationClientOption option = new LocationClientOption();
        option.setLocationMode(locationMode);//设置定位模式
        option.setCoorType(COORD_MODE);//返回的定位结果是百度经纬度
        option.setScanSpan(span);
        option.setIsNeedAddress(IS_NEED_ADDRESS);

        mLocationClient.setLocOption(option);
        debugLog("initiate Location done");
    }

    /**
     * BDclient start(). <br>
     * BDclient's requestLocation() not included
     */
    public void start(){
        mLocationClient.start();
    }

    /**
     * @return true if location demand was successfully sent <br>
     *     <br>
     * invoke BDclient requestLocation(),whose int code returned by requestLocation()<br>
     * 0：正常发起了定位。
     *1：服务没有启动。
     *2：没有监听函数。
     *6：请求间隔过短。 前后两次请求定位时间间隔不能小于1000ms。<br>
     *
     */
    public boolean requestLocation(){
        debugLog("Request BDLocation update");

        int code=mLocationClient.requestLocation();
        switch (code)
        {
            case 0:
                return true;
            case 6:
                return false;
            default:
                return false;

        }


    }

    /**
     * BD add listener
     */
    public void addListener(LocationListener listener){
        //TODO check type is DefaultLocationListener, if not, need a translation
        mLocationClient.registerLocationListener((DefaultLocationListener)listener);
        debugLog("BDListener added" + listener.toString());
    }

    public void removeListener(LocationListener listener) {
        //TODO check type is DefaultLocationListener, if not, need a translation
        mLocationClient.unRegisterLocationListener((DefaultLocationListener)listener);
        debugLog("BDListener removed: " + listener.toString());
    }



    /**
     * BD client stop
     */
    public void stop() {
        mLocationClient.stop();
    }

    /**
     * 将百度地址翻译为model里的Location
     * @param source
     * @return
     */
    public static Location toLocation(BDLocation source){
        return null;
    }
}
