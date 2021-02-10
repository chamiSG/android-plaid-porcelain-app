package com.plaid.platform.porcelain;

import com.google.android.gms.common.api.internal.IStatusCallback;

import java.text.DecimalFormat;
import java.text.NumberFormat;

public class Global {

    public static final String BASE_URL = "http://198.12.255.51:80";

    public static double stringToPrice(String str) {
        double price;
       switch (str){
            case "$0.50":
                price = (double) 0.50;
            break;

            case "$0.75":
               price = (double) 0.75;
            break;

            case "$1.00":
               price = (double) 1.00;
            break;
            default:
                price = (double) 0.00;
        }
        return price;
    }

    public static String splitString3(String strMain){
        String[] arrSplit_3 = strMain.split("\\$");
        return arrSplit_3[1];
    }
    public static String decimalConvert(double price){

        NumberFormat formatter = NumberFormat.getNumberInstance();
        formatter.setMinimumFractionDigits(2);
        formatter.setMaximumFractionDigits(2);
        String output = formatter.format(price);

        return output;
    }

}
