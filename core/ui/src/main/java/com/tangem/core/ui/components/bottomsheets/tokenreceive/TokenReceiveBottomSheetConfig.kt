package com.tangem.core.ui.components.bottomsheets.tokenreceive

import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import kotlinx.collections.immutable.ImmutableList

class TokenReceiveBottomSheetConfig(
    val name: String,
    val symbol: String,
    val network: String,
    val addresses: ImmutableList<AddressModel>,
    val onCopyClick: () -> Unit,
    val onShareClick: () -> Unit,
) : TangemBottomSheetConfigContent