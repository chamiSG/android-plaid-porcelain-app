package com.plaid.platform.porcelain;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity{

    private PrefManager prefManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        /****** Create Thread that will sleep for 5 seconds****/
        Thread background = new Thread() {
            public void run() {
                try {
                    // Thread will sleep for 5 seconds
                    sleep(5*1000);
                    // After 5 seconds redirect to another intent
                    prefManager = new PrefManager(getApplicationContext());
                    if (prefManager.isFirstTimeLogin()) {
                        Intent i=new Intent(getBaseContext(),SignInActivity.class);
                        startActivity(i);
                    }else {
                        Intent i=new Intent(getBaseContext(),MainActivity.class);
                        startActivity(i);
                    }

                    //Remove activity
                    finish();
                } catch (Exception e) {
                }
            }
        };
        // start thread
        background.start();
    }


}
