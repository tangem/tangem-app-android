package com.tangem.data.staking.converters.transaction

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.staking.NetworkType
import com.tangem.domain.models.staking.YieldToken
import com.tangem.domain.staking.model.stakekit.transaction.StakingGasEstimate
import com.tangem.domain.staking.model.stakekit.transaction.StakingTransaction
import com.tangem.domain.staking.model.stakekit.transaction.StakingTransactionStatus
import com.tangem.domain.staking.model.stakekit.transaction.StakingTransactionType
import com.tangem.grow.datasource.stakekit.models.response.model.NetworkTypeDTO
import com.tangem.grow.datasource.stakekit.models.response.model.TokenDTO
import com.tangem.grow.datasource.stakekit.models.response.model.transaction.StakingGasEstimateDTO
import com.tangem.grow.datasource.stakekit.models.response.model.transaction.StakingTransactionDTO
import com.tangem.grow.datasource.stakekit.models.response.model.transaction.StakingTransactionStatusDTO
import com.tangem.grow.datasource.stakekit.models.response.model.transaction.StakingTransactionTypeDTO
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class StakingTransactionConverterTest {

    private val converter = StakingTransactionConverter(
        transactionStatusConverter = StakingTransactionStatusConverter(),
        transactionTypeConverter = StakingTransactionTypeConverter(),
    )

    private val solTokenDTO = TokenDTO(
        name = "Solana",
        network = NetworkTypeDTO.SOLANA,
        symbol = "SOL",
        decimals = 9,
        address = null,
        coinGeckoId = "solana",
        logoURI = "https://logo/sol.png",
        isPoints = false,
    )
    private val solToken = YieldToken(
        name = "Solana",
        network = NetworkType.SOLANA,
        symbol = "SOL",
        decimals = 9,
        address = null,
        coinGeckoId = "solana",
        logoURI = "https://logo/sol.png",
        isPoints = false,
    )

    @Test
    fun `GIVEN transaction dto WHEN convert THEN all fields mapped`() {
        // Arrange
        val gasEstimate = StakingGasEstimateDTO(BigDecimal("0.000005"), solTokenDTO, "200000")
        val dto = transactionDTO(gasEstimate = gasEstimate).copy(
            hash = "0xhash",
            signedTransaction = "0xsigned",
            error = "error",
            ledgerHwAppId = "solana",
            isMessage = true,
        )

        // Act
        val actual = converter.convert(dto)

        // Assert
        val expected = StakingTransaction(
            id = "tx-1",
            network = NetworkType.SOLANA,
            status = StakingTransactionStatus.WAITING_FOR_SIGNATURE,
            type = StakingTransactionType.SPLIT,
            hash = "0xhash",
            signedTransaction = "0xsigned",
            unsignedTransaction = "0xunsigned",
            stepIndex = 1,
            error = "error",
            gasEstimate = StakingGasEstimate(BigDecimal("0.000005"), solToken, "200000"),
            stakeId = "stake-1",
            explorerUrl = "https://explorer/tx",
            ledgerHwAppId = "solana",
            isMessage = true,
        )
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `GIVEN transaction without gas estimate WHEN convert THEN gas estimate null`() {
        // Act
        val actual = converter.convert(transactionDTO())

        // Assert
        assertThat(actual.gasEstimate).isNull()
    }

    @Test
    fun `GIVEN unknown network WHEN convert THEN network UNKNOWN`() {
        // Act
        val actual = converter.convert(transactionDTO(network = NetworkTypeDTO.UNKNOWN))

        // Assert
        assertThat(actual.network).isEqualTo(NetworkType.UNKNOWN)
    }

    private fun transactionDTO(
        gasEstimate: StakingGasEstimateDTO? = null,
        network: NetworkTypeDTO = NetworkTypeDTO.SOLANA,
    ) = StakingTransactionDTO(
        id = "tx-1",
        network = network,
        status = StakingTransactionStatusDTO.WAITING_FOR_SIGNATURE,
        type = StakingTransactionTypeDTO.SPLIT,
        hash = null,
        signedTransaction = null,
        unsignedTransaction = "0xunsigned",
        stepIndex = 1,
        error = null,
        gasEstimate = gasEstimate,
        stakeId = "stake-1",
        explorerUrl = "https://explorer/tx",
        ledgerHwAppId = null,
        isMessage = false,
    )
}