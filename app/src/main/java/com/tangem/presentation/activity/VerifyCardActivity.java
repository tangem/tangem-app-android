package com.tangem.presentation.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.tangem.wallet.R;
import com.tangem.presentation.fragment.VerifyCard;

public class VerifyCardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_card);

        MainActivity.commonInit(getApplicationContext());
    }

    @Override
    public void onBackPressed() {
        //super.onBackPressed();
        VerifyCard verifyCard = (VerifyCard) getSupportFragmentManager().findFragmentById(R.id.verify_card_fragment);
        Intent data = verifyCard.prepareResultIntent();
        data.putExtra("modification", "update");
        setResult(Activity.RESULT_OK, data);
        finish();
    }
}
