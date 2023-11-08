package com.tangem.core.ui.components.bottomsheets.chooseaddress

import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.bottomsheets.tokenreceive.AddressModel
import kotlinx.collections.immutable.ImmutableList

class ChooseAddressBottomSheetConfig(
    val addressModels: ImmutableList<AddressModel>,
    val onClick: (AddressModel) -> Unit,
) : TangemBottomSheetConfigContent