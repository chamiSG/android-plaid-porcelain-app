package com.plaid.donate.porcelain;

import android.content.Context;
import android.content.SharedPreferences;

public class PrefManager {
    SharedPreferences pref;
    SharedPreferences.Editor editor;
    Context _context;

    // shared pref mode
    int PRIVATE_MODE = 0;

    // Shared preferences file name
    private static final String PREF_NAME = "start_welcome";

    private static final String IS_FIRST_TIME_LAUNCH = "IsFirstTimeLaunch";
    private static final String IS_FIRST_TIME_LOGIN = "IsFirstTimeLogin";
    private static final String IS_FIRST_TIME_BANK = "IsFirstTimeBank";

    public PrefManager(Context context) {
        this._context = context;
        pref = _context.getSharedPreferences(PREF_NAME, PRIVATE_MODE);
        editor = pref.edit();
    }

    public void setFirstTimeLaunch(boolean isFirstTime) {
        editor.putBoolean(IS_FIRST_TIME_LAUNCH, isFirstTime);
        editor.commit();
    }

    public boolean isFirstTimeLaunch() {
        return pref.getBoolean(IS_FIRST_TIME_LAUNCH, true);
    }

    public void setFirstTimeLogin(boolean isFirstTime) {
        editor.putBoolean(IS_FIRST_TIME_LOGIN, isFirstTime);
        editor.commit();
    }

    public boolean isFirstTimeLogin() {
        return pref.getBoolean(IS_FIRST_TIME_LOGIN, true);
    }

    public void setFirstTimeBank(boolean isFirstTime) {
        editor.putBoolean(IS_FIRST_TIME_BANK, isFirstTime);
        editor.commit();
    }

    public boolean isFirstTimeBank() {
        return pref.getBoolean(IS_FIRST_TIME_BANK, true);
    }
}