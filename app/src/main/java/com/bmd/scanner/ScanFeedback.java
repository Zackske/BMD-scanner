package com.bmd.scanner;

import android.os.Handler;
import android.os.SystemClock;

/** Spaces successful-code haptics so multiple codes do not overwrite one pulse. */
final class ScanFeedback {
    private final Handler handler;
    private final Runnable pulse;
    private final Object token=new Object();
    private boolean enabled=true;
    private long nextPulse;
    ScanFeedback(Handler handler,Runnable pulse){this.handler=handler;this.pulse=pulse;}
    void setEnabled(boolean enabled){this.enabled=enabled;if(!enabled)cancel();}
    void success(){
        if(!enabled)return;
        long when=Math.max(SystemClock.uptimeMillis(),nextPulse);nextPulse=when+160;
        handler.postAtTime(()->{if(enabled)pulse.run();},token,when);
    }
    void cancel(){handler.removeCallbacksAndMessages(token);nextPulse=0;}
}
