package com.tangem.presentation.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.tangem.wallet.R;
import com.tangem.presentation.fragment.VerifyCardFragment;

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
        VerifyCardFragment verifyCardFragment = (VerifyCardFragment) getSupportFragmentManager().findFragmentById(R.id.verify_card_fragment);
        Intent data = verifyCardFragment.prepareResultIntent();
        data.putExtra("modification", "update");
        setResult(Activity.RESULT_OK, data);
        finish();
    }
}
