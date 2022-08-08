package com.tangem.tap.features.wallet.ui.images

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.constraintlayout.utils.widget.ImageFilterView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.tangem.blockchain.common.DerivationStyle
import com.tangem.tangem_sdk_new.extensions.dpToPx
import com.tangem.tap.common.extensions.getRoundIconRes
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

    private var isBlockchainIconVisible: Boolean
        get() = binding.ivBlockchain.isVisible
        set(value) = binding.ivBlockchain::isVisible.set(value)

    private var isBadgeVisible: Boolean
        get() = binding.badge.isVisible
        set(value) = binding.badge::isVisible.set(value)

    @DrawableRes
    private var blockchainIconRes: Int? = null
        set(value) {
            if (value != null && value != field && isBlockchainIconVisible) {
                binding.ivBlockchain.setImageResource(value)
                field = value
            }
        }

    init {
        minWidth = dpToPx(48f).roundToInt()
        minHeight = dpToPx(48f).roundToInt()
    }

    fun load(
        currency: Currency,
        derivationStyle: DerivationStyle?,
    ) {
        isBlockchainIconVisible = currency.isToken()
        isBadgeVisible = currency.isCustomCurrency(derivationStyle)
        blockchainIconRes = currency.blockchain.getRoundIconRes()

        CurrencyIconRequest(
            currencyImageView = currencyImageView,
            currencyTextView = currencyTextView,
            token = (currency as? Currency.Token)?.token,
            blockchain = currency.blockchain,
        ).load()
    }
}