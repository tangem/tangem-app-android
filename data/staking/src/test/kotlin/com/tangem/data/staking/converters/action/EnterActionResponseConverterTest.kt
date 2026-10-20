package com.tangem.data.staking.converters.action

import com.google.common.truth.Truth.assertThat
import com.tangem.data.staking.converters.transaction.StakingTransactionConverter
import com.tangem.data.staking.converters.transaction.StakingTransactionStatusConverter
import com.tangem.data.staking.converters.transaction.StakingTransactionTypeConverter
import com.tangem.domain.models.staking.action.StakingActionType
import com.tangem.domain.staking.model.stakekit.action.StakingAction
import com.tangem.domain.staking.model.stakekit.action.StakingActionStatus
import com.tangem.grow.datasource.stakekit.models.response.ActionDTO
import com.tangem.grow.datasource.stakekit.models.response.model.NetworkTypeDTO
import com.tangem.grow.datasource.stakekit.models.response.model.action.StakingActionStatusDTO
import com.tangem.grow.datasource.stakekit.models.response.model.action.StakingActionTypeDTO
import com.tangem.grow.datasource.stakekit.models.response.model.transaction.StakingTransactionDTO
import com.tangem.grow.datasource.stakekit.models.response.model.transaction.StakingTransactionStatusDTO
import com.tangem.grow.datasource.stakekit.models.response.model.transaction.StakingTransactionTypeDTO
import com.tangem.test.mock.staking.MockStaking
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class EnterActionResponseConverterTest {

    private val transactionConverter = StakingTransactionConverter(
        transactionStatusConverter = StakingTransactionStatusConverter(),
        transactionTypeConverter = StakingTransactionTypeConverter(),
    )
    private val converter = EnterActionResponseConverter(
        actionStatusConverter = ActionStatusConverter(),
        transactionConverter = transactionConverter,
    )
    private val createdAt = DateTime(1_700_000_000_000L, DateTimeZone.UTC)

    @Test
    fun `GIVEN action dto WHEN convert THEN action and transactions mapped`() {
        // Act
        val actual = converter.convert(actionDTO())

        // Assert
        val expected = StakingAction(
            id = "action-1",
            integrationId = MockStaking.SOLANA_ID,
            status = StakingActionStatus.CREATED,
            type = StakingActionType.STAKE,
            currentStepIndex = 0,
            amount = BigDecimal("1.5"),
            validatorAddress = "validator-1",
            validatorAddresses = null,
            transactions = listOf(transactionConverter.convert(transactionDTO())),
            createdAt = createdAt,
        )
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `GIVEN action without transactions WHEN convert THEN transactions null`() {
        // Act
        val actual = converter.convert(actionDTO(transactions = null))

        // Assert
        assertThat(actual.transactions).isNull()
    }

    private fun actionDTO(transactions: List<StakingTransactionDTO>? = listOf(transactionDTO())) = ActionDTO(
        id = "action-1",
        integrationId = MockStaking.SOLANA_ID,
        status = StakingActionStatusDTO.CREATED,
        type = StakingActionTypeDTO.STAKE,
        currentStepIndex = 0,
        amount = BigDecimal("1.5"),
        validatorAddress = "validator-1",
        validatorAddresses = null,
        transactions = transactions,
        createdAt = createdAt,
    )

    private fun transactionDTO() = StakingTransactionDTO(
        id = "tx-1",
        network = NetworkTypeDTO.SOLANA,
        status = StakingTransactionStatusDTO.WAITING_FOR_SIGNATURE,
        type = StakingTransactionTypeDTO.SPLIT,
        hash = null,
        signedTransaction = null,
        unsignedTransaction = "0xunsigned",
        stepIndex = 1,
        error = null,
        gasEstimate = null,
        stakeId = "stake-1",
        explorerUrl = "https://explorer/tx",
        ledgerHwAppId = null,
        isMessage = false,
    )
}