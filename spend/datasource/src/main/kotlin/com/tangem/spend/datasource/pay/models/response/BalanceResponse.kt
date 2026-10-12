package com.tangem.spend.datasource.pay.models.response

import com.tangem.domain.models.serialization.SerializedBigDecimal
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BalanceResponse(
    @SerialName("fiat") val fiat: FiatBalance?,
    @SerialName("networks") val networks: List<NetworkResponse>? = null,
) {

    @Serializable
    data class NetworkResponse(
        @SerialName("name") val name: String,
        @SerialName("is_testnet") val isTestnet: Boolean,
        @SerialName("chain_id") val chainId: Long,
        @SerialName("status") val status: String,
        @SerialName("deposit_address") val depositAddress: String? = null,
        @SerialName("tokens") val tokens: List<NetworkTokenResponse>,
    )

    @Serializable
    data class NetworkTokenResponse(
        @SerialName("token") val token: String,
        @SerialName("token_contract_address") val tokenContractAddress: String? = null,
        @SerialName("available_for_withdrawal") val availableForWithdrawal: SerializedBigDecimal? = null,
    )
}

@Serializable
data class FiatBalance(
    @SerialName("currency") val currency: String,
    @SerialName("available_balance") val availableBalance: SerializedBigDecimal,
    @SerialName("credit_limit") val creditLimit: SerializedBigDecimal,
    @SerialName("pending_charges") val pendingCharges: SerializedBigDecimal,
    @SerialName("posted_charges") val postedCharges: SerializedBigDecimal,
    @SerialName("balance_due") val balanceDue: SerializedBigDecimal,
)