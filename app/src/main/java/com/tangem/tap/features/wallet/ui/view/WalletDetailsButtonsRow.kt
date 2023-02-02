package com.tangem.tap.features.wallet.ui.view

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.core.view.isVisible
import com.google.android.material.button.MaterialButton
import com.tangem.tap.common.extensions.hide
import com.tangem.tap.common.extensions.show
import com.tangem.tap.features.wallet.redux.CurrencyAction
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
    var onSwapClick: (() -> Unit)? = null
    var onSendClick: (() -> Unit)? = null

    init {
        orientation = HORIZONTAL

        with(binding) {
            btnBuy.setOnClickListener { onBuyClick?.invoke() }
            btnSell.setOnClickListener { onSellClick?.invoke() }
            btnSwap.setOnClickListener { onSwapClick?.invoke() }
            btnTrade.setOnClickListener { onTradeClick?.invoke() }
            btnSend.setOnClickListener { onSendClick?.invoke() }
        }
    }

    fun updateButtonsVisibility(
        actions: Set<CurrencyAction>,
        exchangeServiceFeatureOn: Boolean,
        sendAllowed: Boolean
    ) = with(binding) {
        containerActionButtons.isVisible = exchangeServiceFeatureOn

        when {
            actions.isEmpty() -> {
                containerActionButtons.hide()
            }
            actions.size == 1 -> {
                val action = actions.first()
                btnTrade.hide()
                btnBuy.show(action == CurrencyAction.Buy)
                btnSell.show(action == CurrencyAction.Sell)
                btnSwap.show(action == CurrencyAction.Swap)
            }
            else -> {
                btnBuy.hide()
                btnSell.hide()
                btnSwap.hide()
                btnTrade.show()
            }
        }

        if (containerActionButtons.isVisible) {
            btnSend.gravity = Gravity.START or Gravity.CENTER_VERTICAL
            btnSend.iconGravity = MaterialButton.ICON_GRAVITY_END
        } else {
            btnSend.gravity = Gravity.CENTER
            btnSend.iconGravity = MaterialButton.ICON_GRAVITY_TEXT_END
        }
        btnSend.isEnabled = sendAllowed
    }
}
