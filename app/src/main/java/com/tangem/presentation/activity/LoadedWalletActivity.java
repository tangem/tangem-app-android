package com.tangem.presentation.activity;

import android.app.Activity;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.tangem.presentation.fragment.LoadedWalletActivityFragment;
import com.tangem.wallet.R;


public class LoadedWalletActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loaded_wallet);
        MainActivity.commonInit(getApplicationContext());

        if( getIntent().getExtras().containsKey(NfcAdapter.EXTRA_TAG) )
        {
            Tag tag=getIntent().getParcelableExtra(NfcAdapter.EXTRA_TAG);
            if (tag != null ) {
                LoadedWalletActivityFragment fragment=(LoadedWalletActivityFragment)(getSupportFragmentManager().findFragmentById(R.id.loaded_wallet_fragment));
                fragment.onTagDiscovered(tag);
            }
        }
    }

    @Override
    public void onBackPressed() {
        LoadedWalletActivityFragment loadedWalletActivityFragment=(LoadedWalletActivityFragment) getSupportFragmentManager().findFragmentById(R.id.loaded_wallet_fragment);
        Intent data= loadedWalletActivityFragment.prepareResultIntent();
        data.putExtra("modification", "update");
        setResult(Activity.RESULT_OK, data);
        finish();
    }
}
