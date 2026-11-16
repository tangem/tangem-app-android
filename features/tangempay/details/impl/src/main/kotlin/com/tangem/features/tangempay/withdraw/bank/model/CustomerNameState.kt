package com.tangem.features.tangempay.withdraw.bank.model

internal sealed interface CustomerNameState {

    data object NotNeeded : CustomerNameState

    data object Loading : CustomerNameState

    data object Unreachable : CustomerNameState

    data class Loaded(val firstName: String, val lastName: String) : CustomerNameState
}