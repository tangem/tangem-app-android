package com.tangem.presentation.activity

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.tangem.presentation.fragment.VerifyCard
import com.tangem.wallet.R

class VerifyCardActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verify_card)
        MainActivity.commonInit(applicationContext)
    }


    override fun onBackPressed() {
        super.onBackPressed()
        val verifyCard = supportFragmentManager.findFragmentById(R.id.verify_card_fragment) as VerifyCard
        val data = verifyCard.prepareResultIntent()
        data.putExtra("modification", "update")
        finish()
    }

}