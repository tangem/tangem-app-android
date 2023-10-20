package com.tangem.core.ui.components.bottomsheets.chooseaddress

import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.bottomsheets.tokenreceive.AddressModel

class ChooseAddressBottomSheetConfig(
    val addressModels: List<AddressModel>,
    val onClick: (AddressModel) -> Unit,
) : TangemBottomSheetConfigContent