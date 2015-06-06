package com.yamibo.main.yamibolib.locationservice.impl;

import com.yamibo.main.yamibolib.locationservice.LocationListener;

/**
 * Created by Clover on 2015-06-03.
 */
public interface LocManager {
    public void applyOption();
    public void start();
    public boolean requestLocation();
    public void stop();

    public void addListener(LocationListener listener);
    public void removeListener(LocationListener listener);

    void setUpdateInterval(int updateInterval);

    void setProvider(int providerChoice);
}
