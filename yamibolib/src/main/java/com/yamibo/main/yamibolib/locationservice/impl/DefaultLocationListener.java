package com.yamibo.main.yamibolib.locationservice.impl;

import android.os.Bundle;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.yamibo.main.yamibolib.locationservice.LocationService;
import com.yamibo.main.yamibolib.locationservice.model.Location;

import static com.yamibo.main.yamibolib.locationservice.impl.BDLocationManager.toStringOutput;
import static com.yamibo.main.yamibolib.locationservice.impl.DefaultLocationService.debugLog;

/**
 *
 * 实现实位回调监听
 * call back to location target location service
 */
public class DefaultLocationListener implements BDLocationListener, android.location.LocationListener, com.yamibo.main.yamibolib.locationservice.LocationListener{
    DefaultLocationService targetService=null;

    //USE this

    @Override
    public void onLocationChanged(LocationService sender) {

    }


    @Override
    /**
     * update locationResult,
     * call targetService.onReceiveBDLocation(location)
     */
    public void onReceiveLocation(BDLocation bdLocation) {


        debugLog("BDlocation received" + toStringOutput(bdLocation));

        Location locationResult=BDLocationManager.toLocation(bdLocation);
        //invoke service to retrieve this location
        if(targetService!=null)
            targetService.onReceiveLocation(locationResult);
        else
            debugLog("targetService not assigned!!");
    }

    ////////////ANDROID API

    @Override
    public void onLocationChanged(android.location.Location location) {

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