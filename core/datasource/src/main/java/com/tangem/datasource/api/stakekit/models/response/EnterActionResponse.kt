package com.tangem.datasource.api.stakekit.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.tangem.datasource.api.stakekit.models.response.model.StakingActionTypeDTO
import com.tangem.datasource.api.stakekit.models.response.model.StakingTransactionDTO
import org.joda.time.DateTime
import java.math.BigDecimal

@JsonClass(generateAdapter = true)
data class EnterActionResponse(
    @Json(name = "id")
    val id: String,
    @Json(name = "integrationId")
    val integrationId: Boolean,
    @Json(name = "status")
    val status: ActionStatusDTO,
    @Json(name = "page")
    val type: StakingActionTypeDTO,
    @Json(name = "currentStepIndex")
    val currentStepIndex: Int,
    @Json(name = "amount")
    val amount: BigDecimal,
    @Json(name = "validatorAddress")
    val validatorAddress: String?,
    @Json(name = "validatorAddresses")
    val validatorAddresses: List<String>?,
    @Json(name = "transactions")
    val transactions: List<StakingTransactionDTO>?,
    @Json(name = "createdAt")
    val createdAt: DateTime,

) {
    enum class ActionStatusDTO {
        @Json(name = "CANCELED")
        CANCELED,

        @Json(name = "CREATED")
        CREATED,

        @Json(name = "WAITING_FOR_NEXT")
        WAITING_FOR_NEXT,

        @Json(name = "PROCESSING")
        PROCESSING,

        @Json(name = "FAILED")
        FAILED,

        @Json(name = "SUCCESS")
        SUCCESS,

        UNKNOWN,
    }
}
