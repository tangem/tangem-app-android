package com.tangem.features.tangempay.components

import arrow.core.Either
import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.domain.tangempay.error.TangemPayApiError

interface TangemPayBankingDetailsErrorComponent : ComposableBottomSheetComponent {

    data class Params(
        val retry: suspend () -> Either<TangemPayApiError, Unit>,
        val onDismiss: () -> Unit,
        val onContactSupport: () -> Unit,
    )

    interface Factory : ComponentFactory<Params, TangemPayBankingDetailsErrorComponent>
}