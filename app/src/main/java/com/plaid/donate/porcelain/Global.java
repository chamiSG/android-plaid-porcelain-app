package com.plaid.donate.porcelain;

import java.text.NumberFormat;

public class Global {

    public static final String BASE_URL = "http://198.12.255.51:8000";
    public static final int BASE_TRANSFER_MONEY = 5;

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

    public static String decimalConvert(double price){

        NumberFormat formatter = NumberFormat.getNumberInstance();
        formatter.setMinimumFractionDigits(2);
        formatter.setMaximumFractionDigits(2);

        return formatter.format(price);
    }

}
