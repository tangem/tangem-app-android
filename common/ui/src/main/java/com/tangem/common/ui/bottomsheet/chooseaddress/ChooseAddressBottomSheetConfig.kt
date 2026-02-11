package com.tangem.common.ui.bottomsheet.chooseaddress

import com.tangem.common.ui.bottomsheet.receive.AddressModel
import com.tangem.common.ui.bottomsheet.receive.mapToAddressModels
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.NetworkAddress
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

class ChooseAddressBottomSheetConfig(
    val addressModels: ImmutableList<AddressModel>,
    val onClick: (AddressModel) -> Unit,
) : TangemBottomSheetConfigContent {
    constructor(
        currency: CryptoCurrency,
        networkAddress: NetworkAddress,
        onClick: (AddressModel) -> Unit,
    ) : this(
        addressModels = networkAddress.availableAddresses
            .mapToAddressModels(cryptoCurrency = currency)
            .toImmutableList(),
        onClick = onClick,
    )
}