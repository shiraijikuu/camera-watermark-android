package com.shiraijikuu.cwm;

import android.os.Bundle;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        // Register the native gallery saver BEFORE super.onCreate (Capacitor requirement).
        registerPlugin(GallerySaverPlugin.class);
        super.onCreate(savedInstanceState);
    }
}
