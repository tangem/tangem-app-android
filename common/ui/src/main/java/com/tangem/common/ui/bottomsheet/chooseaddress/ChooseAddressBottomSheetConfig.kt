package com.tangem.common.ui.bottomsheet.chooseaddress

import com.tangem.common.ui.bottomsheet.receive.AddressModel
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import kotlinx.collections.immutable.ImmutableList

class ChooseAddressBottomSheetConfig(
    val addressModels: ImmutableList<AddressModel>,
    val onClick: (AddressModel) -> Unit,
) : TangemBottomSheetConfigContent