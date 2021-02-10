package com.plaid.platform.porcelain;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationCompat.BigPictureStyle;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.TaskStackBuilder;
import androidx.core.content.ContextCompat;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.snackbar.Snackbar;
import com.plaid.link.Plaid;
import com.plaid.link.configuration.LinkTokenConfiguration;
import com.plaid.link.result.LinkResultHandler;
import com.plaid.platform.porcelain.common.mock.MockDatabase;
import com.plaid.platform.porcelain.common.util.NotificationUtil;
import com.plaid.platform.porcelain.db.DBManager;
import com.plaid.platform.porcelain.db.DatabaseHelper;
import com.plaid.platform.porcelain.handlers.BigPictureSocialIntentService;
import com.plaid.platform.porcelain.handlers.BroadcastService;
import com.plaid.platform.porcelain.models.ItemAdapter;
import com.plaid.platform.porcelain.models.Items;
import com.plaid.platform.porcelain.network.LinkTokenRequester;
import com.weiwangcn.betterspinner.library.material.MaterialBetterSpinner;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import kotlin.Unit;

@RequiresApi(api = Build.VERSION_CODES.O)
public class MainActivity<pubilc> extends AppCompatActivity {

    MaterialBetterSpinner priceMaterialSpinner, charityMaterialSpinner;
    private String priceValue, charityValue, transactionValue;
    private NotificationManagerCompat mNotificationManagerCompat;
    public static final int NOTIFICATION_ID = 888;
    private TextView mPendingDonate;
    private TextView mTotalDonate;
    private TextView result;
    private TextView tokenResult;
    private SharedPreferences sharedPreferences;
    private ClipboardManager clipboardManager;
    private ClipData clipData;

    @SuppressLint("StringFormatMatches")
    private final LinkResultHandler myPlaidResultHandler = new LinkResultHandler(

            linkSuccess -> {
                startWelcome();
                transactionData();
                sharedPreferences.edit().putString("accountid", linkSuccess.getMetadata().getAccounts().get(0).getId()).apply();
                sharedPreferences.edit().putString("publictoken",linkSuccess.getPublicToken()).apply();
//                linkSuccess.getMetadata().getAccounts().
                return Unit.INSTANCE;
            },
            linkExit -> {
                tokenResult.setText("");
                if (linkExit.getError() != null) {
                    result.setText(getString(
                            R.string.content_exit,
                            linkExit.getError().getDisplayMessage(),
                            linkExit.getError().getErrorCode()));
                } else {
                    result.setText(getString(
                            R.string.content_cancel,
                            linkExit.getMetadata().getStatus() != null ? linkExit.getMetadata()
                                    .getStatus()
                                    .getJsonValue() : "unknown"));
                }
                return Unit.INSTANCE;
            }
    );
    private PrefManager prefManager;
    private DBManager dbManager;
    private ListView itemListView;
    private ItemAdapter itemAdapter;
    private Cursor cursor;
    private SQLiteDatabase database;
    private Context mContext;
    ArrayList<Items> itemList = new ArrayList<Items>();
    private String name;
    private String type;
    private byte[] image;
    private String donateValue;
    private String donate;


    @SuppressLint({"ClickableViewAccessibility", "SetTextI18n", "CutPasteId"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbManager = new DBManager(this);
        dbManager.open();

        prefManager = new PrefManager(this);
        if (prefManager.isFirstTimeLaunch()) {
            dbManager.defaultDonateInsert(0.50, "United Way Worldwide", 0, 0, 0);
        }

        String moreString;
        if (savedInstanceState == null) {
            Bundle extras = getIntent().getExtras();
            if(extras == null) {
                moreString= null;
                startTermsDialog();
            } else {
                moreString= extras.getString("MORE_INFO");
            }
        } else {
            moreString= (String) savedInstanceState.getSerializable("MORE_INFO");
        }

        sharedPreferences=getSharedPreferences("accountdata",MODE_PRIVATE);

        result = findViewById(R.id.transaction_value);
        tokenResult = findViewById(R.id.total_donate_value);

        mPendingDonate = (TextView) findViewById(R.id.pending_donate_value);
        mTotalDonate = (TextView) findViewById(R.id.total_donate_value);

        if (!isAppServiceRunning(BroadcastService.class)){
            startService(new Intent(getApplicationContext(), BroadcastService.class));
        }

        // price spinner
        final String[] priceCodes = getResources().getStringArray(R.array.donate_price);

        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_dropdown_item_1line, priceCodes);
        priceMaterialSpinner = (MaterialBetterSpinner)
                findViewById(R.id.priceSpinner);
        priceMaterialSpinner.setAdapter(arrayAdapter);
        priceMaterialSpinner.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                String optionSelected = s.toString();
                Log.i("beforeTextChanged", optionSelected);
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String optionSelected = s.toString();
                Log.i("onTextChanged", optionSelected);
            }

            @Override
            public void afterTextChanged(Editable s) {
                priceValue = priceMaterialSpinner.getText().toString();
                Log.i("afterTextChanged", priceValue);
                double cDonate = Global.stringToPrice(priceValue);
                donate = Global.decimalConvert(cDonate);
                int t = dbManager.donateUpdate(Double.parseDouble(donate));

            }
        });

        // charity spinner
        final String[] charityCodes = getResources().getStringArray(R.array.charity);
        ArrayAdapter<String> priceCodeAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_dropdown_item_1line, charityCodes);
        charityMaterialSpinner = (MaterialBetterSpinner)
                findViewById(R.id.charitySpinner);
        charityMaterialSpinner.setAdapter(priceCodeAdapter);
        charityMaterialSpinner.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                String optionSelected = s.toString().toLowerCase();
                Log.i("beforeTextChanged", optionSelected);
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String optionSelected = s.toString().toLowerCase();
                Log.i("onTextChanged", optionSelected);
            }

            @Override
            public void afterTextChanged(Editable s) {
                charityValue = charityMaterialSpinner.getText().toString();
                int t = dbManager.charityUpdate(charityValue);
                Log.i("afterTextChanged", charityValue);
//                charitySave();
            }
        });

        //Load donate, charity, pending...
        Cursor data = dbManager.getData("SELECT * FROM DONATION");
        while(data.moveToNext()) {

            double getDonate = data.getDouble(1);
            String getCharity = data.getString(2);
            double getPendingDonate = data.getDouble(3);
            double getTotalDonate = data.getDouble(4);
            double getTotalTransaction = data.getDouble(5);
            Log.e("getPendingDonate", String.valueOf(getPendingDonate));
            Log.e("getTotalDonate", String.valueOf(getTotalDonate));
            Log.e("getDonate", String.valueOf(getDonate));
            Log.e("getCharity", getCharity);

            priceMaterialSpinner.setText("$" + Global.decimalConvert(getDonate));
            charityMaterialSpinner.setText(getCharity);
            mPendingDonate.setText("$" + Global.decimalConvert(getPendingDonate));
            mTotalDonate.setText("$" + Global.decimalConvert(getTotalDonate));
//
//            if( getDonate != 0.00){
//
//            }
        }
        data.close();

        //Copy URL Clipboard
        Button btncpy = findViewById(R.id.copy_btn);
        TextView ctxt = findViewById(R.id.cUrl);

        clipboardManager = (ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
        btncpy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String txtCopy = ctxt.getText().toString();
                clipData = ClipData.newPlainText("text",txtCopy);
                clipboardManager.setPrimaryClip(clipData);
                Toast.makeText(getApplicationContext(),"Data Copied to Clipboard", Toast.LENGTH_SHORT).show();
            }
        });
        Button moreBtn = findViewById(R.id.more_btn);
        Button withdrawBtn = findViewById(R.id.withdraw_btn);
        moreBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, MoreInfoActivity.class);
                startActivity(intent);
            }
        });

        //Rewards
        //Add Image to assets
        rewardInit();

        //Withdraw Button Event
        withdrawBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setCancelable(false);
                builder.setMessage(R.string.withdraw_required);
                builder.setNegativeButton("OK",new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                AlertDialog alert=builder.create();
                alert.show();
            }
        });
    }

    public void insertItemImageFromAssets(String item_name, String item_type, String image_name)  {
        DatabaseHelper dbHelper = new DatabaseHelper(getApplicationContext());
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        byte[] petimage = new byte[0];
        int image_size = 0;

        try {
            InputStream is = getApplicationContext().getAssets().open(image_name);
            image_size = is.available();
            petimage = new byte[image_size];
            is.read(petimage);
            is.close();

        } catch (IOException e) {
        }
        ContentValues cv = new ContentValues();
        cv.put(DatabaseHelper.ITEM_NAME,item_name);
        cv.put(DatabaseHelper.ITEM_TYPE,item_type);
        if (image_size > 0) {
            cv.put(DatabaseHelper.ITEM_IMAGE,petimage);
        }
        db.insert(DatabaseHelper.ITEM_TABLE, null, cv);
    }

    private void setOptionalEventListener() {
        Plaid.setLinkEventListener(linkEvent -> {
            Log.i("Event", linkEvent.toString());
            return Unit.INSTANCE;
        });
    }

    private boolean isAppServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    @SuppressLint("CheckResult")
    private void openLink() {
        LinkTokenRequester.INSTANCE.getToken()
                .subscribe(this::onLinkTokenSuccess, this::onLinkTokenError);
    }

    private void onLinkTokenSuccess(String token) {
        Plaid.create(
                getApplication(),
                new LinkTokenConfiguration.Builder()
                        .token(token)
                        .build())
                .open(this);
    }

    private void onLinkTokenError(Throwable error) {
        Toast.makeText(this, error.getMessage(), Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (!myPlaidResultHandler.onActivityResult(requestCode, resultCode, data)) {
            Log.i(MainActivity.class.getSimpleName(), "Not handled");
        }
    }

    // Terms and Conditions Dialog
    @SuppressLint("SetJavaScriptEnabled")
    private void startTermsDialog(){
//        prefManager.setFirstTimeLaunch(false);

        View customLayout = LayoutInflater.from(MainActivity.this).inflate(R.layout.activity_terms_conditions, null);
        @SuppressLint("ResourceType") AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomDialogTheme);
        builder.setView(customLayout);
        builder.setCancelable(false);
        WebView webView = (WebView) customLayout.findViewById(R.id.terms_web);
        webView.setVerticalScrollBarEnabled(true);
        webView.setHorizontalScrollBarEnabled(true);
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setBuiltInZoomControls(true);
        webView.clearCache(true);
        webView.loadUrl("file:///android_asset/terms.html");

        AlertDialog termsAlert=builder.create();
        termsAlert.show();

        CheckBox mAgreeCheck = (CheckBox) customLayout.findViewById(R.id.terms_checkBox);
        Button connectBtn = customLayout.findViewById(R.id.connect_btn);
        connectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            if (mAgreeCheck.isChecked()){
                setOptionalEventListener();
                openLink();
                termsAlert.dismiss();
            }else{
                Toast.makeText(MainActivity.this, "Read and agree to the terms and conditions", Toast.LENGTH_LONG).show();
            }
            }
        });
    }

    // Welcome Dialog -- once time
    private void startWelcomeDialog(){
        prefManager.setFirstTimeLaunch(false);

        View customLayout = LayoutInflater.from(MainActivity.this).inflate(R.layout.activity_welcome_dialog, null);
        @SuppressLint("ResourceType") AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomDialogTheme);
        builder.setView(customLayout);
        AlertDialog welcomeAlert=builder.create();
        welcomeAlert.show();

        Button okBtn = customLayout.findViewById(R.id.okay_btn);
        okBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                welcomeAlert.dismiss();
            }
        });

    }

    private void startWelcome(){
        // Checking for first time launch - before calling setContentView()
        prefManager = new PrefManager(this);
        if (prefManager.isFirstTimeLaunch()) {
            startWelcomeDialog();
        }
    }
    // Get All and nae transactions
    private void transactionData(){
        RequestQueue queue = Volley.newRequestQueue(this);
        String url = Global.BASE_URL + "/api/transactions";
        StringRequest ExampleStringRequest = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
            @SuppressLint("LongLogTag")
            @Override
            public void onResponse(String response) {
                //This code is executed if the server responds, whether or not the response contains data.
                Log.d("fetchFriendsCardsData API response is: ", response);
                try {
                    Cursor data = dbManager.getData("SELECT * FROM DONATION");
                    while (data.moveToNext()) {

                        double getTotalTransaction = data.getDouble(5);
                        JSONObject transactionJson = new JSONObject(response);
                        String currentTrans = String.valueOf(getTotalTransaction);
                        Log.d("sdsd", String.valueOf(transactionJson.getInt("total_transactions")+" "+Integer.parseInt(currentTrans)));
                        int getTransactions = transactionJson.getInt("total_transactions");
                        int newTransCnt =getTransactions- Integer.parseInt(currentTrans);
                        priceValue = priceMaterialSpinner.getText().toString();
                        if(newTransCnt > 0 && !priceValue.equals("$0.00")){
                            getDonations(newTransCnt);
                            result.setText("New Transactions: " + newTransCnt);
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
                mPendingDonate.setText("$" + Global.decimalConvert(reTotalPendDonate));
                // Transfer Funds $5...
                transferFund();
                //Total Donation Update.
                dbManager.totalDonateUpdate(getTotalDonate + donate);
                mTotalDonate.setText("$" + Global.decimalConvert(getTotalDonate + donate));
                //notification
                mNotificationManagerCompat = NotificationManagerCompat.from(getApplicationContext());
                boolean areNotificationsEnabled = mNotificationManagerCompat.areNotificationsEnabled();
                CoordinatorLayout mMainLayout = findViewById(R.id.mainlayout);
                if (!areNotificationsEnabled) {
                    // Because the user took an action to create a notification, we create a prompt to let
                    // the user re-enable notifications for this application again.
                    Snackbar snackbar = Snackbar
                            .make(
                                    mMainLayout,
                                    "You need to enable notifications for this app",
                                    Snackbar.LENGTH_LONG)
                            .setAction("ENABLE", new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    // Links to this app's notification settings
                                    openNotificationSettingsForApp();
                                }
                            });
                    snackbar.show();
                    return;
                }
                generateBigPictureStyleNotification(getCharity);

            } else {
                dbManager.pendingDonateUpdate(totalPendingDonate);
                mPendingDonate.setText("$" + Global.decimalConvert(totalPendingDonate));
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

                    Log.d("newTransCnt:" , transactionJson.toString());
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

    //Rewards init function
    @SuppressLint("ClickableViewAccessibility")
    public void rewardInit(){
        SQLiteOpenHelper mDatabaseHelper;
        mDatabaseHelper= new DatabaseHelper(this);
        mDatabaseHelper.getWritableDatabase().delete(DatabaseHelper.ITEM_TABLE,null,null); //<<<<<<<<<< Delete all pets
        insertItemImageFromAssets("Smart Watch with Camera", "Smart Watch", "Smart Watch with Camera.jpg");
        insertItemImageFromAssets("Android 4.4 Quad Core Tablet", "Tablet", "Android 4.4 Quad Core Tablet.jpg");
        insertItemImageFromAssets("Oculus Rift VR Headset", "VR Device", "Oculus Rift VR Headset.jpg");
        insertItemImageFromAssets("Smart Home Doorbell Camera & Alarm", "Camera", "Smart Home Doorbell Camera & Alarm.jpg");
        insertItemImageFromAssets("GPS Drone with Camera", "Drone", "GPS Drone with Camera.jpg");
        insertItemImageFromAssets("Hydroponic Smart Garden", "Smart Garden", "Hydroponic Smart Garden.jpg");
        insertItemImageFromAssets("Wireless Earbuds (Android and iOS)", "Wireless Device", "Wireless Earbuds (Android and iOS).jpg");

        itemListView = findViewById(R.id.item_list_view);
        itemListView.setEmptyView(findViewById(R.id.empty));
        itemList = new ArrayList<>();
        itemAdapter = new ItemAdapter(this, R.layout.activity_item_view_record, itemList);
        itemListView.setAdapter(itemAdapter);
        itemListView.setOnTouchListener(new ListView.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getAction();
                switch (action) {
                    case MotionEvent.ACTION_DOWN:
                        // Disallow ScrollView to intercept touch events.
                        v.getParent().requestDisallowInterceptTouchEvent(true);
                        break;

                    case MotionEvent.ACTION_UP:
                        // Allow ScrollView to intercept touch events.
                        v.getParent().requestDisallowInterceptTouchEvent(false);
                        break;
                }
                // Handle ListView touch events.
                v.onTouchEvent(event);
                return true;
            }
        });


        Cursor data = dbManager.getData("SELECT * FROM ITEMS");
        itemList.clear();
        while(data.moveToNext()) {

            name = data.getString(2);
            type = data.getString(3);
            image = data.getBlob(1);

            itemList.add(new Items( name, type, image));
            Log.i("image",String.valueOf(image));
            Log.i("name",String.valueOf(name));
            Log.i("type",String.valueOf(type));
        }
        data.close(); //<<<<<<<<<< SHOULD ALWAYS CLOSE CURSOR WHEN DONE WITH IT
        itemAdapter.notifyDataSetChanged();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu, menu);
        return true;
    }

    public void showPopup(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.menu, popup.getMenu());
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener(){
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch(item.getItemId()){
                    case R.id.menuAbout:
//                        Toast.makeText(this, "You clicked about", Toast.LENGTH_SHORT).show();
                        break;

                    case R.id.menuReset:

                            dbManager.resetDonateDB();
                            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                            startActivity(intent);
                        break;

                    case R.id.menuLogout:
//                        Toast.makeText(this, "You clicked logout", Toast.LENGTH_SHORT).show();
                        break;
                    default:
                        return false;
                }
                return true;
            }
        });
        popup.show();
    }

    private void generateBigPictureStyleNotification(String charity) {

        Log.d("TAG", "generateBigPictureStyleNotification()");
        // 0. Get your data (everything unique per Notification).
        MockDatabase.BigPictureStyleSocialAppData bigPictureStyleSocialAppData =
                MockDatabase.getBigPictureStyleData();

        // 1. Create/Retrieve Notification Channel for O and beyond devices (26+).
        String notificationChannelId =
                NotificationUtil.createNotificationChannel(this, bigPictureStyleSocialAppData);

        // 2. Build the BIG_PICTURE_STYLE.
        BigPictureStyle bigPictureStyle = new BigPictureStyle()
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
//        RemoteInput remoteInput =
//                new RemoteInput.Builder(BigPictureSocialIntentService.EXTRA_COMMENT)
//                        .setLabel(replyLabel)
//                        // List of quick response choices for any wearables paired with the phone
//                        .setChoices(bigPictureStyleSocialAppData.getPossiblePostResponses())
//                        .build();

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

    @Override
    public void onBackPressed() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);
        builder.setMessage("Do you want to Sign Out?");
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //if user pressed "yes", then he is allowed to exit from application
//                signOut();
                MainActivity.this.finishAffinity();
                sharedPreferences.edit().clear().apply();
            }
        });
        builder.setNegativeButton("No",new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //if user select "No", just cancel this dialog and continue with app
                dialog.cancel();
            }
        });
        AlertDialog alert=builder.create();
        alert.show();
    }

}
