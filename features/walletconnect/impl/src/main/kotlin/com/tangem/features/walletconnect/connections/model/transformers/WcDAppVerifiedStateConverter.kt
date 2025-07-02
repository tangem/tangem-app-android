package com.tangem.features.walletconnect.connections.model.transformers

import com.domain.blockaid.models.dapp.CheckDAppResult
import com.tangem.features.walletconnect.connections.entity.VerifiedDAppState
import com.tangem.utils.converter.Converter

internal class WcDAppVerifiedStateConverter(
    private val onVerifiedClick: (String) -> Unit,
) : Converter<Pair<CheckDAppResult, String>, VerifiedDAppState> {

    override fun convert(value: Pair<CheckDAppResult, String>): VerifiedDAppState {
        val (checkDAppResult, appName) = value
        return if (checkDAppResult == CheckDAppResult.SAFE) {
            VerifiedDAppState.Verified(
                onVerifiedClick = { onVerifiedClick.invoke(appName) },
            )
        } else {
            VerifiedDAppState.Unknown
        }
    }
}