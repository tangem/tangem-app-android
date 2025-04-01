package com.tangem.common.ui.bottomsheet.receive

import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import kotlinx.collections.immutable.ImmutableList

data class TokenReceiveBottomSheetConfig(
    val name: String,
    val symbol: String,
    val network: String,
    val addresses: ImmutableList<AddressModel>,
    val showMemoDisclaimer: Boolean,
    val onCopyClick: (String) -> Unit,
    val onShareClick: (String) -> Unit,
) : TangemBottomSheetConfigContent