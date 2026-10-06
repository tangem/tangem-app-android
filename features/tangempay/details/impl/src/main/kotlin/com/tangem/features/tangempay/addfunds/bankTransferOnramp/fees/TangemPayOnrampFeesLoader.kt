package com.tangem.features.tangempay.addfunds.bankTransferOnramp.fees

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.usecase.GetOnrampFeesUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

internal class TangemPayOnrampFeesLoader(
    private val getOnrampFeesUseCase: GetOnrampFeesUseCase,
    private val userWalletId: UserWalletId,
    private val scope: CoroutineScope,
) {

    val fees: StateFlow<TangemPayOnrampFeesUM>
        field = MutableStateFlow<TangemPayOnrampFeesUM>(TangemPayOnrampFeesUM.Loading)

    private val converter = TangemPayOnrampFeesUMConverter(onRetryClick = ::retry)

    fun load() {
        scope.launch {
            fees.value = converter.convert(getOnrampFeesUseCase(userWalletId = userWalletId))
        }
    }

    private fun retry() {
        if (fees.value !is TangemPayOnrampFeesUM.Error) return
        fees.value = TangemPayOnrampFeesUM.Loading
        load()
    }
}