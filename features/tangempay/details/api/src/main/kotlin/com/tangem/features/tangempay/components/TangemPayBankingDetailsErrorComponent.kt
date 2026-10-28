package com.tangem.features.tangempay.components

import arrow.core.Either
import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.domain.models.account.BankCredentials
import com.tangem.domain.visa.error.VisaApiError

interface TangemPayBankingDetailsErrorComponent : ComposableBottomSheetComponent {

    data class Params(
        val retry: suspend () -> Either<VisaApiError, BankCredentials>,
        val onDismiss: () -> Unit,
        val onContactSupport: () -> Unit,
        val onResolved: (BankCredentials) -> Unit,
    )

    interface Factory : ComponentFactory<Params, TangemPayBankingDetailsErrorComponent>
}