package com.tangem.domain.wallets.models

sealed interface HotWalletAdditionRestriction {

    data object LimitReached : HotWalletAdditionRestriction
}