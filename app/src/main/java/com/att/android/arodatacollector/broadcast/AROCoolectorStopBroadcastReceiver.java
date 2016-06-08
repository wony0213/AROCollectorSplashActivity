package com.att.android.arodatacollector.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.att.android.arodatacollector.main.AROCollectorService;
import com.att.android.arodatacollector.main.AROCollectorTraceService;
import com.att.android.arodatacollector.main.ARODataCollector;
import com.att.android.arodatacollector.utils.AROCollectorUtils;
import com.att.android.arodatacollector.utils.AROLogger;

/**
 * Created by Administrator on 2015/8/27.
 */
public class AROCoolectorStopBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "ARO.AROCoolectorStop";
    /**
     * The tcpdump start time value which is set to 15 seconds. The timer will
     * wait for 15 seconds to check if tcpdump is kicked off in device native
     * shell
     */
    private static final int ARO_START_WATCH_TIME = 15000;

    /**
     * The tcpdump start timer tick time every second
     */
    private static final int ARO_START_TICK_TIME = 1000;
    private AROCollectorUtils mAroUtils;

    public AROCoolectorStopBroadcastReceiver() {
        mAroUtils = new AROCollectorUtils();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) {
            AROLogger.d(TAG, "intent is null");
        }

        ARODataCollector app = (ARODataCollector) context.getApplicationContext();
        app.setCollectorLaunchfromAnalyzer(true);
        app.setDataCollectorStopEnable(false);
        stopARODataCollector(app);
    }

    /**
     * Stops the data collector trace by stopping Video Trace and tcpdump from
     * shell
     */
    private void stopARODataCollector(ARODataCollector app) {
        AROLogger.d(TAG, "Inside stopARODataCollector....");
        app.setARODataCollectorStopFlag(true);
        if (app != null) {
            AROLogger.d(TAG, "calling unregisterUsbBroadcastReceiver inside stopARODataCollector");
        }
        if (AROCollectorService.getServiceObj() != null && AROCollectorTraceService.getServiceObj() != null) {
            // Sends the STOP Command to tcpdump socket and Stop the Video
            // capture on device
            AROCollectorService.getServiceObj().requestDataCollectorStop();
            app.cancleAROAlertNotification();
        } else {
            AROLogger.e(TAG, "inside AROCollectorHomeActivity.stopARODataCollector, but AROCollectorService/AROCollectorTraceService is null. Timestamp: " + System.currentTimeMillis());
            //this typically happens when the service had been killed by Android and has not been restarted.
            //This should no longer happen since we implemented these services as foreground service
        }

    }
}
