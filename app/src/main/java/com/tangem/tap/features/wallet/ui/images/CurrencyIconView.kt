package com.tangem.tap.features.wallet.ui.images

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.TextView
import androidx.constraintlayout.utils.widget.ImageFilterView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.tangem.blockchain.common.DerivationStyle
import com.tangem.tangem_sdk_new.extensions.dpToPx
import com.tangem.tap.features.wallet.models.Currency
import com.tangem.wallet.databinding.ViewCurrencyIconBinding
import kotlin.math.roundToInt

class CurrencyIconView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : ConstraintLayout(context, attrs, defStyleAttr) {
    private val binding = ViewCurrencyIconBinding.inflate(
        LayoutInflater.from(context),
        this,
    )

    private val currencyImageView: ImageFilterView
        get() = binding.ivCurrency

    private val currencyTextView: TextView
        get() = binding.tvTokenLetter

    private var isBlockchainBadgeVisible: Boolean
        get() = binding.ivBlockchainBadge.isVisible
        set(value) = binding.ivBlockchainBadge::isVisible.set(value)

    private var isCustomCurrencyBadgeVisible: Boolean
        get() = binding.customBadge.isVisible
        set(value) = binding.customBadge::isVisible.set(value)

    init {
        minWidth = dpToPx(48f).roundToInt()
        minHeight = dpToPx(48f).roundToInt()
    }

    fun load(
        currency: Currency,
        derivationStyle: DerivationStyle?,
    ) {
        isCustomCurrencyBadgeVisible = currency.isCustomCurrency(derivationStyle)

        CurrencyIconRequest(
            currencyImageView = currencyImageView,
            currencyTextView = currencyTextView,
            token = (currency as? Currency.Token)?.token,
            blockchain = currency.blockchain,
        ).load()

        if (currency.isToken()) {
            // load a blockchain icon into the blockchain badge
            isBlockchainBadgeVisible = true

            CurrencyIconRequest(
                currencyImageView = binding.ivBlockchainBadge,
                currencyTextView = null,
                token = null,
                blockchain = currency.blockchain,
            ).load()
        }
    }
}
