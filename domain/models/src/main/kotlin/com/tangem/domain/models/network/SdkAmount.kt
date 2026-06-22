package com.tangem.domain.models.network

import com.tangem.domain.models.serialization.SerializedBigDecimal
import kotlinx.serialization.Serializable

/**
 * Domain mirror of the blockchain SDK `Amount`, kept [Serializable] so it can be carried inside the serializable
 * [TxInfo] graph (the SDK `Amount` is not serializable and pulls in blockchain-specific types).
 *
 * Holds a monetary value together with the metadata needed to display it. Compared to the SDK model it drops
 * `maxValue` (irrelevant outside of "send" flows) and keeps only the currency identity on [SdkAmountType].
 *
 * @property currencySymbol display symbol of the currency (e.g. `ETH`, `USDT`)
 * @property value          amount value; `null` when the value is unknown
 * @property decimals       number of decimals of the currency
 * @property type           kind of currency the amount is denominated in
 */
@Serializable
data class SdkAmount(
    val currencySymbol: String,
    val value: SerializedBigDecimal? = null,
    val decimals: Int,
    val type: SdkAmountType = SdkAmountType.Coin,
)

/** Kind of currency an [SdkAmount] is denominated in. Mirrors the SDK `AmountType`. */
@Serializable
sealed interface SdkAmountType {

    /** Native coin of the blockchain. */
    @Serializable
    data object Coin : SdkAmountType

    /** Native coin used as a reserve currency for fee calculation (e.g. Algorand). */
    @Serializable
    data object Reserve : SdkAmountType

    /** A resource that can be spent to pay the fee (e.g. Mana on Koinos). */
    @Serializable
    data class FeeResource(val name: String? = null) : SdkAmountType

    /**
     * A token of the blockchain.
     *
     * @property contractAddress token contract address
     * @property id              backend currency id, when known
     */
    @Serializable
    data class Token(val contractAddress: String, val id: String? = null) : SdkAmountType
}