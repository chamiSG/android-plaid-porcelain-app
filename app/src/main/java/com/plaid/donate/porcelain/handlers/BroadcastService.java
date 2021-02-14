package com.plaid.donate.porcelain.handlers;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.plaid.donate.porcelain.Global;
import com.plaid.donate.porcelain.GlobalNotificationBuilder;
import com.plaid.donate.porcelain.MainActivity;
import com.plaid.donate.porcelain.R;
import com.plaid.donate.porcelain.common.mock.MockDatabase;
import com.plaid.donate.porcelain.common.util.NotificationUtil;
import com.plaid.donate.porcelain.db.DBManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.TaskStackBuilder;
import androidx.core.content.ContextCompat;

import static com.plaid.donate.porcelain.MainActivity.NOTIFICATION_ID;

public class BroadcastService extends Service {

    private final static String TAG = "BroadcastService";

    public static final String COUNTDOWN_BR = "com.lazycoder.cakevpn.countdown_br";
    Intent bi = new Intent(COUNTDOWN_BR);
    private NotificationManagerCompat mNotificationManagerCompat;

    private DBManager dbManager;

    @Override
        public void onCreate() {
            super.onCreate();

            Handler handler=new Handler();

            dbManager = new DBManager(this);
            dbManager.open();

            //Load donate, charity, pending...
            Cursor data = dbManager.getData("SELECT * FROM DONATION");
            while(data.moveToNext()) {
                double getDonate = data.getDouble(1);
                String getCharity = data.getString(2);
                double getPendingDonate = data.getDouble(3);
                double getTotalDonate = data.getDouble(4);
            }
            data.close();

            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    transactionData();
                    handler.postDelayed(this,1000*60*10);

                }
            }, 1000*60);

        }

        @Override
        public void onDestroy() {

            super.onDestroy();
        }

        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {
            return super.onStartCommand(intent, flags, startId);
        }

        @Override
        public IBinder onBind(Intent arg0) {
            return null;
        }

    // Get All and nae transactions
    private void transactionData(){
        RequestQueue queue = Volley.newRequestQueue(this);
        String url = Global.BASE_URL +  "/api/transactions";
        StringRequest ExampleStringRequest = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
            @SuppressLint("LongLogTag")
            @Override
            public void onResponse(String response) {
                //This code is executed if the server responds, whether or not the response contains data.
                Log.d("fetchFriendsCardsData API response is: ", response);
                try {
                    Cursor data = dbManager.getData("SELECT * FROM DONATION");
                    while (data.moveToNext()) {

                        double getDonate = data.getDouble(1);
                        double getTotalTransaction = data.getDouble(5);
                        JSONObject transactionJson = new JSONObject(response);
                        String currentTrans = String.valueOf(getTotalTransaction);
                        Log.d("sdsd", String.valueOf(transactionJson.getInt("total_transactions")+" "+Integer.parseInt(currentTrans)));
                        int getTransactions = transactionJson.getInt("total_transactions");
                        int newTransCnt =getTransactions- Integer.parseInt(currentTrans);
//                        priceValue = priceMaterialSpinner.getText().toString();
                        if(newTransCnt > 0 && getDonate != 0.00){
                            getDonations(newTransCnt);
                        }
                        dbManager.transactionUpdate(getTransactions);
                        System.out.println("newTransCnt:" + newTransCnt);
                    }
                    data.close();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() { //Create an error listener to handle errors appropriately.
            @Override
            public void onErrorResponse(VolleyError error) {
                //This code is executed if there is an error.
            }

        });

        queue.add(ExampleStringRequest);
    }

    //Get Donations sum with Transactions Event.
    @SuppressLint("SetTextI18n")
    public void getDonations(int cnt) {
        double donate = 5.00;

        Cursor data = dbManager.getData("SELECT * FROM DONATION");
        while (data.moveToNext()) {

            double getDonate = data.getDouble(1);
            String getCharity = data.getString(2);
            double getPendingDonate = data.getDouble(3);
            double getTotalDonate = data.getDouble(4);
            double getTotalTransaction = data.getDouble(5);
            getDonate = getDonate * cnt;

            // Total Pending Donate
            double totalPendingDonate = getDonate + getPendingDonate;
            if (totalPendingDonate > 4.99) {
                int tmp = (int) totalPendingDonate / 5;
                double reTotalPendDonate = totalPendingDonate - donate * tmp;
                System.out.println("reTotalPendDonate: " + reTotalPendDonate);
                dbManager.pendingDonateUpdate(reTotalPendDonate);
//                mPendingDonate.setText("$" + Global.decimalConvert(reTotalPendDonate));
                // Transfer Funds $5...
                transferFund();
                //Total Donation Update.
                dbManager.totalDonateUpdate(getTotalDonate + donate);
                //notification
                mNotificationManagerCompat = NotificationManagerCompat.from(getApplicationContext());
                boolean areNotificationsEnabled = mNotificationManagerCompat.areNotificationsEnabled();
//                CoordinatorLayout mMainLayout = findViewById(R.id.mainlayout);
                if (!areNotificationsEnabled) {
                    // Because the user took an action to create a notification, we create a prompt to let
                    // the user re-enable notifications for this application again.
                    return;
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    generateBigPictureStyleNotification(getCharity);
                }

            } else {
                dbManager.pendingDonateUpdate(totalPendingDonate);
//                mPendingDonate.setText("$" + Global.decimalConvert(totalPendingDonate));
            }
        }
        data.close();
    }

    //Transfer $5
    private void transferFund(){
        RequestQueue queue = Volley.newRequestQueue(this);
        String url = Global.BASE_URL + "/api/transferFunds";
        StringRequest ExampleStringRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @SuppressLint("LongLogTag")
                    @Override
                    public void onResponse(String response) {
                        //This code is executed if the server responds, whether or not the response contains data.
                        try {

                            JSONObject transactionJson = new JSONObject(response);

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() { //Create an error listener to handle errors appropriately.
            @Override
            public void onErrorResponse(VolleyError error) {
                //This code is executed if there is an error.
            }
        }){
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<String, String>();
                SharedPreferences sharedPreferences = getSharedPreferences("accountdata", MODE_PRIVATE);
                params.put("accountid", sharedPreferences.getString("accountid",""));
                params.put("amount", String.valueOf(5));
                params.put("publictoken", sharedPreferences.getString("publictoken",""));
                return params;
            }
        };;
        queue.add(ExampleStringRequest).setRetryPolicy(new RetryPolicy() {
            @Override
            public int getCurrentTimeout() {
                return 10000;
            }

            @Override
            public int getCurrentRetryCount() {
                return 0; //retry turn off
            }

            @Override
            public void retry(VolleyError error) throws VolleyError {

            }
        });
//        queue.add(ExampleStringRequest);

    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void generateBigPictureStyleNotification(String charity) {

        // 0. Get your data (everything unique per Notification).
        MockDatabase.BigPictureStyleSocialAppData bigPictureStyleSocialAppData =
                MockDatabase.getBigPictureStyleData();

        // 1. Create/Retrieve Notification Channel for O and beyond devices (26+).
        String notificationChannelId =
                NotificationUtil.createNotificationChannel(this, bigPictureStyleSocialAppData);

        // 2. Build the BIG_PICTURE_STYLE.
        NotificationCompat.BigPictureStyle bigPictureStyle = new NotificationCompat.BigPictureStyle()
                // Provides the bitmap for the BigPicture notification.
                .bigPicture(
                        BitmapFactory.decodeResource(
                                getResources(),
                                bigPictureStyleSocialAppData.getBigImage()))
                // Overrides ContentTitle in the big form of the template.
                .setBigContentTitle(bigPictureStyleSocialAppData.getBigContentTitle())
                // Summary line after the detail section in the big form of the template.
                .setSummaryText("Donation $5 was transferred to" + charity +".");

        // 3. Set up main Intent for notification.
        Intent mainIntent = new Intent(this, MainActivity.class);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        // Adds the back stack.
        stackBuilder.addParentStack(MainActivity.class);
        // Adds the Intent to the top of the stack.
        stackBuilder.addNextIntent(mainIntent);
        // Gets a PendingIntent containing the entire back stack.
        PendingIntent mainPendingIntent =
                PendingIntent.getActivity(
                        this,
                        0,
                        mainIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );

        // 4. Set up RemoteInput, so users can input (keyboard and voice) from notification.

        // Create the RemoteInput.
        String replyLabel = getString(R.string.confirm);

        PendingIntent replyActionPendingIntent;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Intent intent = new Intent(this, BigPictureSocialIntentService.class);
            intent.setAction(BigPictureSocialIntentService.ACTION_COMMENT);
            replyActionPendingIntent = PendingIntent.getService(this, 0, intent, 0);

        } else {
            replyActionPendingIntent = mainPendingIntent;
        }

        NotificationCompat.Action replyAction =
                new NotificationCompat.Action.Builder(
                        R.drawable.ic_notification,
                        replyLabel,
                        replyActionPendingIntent)
                        .build();

        // 5. Build and issue the notification.
        NotificationCompat.Builder notificationCompatBuilder =
                new NotificationCompat.Builder(getApplicationContext(), notificationChannelId);

        GlobalNotificationBuilder.setNotificationCompatBuilderInstance(notificationCompatBuilder);

        notificationCompatBuilder
                // BIG_PICTURE_STYLE sets title and content for API 16 (4.1 and after).
                .setStyle(bigPictureStyle)
                // Title for API <16 (4.0 and below) devices.
                .setContentTitle(bigPictureStyleSocialAppData.getContentTitle())
                // Content for API <24 (7.0 and below) devices.
                .setContentText(bigPictureStyleSocialAppData.getContentText())
                .setSmallIcon(R.drawable.ic_notification)
                .setLargeIcon(BitmapFactory.decodeResource(
                        getResources(),
                        R.drawable.icon))
                .setContentIntent(mainPendingIntent)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                // Set primary color (important for Wear 2.0 Notifications).
                .setColor(ContextCompat.getColor(getApplicationContext(), R.color.colorPrimary))
                .setSubText(Integer.toString(1))
                .setCategory(Notification.CATEGORY_SOCIAL)
                .setPriority(bigPictureStyleSocialAppData.getPriority())
                .setVisibility(bigPictureStyleSocialAppData.getChannelLockscreenVisibility());
        for (String name : bigPictureStyleSocialAppData.getParticipants()) {
            notificationCompatBuilder.addPerson(name);
        }

        Notification notification = notificationCompatBuilder.build();

        mNotificationManagerCompat.notify(NOTIFICATION_ID, notification);
    }

    private void openNotificationSettingsForApp() {
        // Links to this app's notification settings.
        Intent intent = new Intent();
        intent.setAction("android.settings.APP_NOTIFICATION_SETTINGS");
        intent.putExtra("app_package", getPackageName());
        intent.putExtra("app_uid", getApplicationInfo().uid);

        // for Android 8 and above
        intent.putExtra("android.provider.extra.APP_PACKAGE", getPackageName());

        startActivity(intent);
    }

}