@file:Suppress("BooleanPropertyNaming")

package com.tangem.grow.datasource.stakekit.models.response.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.tangem.domain.models.serialization.SerializedBigDecimal
import com.tangem.domain.models.serialization.SerializedIsoDateTime
import com.tangem.grow.datasource.stakekit.models.request.Address
import com.tangem.grow.datasource.stakekit.models.response.model.BalanceDTO.PendingAction.PendingActionArgs.Amount
import com.tangem.grow.datasource.stakekit.models.response.model.action.StakingActionTypeDTO
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@JsonClass(generateAdapter = true)
data class YieldBalanceWrapperDTO(
    @Json(name = "addresses")
    val addresses: Address,
    @Json(name = "balances")
    val balances: List<BalanceDTO>,
    @Json(name = "integrationId")
    val integrationId: String?,
)

@Serializable
@JsonClass(generateAdapter = true)
data class BalanceDTO(
    @Json(name = "groupId")
    val groupId: String,
    @Json(name = "type")
    val type: BalanceTypeDTO,
    @Json(name = "amount")
    val amount: SerializedBigDecimal,
    @Json(name = "date")
    val date: SerializedIsoDateTime?,
    @Json(name = "pricePerShare")
    val pricePerShare: SerializedBigDecimal,
    @Json(name = "pendingActions")
    val pendingActions: List<PendingAction>,
    @Json(name = "pendingActionConstraints")
    val pendingActionConstraints: List<PendingActionConstraints>?,
    @SerialName("token")
    @Json(name = "token")
    val tokenDTO: TokenDTO,
    @Json(name = "validatorAddress")
    val validatorAddress: String?,
    @Json(name = "validatorAddresses")
    val validatorAddresses: List<String>?,
    @Json(name = "providerId")
    val providerId: String?,
) {

    @Serializable
    @JsonClass(generateAdapter = false)
    enum class BalanceTypeDTO {
        @SerialName("available")
        @Json(name = "available")
        AVAILABLE,

        @SerialName("staked")
        @Json(name = "staked")
        STAKED,

        @SerialName("unstaking")
        @Json(name = "unstaking")
        UNSTAKING,

        @SerialName("unstaked")
        @Json(name = "unstaked")
        UNSTAKED,

        @SerialName("preparing")
        @Json(name = "preparing")
        PREPARING,

        @SerialName("rewards")
        @Json(name = "rewards")
        REWARDS,

        @SerialName("locked")
        @Json(name = "locked")
        LOCKED,

        @SerialName("unlocking")
        @Json(name = "unlocking")
        UNLOCKING,

        UNKNOWN,
    }

    @Serializable
    @JsonClass(generateAdapter = true)
    data class PendingAction(
        @Json(name = "type")
        val type: StakingActionTypeDTO,
        @Json(name = "passthrough")
        val passthrough: String,
        @Json(name = "args")
        val args: PendingActionArgs?,
    ) {
        @Serializable
        @JsonClass(generateAdapter = true)
        data class PendingActionArgs(
            @Json(name = "amount")
            val amount: Amount?,
            @Json(name = "duration")
            val duration: Duration?,
            @Json(name = "validatorAddress")
            val validatorAddress: Required?,
            @Json(name = "validatorAddresses")
            val validatorAddresses: Required?,
            @Json(name = "nfts")
            val nfts: List<Nft>?,
            @Json(name = "tronResource")
            val tronResource: TronResource?,
            @Json(name = "signatureVerification")
            val signatureVerification: Required?,
        ) {
            @Serializable
            @JsonClass(generateAdapter = true)
            data class Amount(
                @Json(name = "required")
                val required: Boolean = true,
                @Json(name = "minimum")
                val minimum: SerializedBigDecimal?,
                @Json(name = "maximum")
                val maximum: SerializedBigDecimal?,
            )

            @Serializable
            @JsonClass(generateAdapter = true)
            data class Duration(
                @Json(name = "required")
                val required: Boolean,
                @Json(name = "minimum")
                val minimum: Int?,
                @Json(name = "maximum")
                val maximum: Int?,
            )

            @Serializable
            @JsonClass(generateAdapter = true)
            data class Nft(
                @Json(name = "baycId")
                val baycId: Required?,
                @Json(name = "maycId")
                val maycId: Required?,
                @Json(name = "bakcId")
                val bakcId: Required?,
            )

            @Serializable
            @JsonClass(generateAdapter = true)
            data class TronResource(
                @Json(name = "required")
                val required: Boolean,
                @Json(name = "options")
                val options: List<String>,
            )
        }
    }

    @Serializable
    @JsonClass(generateAdapter = true)
    data class PendingActionConstraints(
        @Json(name = "type")
        val type: StakingActionTypeDTO,
        @SerialName("amount")
        @Json(name = "amount")
        val amountArg: Amount,
    )

    @Serializable
    @JsonClass(generateAdapter = true)
    data class Required(
        @Json(name = "required")
        val required: Boolean,
    )
}