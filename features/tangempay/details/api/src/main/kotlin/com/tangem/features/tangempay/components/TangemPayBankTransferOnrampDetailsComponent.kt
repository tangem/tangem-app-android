package com.tangem.features.tangempay.components

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.tangempay.TangemPayProduct
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.model.BankCredentials

interface TangemPayBankTransferOnrampDetailsComponent : ComposableContentComponent {

    data class Params(
        val userWalletId: UserWalletId,
        val bankCredentials: List<BankCredentials>,
        val feeSource: FeeSource,
        val analytics: Analytics,
    )

    sealed interface FeeSource {
        data class Catalogue(val product: TangemPayProduct) : FeeSource
        data object Route : FeeSource
    }

    interface Analytics {
        fun onDetailsShown(credentials: BankCredentials)
        fun onFieldCopied(field: RequisiteField, credentials: BankCredentials)
        fun onShareClicked(credentials: BankCredentials)
    }

    enum class RequisiteField {
        ACCOUNT_NUMBER,
        ROUTING_NUMBER,
        CLABE,
        BENEFICIARY_NAME,
        BENEFICIARY_ADDRESS,
        BANK_NAME,
        BANK_ADDRESS,
    }

    interface Factory : ComponentFactory<Params, TangemPayBankTransferOnrampDetailsComponent>
}