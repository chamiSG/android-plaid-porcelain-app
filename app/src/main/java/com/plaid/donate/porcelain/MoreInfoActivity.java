package com.plaid.donate.porcelain;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class MoreInfoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_more_info);

        Button moreCloseBtn = findViewById(R.id.more_close_btn);
        moreCloseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MoreInfoActivity.this, MainActivity.class);
                intent.putExtra("MORE_INFO", "MoreInfo");
                startActivity(intent);
                finish();
            }
        });
    }
}
