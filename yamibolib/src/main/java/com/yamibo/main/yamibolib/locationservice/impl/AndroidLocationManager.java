package com.yamibo.main.yamibolib.locationservice.impl;

import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;

import com.yamibo.main.yamibolib.locationservice.LocationListener;

import static com.yamibo.main.yamibolib.locationservice.impl.DefaultLocationService.debugLog;

/**
 * Created by Clover on 2015-06-03.
 * helper Class to control AndroidManager
 */
class AndroidLocationManager implements LocManager{
    private LocationManager locationManager;

    private int DEFAULT_INTERVAL=10*60*1000;//default requestLocation time 10min
    private int updateInterval =1000;
    private static String provider_best =null;
    private static String provider_network =null;
    private static String provider_gps =null;
    private static int MIN_DISTANCE= 0;
    private String provider;



    public AndroidLocationManager(Context mContext){
        locationManager=(LocationManager)mContext.getSystemService(Context.LOCATION_SERVICE);
        provider_best =locationManager.getBestProvider(new Criteria(),true);
        provider_network =locationManager.NETWORK_PROVIDER;
        provider_gps =locationManager.GPS_PROVIDER;
        provider= provider_network;
    }

    /**
     * doesn't support switch, included in the request listener part!
     */
    @Override
    public void applyOption() {
    }

    /**
     * when addListener, the request starts.
     */
    @Override
    public void start() {

    }

    @Override
    public boolean requestLocation() {
        //return false;
        return true;
    }

    /**
     * when all listener stop, client stops
     */
    @Override
    public void stop() {

    }

    @Override
    public void addListener(LocationListener listener) {
        //check type is DefaultLoctionListner, if not, need a translation
        if (updateInterval<1000) {
            updateInterval = 1000;
            ((DefaultLocationListener)listener).isAutoRequestUpdate=false;
        }
        else {
            ((DefaultLocationListener)listener).isAutoRequestUpdate=true;
            locationManager.requestLocationUpdates
                    (provider, updateInterval, MIN_DISTANCE, (DefaultLocationListener) listener);
            debugLog("Android autoRequest sent with provider: "+provider);
        }
    }

    @Override
    public void removeListener(LocationListener listener) {
        //check type is DefaultLocationListener, if not, need a translation
        locationManager.removeUpdates((DefaultLocationListener) listener);
        debugLog("Android listener removed");
    }

    @Override
    public void setUpdateInterval(int updateInterval) {
            this.updateInterval =updateInterval;
    }

    @Override
    public void setProvider(int providerChoice) {
        switch (providerChoice){
            case DefaultLocationService.PROVIDER_BEST:
                provider= provider_best;
                break;
            case DefaultLocationService.PROVIDER_NETWORK:
                provider= provider_network;
                break;
            case DefaultLocationService.PROVIDER_GPS:
                provider= provider_gps;
                break;
            default:
                debugLog("provider not supported!");
                return;
        }

    }

//TODO
    public static com.yamibo.main.yamibolib.locationservice.model.Location toLocation(Location location) {
        return null;
    }
}
