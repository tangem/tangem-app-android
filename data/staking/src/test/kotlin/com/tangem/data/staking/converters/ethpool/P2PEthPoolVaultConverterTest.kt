package com.tangem.data.staking.converters.ethpool

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.staking.model.ethpool.P2PEthPoolVault
import com.tangem.grow.datasource.ethpool.models.response.P2PEthPoolVaultDTO
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import org.junit.jupiter.api.TestInstance

internal class P2PEthPoolVaultConverterTest {

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Convert {

        @Test
        fun `GIVEN vault dto WHEN convert THEN percents scaled to fractions`() {
            // Act
            val actual = P2PEthPoolVaultConverter.convert(vaultDTO())

            // Assert
            val expected = P2PEthPoolVault(
                vaultAddress = "0xvault1",
                displayName = "Vault 1",
                apy = BigDecimal("0.03500000"),
                baseApy = BigDecimal("0.03000000"),
                capacity = BigDecimal("10000.50"),
                totalAssets = BigDecimal("5000"),
                feePercent = BigDecimal("10"),
                isPrivate = false,
                isGenesis = true,
                isSmoothingPool = false,
                isErc20 = true,
                tokenName = "osETH",
                tokenSymbol = "osETH",
                createdAt = 1_700_000_000L,
            )
            assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `GIVEN apy with many decimals WHEN convert THEN rounded half up to 8 digits`() {
            // Act
            val actual = P2PEthPoolVaultConverter.convert(vaultDTO(apy = BigDecimal("1.234567895")))

            // Assert
            assertThat(actual.apy).isEqualTo(BigDecimal("0.01234568"))
        }

        @Test
        fun `GIVEN private vault dto WHEN convert THEN private flag kept`() {
            // Act
            val actual = P2PEthPoolVaultConverter.convert(vaultDTO().copy(isPrivate = true))

            // Assert
            assertThat(actual.isPrivate).isTrue()
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ConvertFromEntity {

        @Test
        fun `GIVEN dto WHEN stored as entity and restored THEN equals live conversion`() {
            // Arrange
            val entity = P2PEthPoolVaultEntityConverter.convert(vaultDTO())

            // Act
            val actual = P2PEthPoolVaultConverter.convertFromEntity(entity)

            // Assert
            assertThat(actual).isEqualTo(P2PEthPoolVaultConverter.convert(vaultDTO()))
        }

        @Test
        fun `GIVEN private vault entity WHEN convertFromEntity THEN private flag kept`() {
            // Arrange
            val entity = P2PEthPoolVaultEntityConverter.convert(vaultDTO()).copy(isPrivate = true)

            // Act
            val actual = P2PEthPoolVaultConverter.convertFromEntity(entity)

            // Assert
            assertThat(actual.isPrivate).isTrue()
        }

        @Test
        fun `GIVEN dto WHEN converted to entity THEN numbers stored as plain strings`() {
            // Arrange
            val dto = vaultDTO(apy = BigDecimal("3.5"), capacity = BigDecimal("1E+3"))

            // Act
            val actual = P2PEthPoolVaultEntityConverter.convert(dto)

            // Assert
            assertThat(actual.apy).isEqualTo("3.5")
            assertThat(actual.baseApy).isEqualTo("3")
            assertThat(actual.capacity).isEqualTo("1000")
            assertThat(actual.totalAssets).isEqualTo("5000")
            assertThat(actual.feePercent).isEqualTo("10")
        }

        // Pins current behaviour: convertFromEntity is not total, a corrupt row breaks getPersistedVaultsFlow
        @Test
        fun `GIVEN non numeric stored apy WHEN convertFromEntity THEN throws NumberFormatException`() {
            // Arrange
            val entity = P2PEthPoolVaultEntityConverter.convert(vaultDTO()).copy(apy = "n/a")

            // Act
            val error = runCatching { P2PEthPoolVaultConverter.convertFromEntity(entity) }.exceptionOrNull()

            // Assert
            assertThat(error).isInstanceOf(NumberFormatException::class.java)
        }
    }

    private fun vaultDTO(
        vaultAddress: String = "0xvault1",
        apy: BigDecimal = BigDecimal("3.5"),
        baseApy: BigDecimal = BigDecimal("3"),
        capacity: BigDecimal = BigDecimal("10000.50"),
        totalAssets: BigDecimal = BigDecimal("5000"),
        feePercent: BigDecimal = BigDecimal("10"),
    ) = P2PEthPoolVaultDTO(
        vaultAddress = vaultAddress,
        displayName = "Vault 1",
        apy = apy,
        baseApy = baseApy,
        capacity = capacity,
        totalAssets = totalAssets,
        feePercent = feePercent,
        isPrivate = false,
        isGenesis = true,
        isSmoothingPool = false,
        isErc20 = true,
        tokenName = "osETH",
        tokenSymbol = "osETH",
        createdAt = 1_700_000_000L,
    )
}