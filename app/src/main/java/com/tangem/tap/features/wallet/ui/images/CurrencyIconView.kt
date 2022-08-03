package com.tangem.tap.features.wallet.ui.images

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.utils.widget.ImageFilterView
import com.google.android.material.card.MaterialCardView
import com.tangem.tangem_sdk_new.extensions.dpToPx
import com.tangem.wallet.databinding.ViewCurrencyIconBinding

class CurrencyIconView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : MaterialCardView(context, attrs, defStyleAttr) {
    private val binding = ViewCurrencyIconBinding.inflate(
        LayoutInflater.from(context),
        this
    )

    val imageView: ImageFilterView
        get() = binding.iv

    init {
        elevation = 0f
        cardElevation = 0f
        radius = dpToPx(6f)
    }
}