package com.tangem.features.nft.details.entity

import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.extensions.TextReference

internal data class NFTInfoBottomSheetConfig(
    val title: TextReference,
    val text: TextReference,
) : TangemBottomSheetConfigContent