package com.tangem.features.collectibles.impl.packopening.ui.preview

import com.tangem.core.res.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.collectibles.models.CollectibleRarity
import com.tangem.domain.collectibles.models.OpenedCollectiblePack
import com.tangem.features.collectibles.impl.packopening.model.converter.CollectiblesRewardCardConverter
import com.tangem.features.collectibles.impl.packopening.ui.state.CollectiblesPackOpeningUM
import kotlinx.collections.immutable.persistentListOf
import java.math.BigDecimal

internal object CollectiblesPackOpeningPreviewData {

    /** The design's sample pull (Figma `76:11506`). */
    private val pack = OpenedCollectiblePack(
        txId = "preview",
        nftAddress = "",
        imageFrontUrl = "https://nft-dev.collectorcrypt.com/front/2026090150C5275487",
        imageBackUrl = "https://nft-dev.collectorcrypt.com/back/2026090150C5275487",
        rarity = CollectibleRarity.UNCOMMON,
        gradingCompany = "PSA",
        gradingId = "00000000",
        year = "2024",
        set = "Series One",
        serialNumber = "216",
        insuredValue = BigDecimal("62"),
    )

    val content = CollectiblesPackOpeningUM.Content(
        onBackClick = {},
        onCloseClick = {},
        order = CollectiblesPackOpeningUM.OrderUM(
            rows = persistentListOf(
                CollectiblesPackOpeningUM.PriceRowUM(labelRef = stringReference("Elite pack x1"), value = "$50"),
                CollectiblesPackOpeningUM.PriceRowUM(
                    labelRef = resourceReference(R.string.collectibles_confirm_order_platform_fee),
                    value = "$1",
                ),
                CollectiblesPackOpeningUM.PriceRowUM(
                    labelRef = resourceReference(R.string.collectibles_confirm_order_total),
                    value = "51 USDC",
                    isEmphasised = true,
                    hasDivider = false,
                ),
            ),
            button = CollectiblesPackOpeningUM.ButtonUM(
                textRef = resourceReference(R.string.collectibles_confirm_order_approve_button),
                isEnabled = true,
            ),
        ),
        reveal = CollectiblesPackOpeningUM.RevealUM(
            card = CollectiblesRewardCardConverter.convert(pack = pack),
            onKeepCardClick = {},
            onSellBackClick = {},
        ),
        onApproveClick = {},
        isNavigationVisible = true,
    )

    val notEnoughFunds = content.copy(
        order = content.order.copy(
            button = CollectiblesPackOpeningUM.ButtonUM(
                textRef = resourceReference(R.string.collectibles_confirm_order_not_enough_funds_button),
                isEnabled = false,
            ),
        ),
    )

    val loading = CollectiblesPackOpeningUM.Loading(onBackClick = {}, onCloseClick = {})

    val error = CollectiblesPackOpeningUM.Error(
        onBackClick = {},
        onCloseClick = {},
        message = stringReference("Failed to load the pack.\nTap to reload"),
        onReloadClick = {},
    )
}