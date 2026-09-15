package com.tangem.data.collectibles.account.entity

import com.tangem.domain.models.serialization.SerializedBigDecimal
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Everything a wallet's collectibles account persists, in one entry.
 *

 * device has no delegate of ours.
 *
 * Of the delegate only its keystore id is kept — the auth type is always `NoAuth`, and the public key is cheaper
 * to re-derive than to risk serving one whose keystore entry is already gone.
 */
@Serializable
internal data class CollectiblesAccountDTO(
    @SerialName("status") val status: CollectiblesAccountStatusValueDTO? = null,
    @SerialName("delegate_hot_wallet_id") val delegateHotWalletId: String? = null,
)

/**
 * On-disk representation of the collectibles account status, kept separate from the
 * [com.tangem.domain.models.account.CollectiblesAccountStatusValue] domain model.
 *
 * Every name here is pinned with [SerialName], so the file format survives a rename or a package move of the
 * domain type.
 *
 * Three things are deliberately absent: the status source, the fiat rates, which belong to the app's selected
 * currency, and the loading, not-onboarded and error states, so a momentary failure cannot be persisted and then
 * read back as the account's real state.
 */
@Serializable
internal sealed interface CollectiblesAccountStatusValueDTO {

    val balance: CollectiblesVaultBalanceDTO?

    /** The vault exists and answers to the delegate this device holds. */
    @Serializable
    @SerialName("onboarded")
    data class Onboarded(
        @SerialName("balance") override val balance: CollectiblesVaultBalanceDTO? = null,
    ) : CollectiblesAccountStatusValueDTO

    /** The vault exists but this device has not derived its owner key yet, so it has no address to read. */
    @Serializable
    @SerialName("missed_derivation")
    data object MissedDerivation : CollectiblesAccountStatusValueDTO {
        override val balance: CollectiblesVaultBalanceDTO? = null
    }

    /** The vault exists but answers to another key, so it has to be re-pointed before it can sign. */
    @Serializable
    @SerialName("update_required")
    data class UpdateRequired(
        @SerialName("balance") override val balance: CollectiblesVaultBalanceDTO? = null,
    ) : CollectiblesAccountStatusValueDTO
}

/**
 * What the vault held when it was last read. A `null` amount is impossible: an unread vault has no [balance].
 *
 * Of the token only the mint is kept — the name, the symbol and the decimals are constants of
 * [com.tangem.domain.collectibles.account.CollectiblesVaultCurrencyFactory]. So a cached amount renders on a cold
 * start without reading the program config first.
 */
@Serializable
internal data class CollectiblesVaultBalanceDTO(
    @SerialName("usdc_amount") val usdcAmount: SerializedBigDecimal,
    @SerialName("usdc_mint") val usdcMint: String,
)