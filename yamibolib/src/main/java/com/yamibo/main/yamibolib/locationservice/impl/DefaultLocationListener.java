package com.yamibo.main.yamibolib.locationservice.impl;

import android.os.Bundle;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.yamibo.main.yamibolib.locationservice.LocationService;
import com.yamibo.main.yamibolib.locationservice.model.Location;

import static com.yamibo.main.yamibolib.locationservice.impl.BDLocationManager.bdLocationToString;
import static com.yamibo.main.yamibolib.locationservice.impl.DefaultLocationService.debugLog;

/**
 *
 * 将此类实例化，作为监听器提供给DefaultLocationService对象。<br>
 *     <p/>
 * 监听器队列里的任何一个监听器在收到位置信息时，会通知DefaultLocationService targetService的onReceiveLocation(location)方法更新位置信息。<br>
 * 然后targetService会作为sender执行队列中的每一个listener的onLocationChanged(LocationService sender)方法。
 * <p/>
 * 用户可以继承这个类以后自定义onLocationChanged(LocationService sender)方法
 */
public class DefaultLocationListener implements BDLocationListener, android.location.LocationListener, com.yamibo.main.yamibolib.locationservice.LocationListener{
    DefaultLocationService targetService =null;
    /**
     * 仅ANDROID API 使用。用于进行单次更新操作。
     */
    public boolean isAutoRequestUpdate =false;

    /**
     * sender的位置更新后会调用此方法。用户可以自定义。
     * @param sender
     */
    @Override
    public void onLocationChanged(LocationService sender) {

    }


    @Override
    /**
     * update locationResult,
     * call targetService.onReceiveBDLocation(location)
     */
    public void onReceiveLocation(BDLocation bdLocation) {


        debugLog("BDlocation received" + bdLocationToString(bdLocation));

        Location locationResult=BDLocationManager.toLocation(bdLocation);
        //invoke service to retrieve this location
        if(targetService !=null)
            targetService.onReceiveLocation(locationResult);
        else
            debugLog("targetService not assigned!!");
    }

    ////////////ANDROID API

    @Override
    public void onLocationChanged(android.location.Location location) {
        // send result to targetService
        if(!isAutoRequestUpdate)
            targetService.removeListener(this);
        debugLog("Android on location changed received:\n" + location.toString());

        Location locationResult=AndroidLocationManager.toLocation(location);
        //invoke service to retrieve this location
        if(targetService !=null)
            targetService.onReceiveLocation(locationResult);
        else
            debugLog("targetService not assigned!!");
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }


}