package com.tangem.presentation.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.tangem.App
import com.tangem.di.Navigator
import com.tangem.domain.wallet.CoinData
import com.tangem.domain.wallet.TangemCard
import com.tangem.domain.wallet.TangemContext
import com.tangem.presentation.fragment.VerifyCard
import com.tangem.wallet.R
import javax.inject.Inject

class VerifyCardActivity : AppCompatActivity() {

    @Inject
    lateinit var navigator: Navigator

    companion object {
        fun callingIntent(context: Context, card: TangemCard, coinData: CoinData, message: String, error: String): Intent {
            val intent = Intent(context, VerifyCardActivity::class.java)
            intent.putExtra(TangemCard.EXTRA_UID, card.uid)
            intent.putExtra(TangemCard.EXTRA_CARD, card.asBundle)
            intent.putExtra(TangemContext.EXTRA_BLOCKCHAIN_DATA, coinData.asBundle())
            intent.putExtra("Message", message)
            intent.putExtra("Error", error)
            return intent
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verify_card)

        App.getNavigatorComponent().inject(this)

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