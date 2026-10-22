package com.tangem.features.tangempay.components

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.account.BankCredentials
import com.tangem.domain.models.wallet.UserWalletId

interface TangemPayBankTransferOnrampDetailsComponent : ComposableContentComponent {

    data class Params(
        val userWalletId: UserWalletId,
        val bankCredentials: BankCredentials,
        val analytics: Analytics,
    )

    interface Analytics {
        fun onDetailsShown()
        fun onFieldCopied(field: RequisiteField)
        fun onShareClicked()
    }

    enum class RequisiteField {
        ACCOUNT_NUMBER,
        ROUTING_NUMBER,
        BENEFICIARY_NAME,
        BENEFICIARY_ADDRESS,
        BANK_NAME,
        BANK_ADDRESS,
    }

    interface Factory : ComponentFactory<Params, TangemPayBankTransferOnrampDetailsComponent>
}