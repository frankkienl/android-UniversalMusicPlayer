package nl.frankkie.bronyradio;

import android.app.Application;

/**
 * Created by frankb on 26/07/2017.
 */

public class MyApplication extends Application {
    public static Application app;

    @Override
    public void onCreate() {
        super.onCreate();
        app = this;
    }
}
