package com.tangem.domain.managetokens.model

import com.tangem.domain.models.network.Network

sealed class ManageTokensUpdateAction {

    data class AddCurrency(
        val currencyId: ManagedCryptoCurrency.ID,
        val network: Network,
        val isSelected: Boolean,
    ) : ManageTokensUpdateAction()
}