package com.tangem.datasource.api.stakekit.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.tangem.datasource.api.stakekit.models.response.model.action.StakingActionStatusDTO
import com.tangem.datasource.api.stakekit.models.response.model.action.StakingActionTypeDTO
import com.tangem.datasource.api.stakekit.models.response.model.transaction.StakingTransactionDTO
import org.joda.time.DateTime
import java.math.BigDecimal

@JsonClass(generateAdapter = true)
data class EnterActionResponse(
    @Json(name = "id")
    val id: String,
    @Json(name = "integrationId")
    val integrationId: String,
    @Json(name = "status")
    val status: StakingActionStatusDTO,
    @Json(name = "type")
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
)
