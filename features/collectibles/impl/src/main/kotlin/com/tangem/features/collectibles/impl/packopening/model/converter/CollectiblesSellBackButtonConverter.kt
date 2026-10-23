package com.tangem.features.collectibles.impl.packopening.model.converter

import com.tangem.core.res.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.collectibles.models.CollectibleBuybackOffer
import com.tangem.features.collectibles.impl.common.formatter.formatUsd
import com.tangem.features.collectibles.impl.packopening.model.BuybackState
import com.tangem.features.collectibles.impl.packopening.ui.state.CollectiblesPackOpeningUM.SellBackButtonUM
import com.tangem.utils.converter.Converter

internal object CollectiblesSellBackButtonConverter : Converter<BuybackState, SellBackButtonUM> {

    override fun convert(value: BuybackState): SellBackButtonUM = when (value) {
        BuybackState.Checking -> SellBackButtonUM(
            textRef = resourceReference(R.string.collectibles_open_pack_buyback_unavailable),
            isEnabled = true,
            isLoading = true,
        )
        BuybackState.Unavailable -> SellBackButtonUM(
            textRef = resourceReference(R.string.collectibles_open_pack_buyback_unavailable),
            isEnabled = false,
            isLoading = false,
        )
        is BuybackState.Available -> SellBackButtonUM(
            textRef = value.offer.toText(),
            isEnabled = true,
            isLoading = false,
        )
        is BuybackState.InProgress -> SellBackButtonUM(
            textRef = value.offer.toText(),
            isEnabled = true,
            isLoading = true,
        )
    }

    private fun CollectibleBuybackOffer.toText(): TextReference = resourceReference(
        id = R.string.collectibles_open_pack_sell_back_button,
        formatArgs = wrappedList(amount.formatUsd()),
    )
}