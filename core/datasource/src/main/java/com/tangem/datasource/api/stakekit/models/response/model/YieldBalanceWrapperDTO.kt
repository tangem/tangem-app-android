package com.tangem.datasource.api.stakekit.models.response.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.tangem.datasource.api.stakekit.models.response.model.action.StakingActionTypeDTO
import org.joda.time.DateTime
import java.math.BigDecimal

@JsonClass(generateAdapter = true)
data class YieldBalanceWrapperDTO(
    @Json(name = "balances")
    val balances: List<BalanceDTO>,
    @Json(name = "integrationId")
    val integrationId: String?,
)

@JsonClass(generateAdapter = true)
data class BalanceDTO(
    @Json(name = "groupId")
    val groupId: String,
    @Json(name = "type")
    val type: BalanceTypeDTO,
    @Json(name = "amount")
    val amount: BigDecimal,
    @Json(name = "date")
    val date: DateTime?,
    @Json(name = "pricePerShare")
    val pricePerShare: BigDecimal,
    @Json(name = "pendingActions")
    val pendingActions: List<PendingAction>,
    @Json(name = "token")
    val tokenDTO: TokenDTO,
    @Json(name = "validatorAddress")
    val validatorAddress: String?,
    @Json(name = "validatorAddresses")
    val validatorAddresses: List<String>?,
    @Json(name = "providerId")
    val providerId: String?,
) {

    enum class BalanceTypeDTO {
        @Json(name = "available")
        AVAILABLE,

        @Json(name = "staked")
        STAKED,

        @Json(name = "unstaking")
        UNSTAKING,

        @Json(name = "unstaked")
        UNSTAKED,

        @Json(name = "preparing")
        PREPARING,

        @Json(name = "rewards")
        REWARDS,

        @Json(name = "locked")
        LOCKED,

        @Json(name = "unlocking")
        UNLOCKING,

        UNKNOWN,
    }

    @JsonClass(generateAdapter = true)
    data class PendingAction(
        @Json(name = "type")
        val type: StakingActionTypeDTO,
        @Json(name = "passthrough")
        val passthrough: String,
        @Json(name = "args")
        val args: PendingActionArgs?,
    ) {
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
            @JsonClass(generateAdapter = true)
            data class Amount(
                @Json(name = "required")
                val required: Boolean,
                @Json(name = "minimum")
                val minimum: BigDecimal?,
                @Json(name = "maximum")
                val maximum: BigDecimal?,
            )

            @JsonClass(generateAdapter = true)
            data class Duration(
                @Json(name = "required")
                val required: Boolean,
                @Json(name = "minimum")
                val minimum: Int?,
                @Json(name = "maximum")
                val maximum: Int?,
            )

            @JsonClass(generateAdapter = true)
            data class Nft(
                @Json(name = "baycId")
                val baycId: Required?,
                @Json(name = "maycId")
                val maycId: Required?,
                @Json(name = "bakcId")
                val bakcId: Required?,
            )

            @JsonClass(generateAdapter = true)
            data class TronResource(
                @Json(name = "required")
                val required: Boolean,
                @Json(name = "options")
                val options: List<String>,
            )
        }
    }

    @JsonClass(generateAdapter = true)
    data class Required(
        @Json(name = "required")
        val required: Boolean,
    )
}