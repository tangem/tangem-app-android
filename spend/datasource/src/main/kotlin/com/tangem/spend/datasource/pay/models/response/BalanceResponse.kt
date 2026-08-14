package com.tangem.spend.datasource.pay.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.math.BigDecimal

@JsonClass(generateAdapter = true)
data class BalanceResponse(
    @Json(name = "fiat") val fiat: FiatBalance?,
    @Json(name = "crypto") val crypto: CryptoBalance?,
    @Json(name = "available_for_withdrawal") val availableForWithdrawal: AvailableForWithdrawal?,
    @Json(name = "networks") val networks: List<NetworkResponse>? = null,
) {

    @JsonClass(generateAdapter = true)
    data class NetworkResponse(
        @Json(name = "name") val name: String,
        @Json(name = "is_testnet") val isTestnet: Boolean,
        @Json(name = "chain_id") val chainId: Long,
        @Json(name = "status") val status: String,
        @Json(name = "deposit_address") val depositAddress: String?,
        @Json(name = "tokens") val tokens: List<NetworkTokenResponse>,
    )

    @JsonClass(generateAdapter = true)
    data class NetworkTokenResponse(
        @Json(name = "token") val token: String,
        @Json(name = "token_contract_address") val tokenContractAddress: String,
        @Json(name = "available_for_withdrawal") val availableForWithdrawal: BigDecimal?,
    )
}

@JsonClass(generateAdapter = true)
data class FiatBalance(
    @Json(name = "currency") val currency: String,
    @Json(name = "available_balance") val availableBalance: BigDecimal,
    @Json(name = "credit_limit") val creditLimit: BigDecimal,
    @Json(name = "pending_charges") val pendingCharges: BigDecimal,
    @Json(name = "posted_charges") val postedCharges: BigDecimal,
    @Json(name = "balance_due") val balanceDue: BigDecimal,
)

@JsonClass(generateAdapter = true)
data class CryptoBalance(
    @Json(name = "id") val id: String,
    @Json(name = "chain_id") val chainId: Int,
    @Json(name = "deposit_address") val depositAddress: String?,
    @Json(name = "token_contract_address") val tokenContractAddress: String,
    @Json(name = "balance") val balance: BigDecimal,
)

@JsonClass(generateAdapter = true)
data class AvailableForWithdrawal(
    @Json(name = "amount") val amount: BigDecimal,
    @Json(name = "currency") val currency: String,
)