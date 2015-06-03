package com.yamibo.main.yamibolib.locationservice.impl;

/**
 * Created by Clover on 2015-06-03.
 */
public interface LocManager {
    public void initLocation();
    public void start();
    public boolean requestLocation();
    public void stop();
}
