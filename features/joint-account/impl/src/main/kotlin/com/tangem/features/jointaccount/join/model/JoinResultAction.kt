package com.tangem.features.jointaccount.join.model

internal sealed interface JoinResultAction {
    data object CloseFlow : JoinResultAction
    data object None : JoinResultAction
}