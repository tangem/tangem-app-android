package com.tangem.data.collectibles.account.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.data.collectibles.account.entity.CollectiblesAccountStatusValueDTO
import com.tangem.data.collectibles.account.entity.CollectiblesVaultBalanceDTO
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.CollectiblesAccountStatusValue
import com.tangem.domain.models.account.CollectiblesVaultBalance
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CollectiblesAccountStatusValueDTOConverterTest {

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Convert {

        @ParameterizedTest
        @ProvideTestModels
        fun convert(model: ConvertModel) {
            // Act
            val actual = CollectiblesAccountStatusValueDTOConverter.convert(model.status)

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            ConvertModel(
                status = CollectiblesAccountStatusValue.Onboarded(source = StatusSource.ACTUAL),
                expected = CollectiblesAccountStatusValueDTO.Onboarded(balance = null),
            ),
            ConvertModel(
                status = CollectiblesAccountStatusValue.Onboarded(source = StatusSource.ACTUAL, vaultBalance = BALANCE),
                expected = CollectiblesAccountStatusValueDTO.Onboarded(balance = BALANCE_DTO),
            ),
            ConvertModel(
                status = CollectiblesAccountStatusValue.UpdateRequired(
                    source = StatusSource.ACTUAL,
                    vaultBalance = BALANCE,
                ),
                expected = CollectiblesAccountStatusValueDTO.UpdateRequired(balance = BALANCE_DTO),
            ),
            ConvertModel(
                status = CollectiblesAccountStatusValue.MissedDerivation,
                expected = CollectiblesAccountStatusValueDTO.MissedDerivation,
            ),
            ConvertModel(status = CollectiblesAccountStatusValue.Loading, expected = null),
            ConvertModel(status = CollectiblesAccountStatusValue.NotOnboarded, expected = null),
            ConvertModel(status = CollectiblesAccountStatusValue.Error.Unavailable, expected = null),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ConvertBack {

        @ParameterizedTest
        @ProvideTestModels
        fun convertBack(model: ConvertBackModel) {
            // Act
            val actual = CollectiblesAccountStatusValueDTOConverter.convertBack(model.stored)

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            ConvertBackModel(
                stored = CollectiblesAccountStatusValueDTO.Onboarded(balance = null),
                expected = CollectiblesAccountStatusValue.Onboarded(source = StatusSource.CACHE),
            ),
            ConvertBackModel(
                stored = CollectiblesAccountStatusValueDTO.Onboarded(balance = BALANCE_DTO),
                expected = CollectiblesAccountStatusValue.Onboarded(
                    source = StatusSource.CACHE,
                    vaultBalance = CollectiblesVaultBalance(usdcAmount = USDC),
                ),
            ),
            ConvertBackModel(
                stored = CollectiblesAccountStatusValueDTO.MissedDerivation,
                expected = CollectiblesAccountStatusValue.MissedDerivation,
            ),
            ConvertBackModel(
                stored = CollectiblesAccountStatusValueDTO.UpdateRequired(balance = BALANCE_DTO),
                expected = CollectiblesAccountStatusValue.UpdateRequired(
                    source = StatusSource.CACHE,
                    vaultBalance = CollectiblesVaultBalance(usdcAmount = USDC),
                ),
            ),
        )
    }

    internal data class ConvertModel(
        val status: CollectiblesAccountStatusValue,
        val expected: CollectiblesAccountStatusValueDTO?,
    )

    internal data class ConvertBackModel(
        val stored: CollectiblesAccountStatusValueDTO,
        val expected: CollectiblesAccountStatusValue,
    )

    private companion object {
        val USDC: BigDecimal = BigDecimal("20")

        // The rate is not part of the file format: it is dropped on the way in and absent on the way out
        val BALANCE = CollectiblesVaultBalance(usdcAmount = USDC, usdcFiatRate = BigDecimal.ONE)
        val BALANCE_DTO = CollectiblesVaultBalanceDTO(usdcAmount = USDC)
    }
}