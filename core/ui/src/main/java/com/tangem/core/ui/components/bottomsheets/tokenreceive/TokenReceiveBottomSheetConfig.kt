package com.tangem.core.ui.components.bottomsheets.tokenreceive

import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent

class TokenReceiveBottomSheetConfig(
    val name: String,
    val symbol: String,
    val network: String,
    val addresses: List<AddressModel>,
) : TangemBottomSheetConfigContent