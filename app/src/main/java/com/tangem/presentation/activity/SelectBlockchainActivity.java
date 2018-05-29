package com.tangem.presentation.activity;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.tangem.wallet.R;

public class SelectBlockchainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_blockchain);

//        Spinner spBlockchain = (Spinner) findViewById(R.id.spBlockchain);
//        ArrayAdapter<Blockchain> adapter = new ArrayAdapter<Blockchain>(this, android.R.layout.simple_spinner_item, Blockchain.values());
//        spBlockchain.setAdapter(adapter);
//        spBlockchain.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
//            @Override
//            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
//                Intent intent = new Intent();
//                intent.putExtra("blockchain", Blockchain.values()[position].toString());
//                setResult(RESULT_OK, intent);
//                finish();
//            }
//
//            @Override
//            public void onNothingSelected(AdapterView<?> parent) {
//
//            }
//        });
    }
}
