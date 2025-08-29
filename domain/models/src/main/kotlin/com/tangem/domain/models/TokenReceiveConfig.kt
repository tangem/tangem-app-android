package com.tangem.domain.models

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.serialization.Serializable

@Serializable
data class TokenReceiveConfig(
    val shouldShowWarning: Boolean,
    val cryptoCurrency: CryptoCurrency,
    val userWalletId: UserWalletId,
    val showMemoDisclaimer: Boolean,
    val receiveAddress: List<ReceiveAddressModel>,
    val tokenReceiveNotification: List<TokenReceiveNotification> = emptyList(),
    val asset: Asset = Asset.Currency,
)

@Serializable
data class ReceiveAddressModel(
    val nameService: NameService,
    val value: String,
    val displayName: String,
) {
    enum class NameService {
        Default, Ens
    }
}

@Serializable
data class TokenReceiveNotification(
    val title: Int,
    val subtitle: Int,
)

enum class Asset {
    Currency, NFT
}