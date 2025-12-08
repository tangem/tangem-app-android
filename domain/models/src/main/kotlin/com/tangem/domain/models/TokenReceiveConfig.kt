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
    val type: TokenReceiveType = TokenReceiveType.Default,
)

@Serializable
data class ReceiveAddressModel(
    val nameService: NameService,
    val value: String,
) {
    enum class NameService {
        Default, Legacy, Ens
    }
}

@Serializable
data class TokenReceiveNotification(
    val title: Int,
    val subtitle: Int,
    val isYieldSupplyNotification: Boolean = false,
)

enum class Asset {
    Currency, NFT
}

@Serializable
sealed class TokenReceiveType {

    /**
     * Default setting.
     * TokenReceiveComponent will use [CryptoCurrency] to get token icon and name
     */
    data object Default : TokenReceiveType()

    /**
     * Custom setting.
     * TokenReceiveComponent will use custom icon and name
     */
    data class Custom(
        val tokenIconUrl: String,
        val tokenName: String,
        val fallbackTint: Int,
        val fallbackBackground: Int,
    ) : TokenReceiveType()
}