package com.tangem.tap.features.wallet.ui.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.core.view.isVisible
import com.tangem.tap.common.extensions.hide
import com.tangem.wallet.databinding.ViewWalletDetailsButtonsRowBinding

internal class WalletDetailsButtonsRow @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {
    private val binding = ViewWalletDetailsButtonsRowBinding.inflate(
        LayoutInflater.from(context),
        this,
    )
    var onBuyClick: (() -> Unit)? = null
    var onSellClick: (() -> Unit)? = null
    var onTradeClick: (() -> Unit)? = null
    var onSendClick: (() -> Unit)? = null

    init {
        orientation = HORIZONTAL

        with(binding) {
            btnBuy.setOnClickListener { onBuyClick?.invoke() }
            btnSell.setOnClickListener { onSellClick?.invoke() }
            btnTrade.setOnClickListener { onTradeClick?.invoke() }
            btnSend.setOnClickListener { onSendClick?.invoke() }
        }
    }

    fun updateButtonsVisibility(exchangeServiceFeatureOn: Boolean, sendAllowed: Boolean) = with(binding) {
        containerExchangeButtons.isVisible = exchangeServiceFeatureOn
        btnBuy.hide()
        btnSell.hide()
        btnTrade.isVisible = exchangeServiceFeatureOn
        btnSend.isEnabled = sendAllowed
    }
}
