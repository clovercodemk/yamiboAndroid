package com.yamibo.main.yamibolib.locationservice.impl;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.yamibo.main.yamibolib.locationservice.LocationListener;
import com.yamibo.main.yamibolib.locationservice.LocationService;
import com.yamibo.main.yamibolib.locationservice.model.Location;

import android.content.Context;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.yamibo.main.yamibolib.locationservice.impl.DefaultLocationService.debugLog;
import static com.yamibo.main.yamibolib.locationservice.impl.DefaultLocationService.isLocationEnabled;

/**
 * Clover on 2015-06-01
 * helper class to control BDLocationClient.<br>
 * modified from default BD location service sample, which is an Application classuse getApplication() in activity to control this, and register its name in manifest.xml
 * 将LocationListener 翻译为apiListener
 */
class BDLocationService implements APILocationService {
    DefaultLocationService supervisorService=null;

    /**
     * 为每个LocationListener提供一个相应的API location listener
     */
    Map<LocationListener, BDListener> mapListeners=new HashMap<LocationListener, BDListener>();


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
    private boolean isAutoSwitchService =true;
    private int DEFAULT_UPDATE_INTERVAL=10*60*1000;//default requestLocation time 10min
    /**
     * 当更新时间小于1000ms时，为单次更新
     */
    private int updateInterval =-1;
    private int providerChoice=PROVIDER_BEST;
    private boolean isStarted=false;




    public static final int BAIDU_MODE=0;
    public static final int ANDROID_API_MODE=1;
    public static final int PROVIDER_BEST=0;
    public static final int PROVIDER_NETWORK=1;
    public static final int PROVIDER_GPS=2;


    /**
     * to be read by the textView for shown to mobile activity
     */
    public String debugMessage = null;

    //private APILocationService locManager = null;


    /**
     *
     */
//    private List<LocationListener> listeners = new ArrayList<>();


    //Use the following two flags to track the working status of location application
    private boolean isLocationDemand=false;
    private boolean isLocationReceived=false;


    private LocationClient bdLocationClient;

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
    public BDLocationService(Context context, int updateInterval, int providerChoice, DefaultLocationService supervisorService) {
        this.supervisorService= supervisorService;
        bdLocationClient = new LocationClient(context);
        setUpdateInterval(updateInterval);
        setProvider(providerChoice);
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

    /**Baidu sample, TODO test set global option for the client?
     *
     */
    void applyOption() {
        LocationClientOption option = new LocationClientOption();
        option.setLocationMode(locationMode);//设置定位模式
        option.setCoorType(COORD_MODE);//返回的定位结果是百度经纬度
        option.setScanSpan(span);
        option.setIsNeedAddress(IS_NEED_ADDRESS);

        bdLocationClient.setLocOption(option);
        debugLog("initiate BD client Option done");
    }

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


    /**
     * BDclient start(). <br>
     * BDclient's requestLocation() not included
     * TODO 测试多次调用
     */
    public boolean start(){
        if (bdLocationClient== null) {
            return false;
        }
        if(isStarted){
            debugLog("already in work");
            return true;
        }
        if (isLocationEnabled(mContext)) {
            DefaultLocationListener listener=new DefaultLocationListener();
            addListener(listener);
            bdLocationClient.start();;
            isStarted=true;
            isLocationDemand = requestLocation();


            debugLog("location service starts");
            return true;
        } else {
            debugLog("PLEASE enable system's location permission");
            return false;
        }

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
    boolean requestLocation() {
        debugLog("Request BDLocation update");

        int code= bdLocationClient.requestLocation();
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
     * TODO 之后是否需要启动client？
     */
    void registerListener(BDListener bdListener){
        //TODO check type is DefaultLocationListener, if not, need a translation
        bdLocationClient.registerLocationListener(bdListener);
        debugLog("BDListener added" + bdListener.toString());
        applyOption();
    }

    void unregisterListener(BDListener bdListener) {
        //TODO check type is DefaultLocationListener, if not, need a translation
        bdLocationClient.unRegisterLocationListener(bdListener);
        debugLog("BDListener removed: " + bdListener.toString());
    }



    /**
     * BD client stop
     * TODO test multiple invoke
     */
    public void stop() {
        if (bdLocationClient == null)
            return;

        //reset flags
        resetFlag();
        for (LocationListener listener: mapListeners.keySet())
            removeListener(listener);
        bdLocationClient.stop();
        isStarted=false;
        debugLog("location service stops");
    }

    @Override
    public boolean refresh() {
        if(!isLocationEnabled(mContext))
            return false;
        if (bdLocationClient == null)
            return false;
       return requestLocation();
    }

    @Override
    public void addListener(LocationListener listener) {
        if (listener != null && !mapListeners.containsKey(listener)) {
            BDListener bdListener=new BDListener(listener,this);
            registerListener(bdListener);
            mapListeners.put(listener,bdListener);
        }
    }

    @Override
    public void removeListener(LocationListener listener) {
        if (listener != null && mapListeners.containsKey(listener)) {
            unregisterListener(mapListeners.get(listener));
            mapListeners.remove(listener);
        }
    }

    @Override
    public void resetServiceOption(int updateInterval, int providerChoice) {
            setUpdateInterval(updateInterval);
            setProvider(providerChoice);
            applyOption();
    }


    /**
     * TODO 将百度地址翻译为model里的Location
     * @param source
     * @return
     */
    public static Location toLocation(BDLocation source){
        return null;
    }
    //Location location = new Location();
    //return location;

    //TODO check if this is correct
    public static boolean isInChina(BDLocation location){
        return(location.getCountry().equals("china"));
    }

    private void resetFlag() {
        isLocationDemand=false;
        isLocationReceived=false;
    }
}
