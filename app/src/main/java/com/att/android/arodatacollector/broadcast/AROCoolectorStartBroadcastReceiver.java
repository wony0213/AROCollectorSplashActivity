package com.att.android.arodatacollector.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.att.android.arodatacollector.main.AROCollectorService;
import com.att.android.arodatacollector.main.AROCollectorTraceService;
import com.att.android.arodatacollector.main.ARODataCollector;
import com.att.android.arodatacollector.utils.AROCollectorUtils;
import com.att.android.arodatacollector.utils.AROLogger;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Administrator on 2015/8/27.
 */
public class AROCoolectorStartBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "ARO.AROCoolectorStart";
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

    public AROCoolectorStartBroadcastReceiver() {
        mAroUtils = new AROCollectorUtils();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) {
            AROLogger.d(TAG, "intent is null");
        }
        String traceFolderName = intent.getStringExtra("traceFolderName");
        if (traceFolderName == null) {
            AROLogger.d(TAG, "Trace folder fame is null");
        }
        ARODataCollector app = (ARODataCollector) context.getApplicationContext();
        app.setTcpDumpTraceFolderName(traceFolderName);
        app.setCollectorLaunchfromAnalyzer(true);
        app.setDataCollectorStopEnable(false);
        startARODataCollector(app);
    }

    /**
     * Starts the ARO Data Collector trace in the background by starting the
     * tcpdump/ffmpeg in native shell along with other peripherals trace like
     * Wifi,Battery,GPS,Screen State,Bluetooth,Radio states.The
     * startARODataCollector waits for 15 seconds to make sure all traces have
     * been started in bacground before showing failed message to user. In case
     * of failed start all traces files under trace folder name will be deleted
     * from SD card.
     */
    private void startARODataCollector(final ARODataCollector app) {

        // Timer object which start as soon user press the Start Data Collector
        // to checks the tcpdump execution in the shell
        final Timer aroDCStartWatchTimer = new Timer();
        // Timers to get the PS list from the shell every seconds to verify
        // tcpdump execution till SCStartWatchTimer times out*/
        final Timer aroDCStartTimer = new Timer();
        // Task Killer process info class to manage and store all running
        // process

        app.setARODataCollectorStopFlag(false);
        app.setDataCollectorInProgressFlag(true);
        app.setRequestDataCollectorStop(false);
        app.setVideoCaptureFailed(false);
        createAROTraceDirectory(app);

        if (app.getDumpTraceFolderName() != null) {

            //Takes a snap shoot of the time the system booted to be used for the timer on the home page.
            app.setElapsedTimeStartTime(System.currentTimeMillis());

            // Starting the ARO Data collector service before tcpdump to record
            // >=t(0)
            app.startService(new Intent(app, AROCollectorTraceService.class));
            // Starting the tcpdump service and starts the video capture
            app.startService(new Intent(app, AROCollectorService.class));
            app.setCollectVideoOption(false);
            // ARO Watch timer for failed start message of data collector after 15
            // sec
            aroDCStartWatchTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    if (!app.getTcpDumpStartFlag()) {
                        AROLogger.w(TAG, "Failed to start ARODataCollector in 15 sec");
                        app.stopService(new Intent(app, AROCollectorTraceService.class));
                        app.stopService(new Intent(app, AROCollectorService.class));
                        // As we collect peripherals trace i.e wifi,GPs
                        // service before tcpdump trace so we making sure we delete
                        // all of the traces if we don't have tcpdump running
                        mAroUtils.deleteTraceFolder(new File(app.getTcpDumpTraceFolderName()));
                    }
                    // Cancel the timers
                    aroDCStartWatchTimer.cancel();
                    aroDCStartTimer.cancel();
                }
            }, ARO_START_WATCH_TIME);
            // Timer to check start data collector kick-off within 15 secs
            aroDCStartTimer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    app.setTcpDumpStartFlag(AROCollectorUtils.isTcpDumpRunning());
                    if (app.getTcpDumpStartFlag()) {
                        app.hideProgressDialog();
                        app.setDataCollectorInProgressFlag(false);
                        app.triggerAROAlertNotification();
                        aroDCStartWatchTimer.cancel();
                        aroDCStartTimer.cancel();
                    }
                }
            }, ARO_START_TICK_TIME, ARO_START_TICK_TIME);
        }
    }

    /**
     * Creates the given trace directory on the device SD card under root
     * directory of ARO (\SDCARD\ARO)
     */
    private void createAROTraceDirectory(final ARODataCollector app) {

        final String mAroTraceDatapath = app.getTcpDumpTraceFolderName();
        final File traceFolder = new File(mAroTraceDatapath);
        final File traceRootFolder = new File(ARODataCollector.ARO_TRACE_ROOTDIR);

        AROLogger.d(TAG, "mAroTraceDatapath=" + mAroTraceDatapath);

        // Creates the trace root directory
        if (!traceRootFolder.exists()) {
            traceRootFolder.mkdir();
        }
        // Creates the trace directory inside /SDCARD/ARO
        if (!traceFolder.exists()) {
            traceFolder.mkdir();
        }
    }
}
