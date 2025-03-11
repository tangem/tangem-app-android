package com.tangem.domain.managetokens.model

import com.tangem.domain.tokens.model.Network

sealed class ManageTokensUpdateAction {

    data class AddCurrency(
        val currencyId: ManagedCryptoCurrency.ID,
        val network: Network,
        val isSelected: Boolean,
    ) : ManageTokensUpdateAction()
}