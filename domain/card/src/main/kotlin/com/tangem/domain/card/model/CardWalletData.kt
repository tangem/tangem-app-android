package com.tangem.domain.card.model

import com.tangem.common.card.WalletData

sealed interface CardWalletData {
    val data: WalletData?

    data class File(override val data: WalletData) : CardWalletData

    data class Legacy(override val data: WalletData) : CardWalletData

    data class Twin(override val data: WalletData, val twinData: TwinData) : CardWalletData

    object None : CardWalletData {
        override val data: WalletData? = null
    }
}
