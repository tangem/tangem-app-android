package com.tangem.data.tangempay.util

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.tangempay.model.AccountFee
import com.tangem.domain.tangempay.model.AccountFeeGroupType
import com.tangem.domain.tangempay.model.FeeCalculationType
import com.tangem.spend.datasource.tangempay.models.response.FeesResponse
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class AccountFeeConverterTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN fee WHEN convert THEN the fixed part travels with the percentage`(model: FeeModel) {
        // Act
        val actual = AccountFeeConverter.convert(model.response)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    private fun provideTestModels() = listOf(
        FeeModel(
            response = response(calculationType = "PERCENTAGE", amount = "1", fixedAmount = "0.50"),
            expected = fee(calculationType = FeeCalculationType.PERCENTAGE, amount = "1", fixedAmount = "0.50"),
        ),
        FeeModel(
            response = response(calculationType = "PERCENTAGE", amount = "1", fixedAmount = null),
            expected = fee(calculationType = FeeCalculationType.PERCENTAGE, amount = "1", fixedAmount = null),
        ),
        FeeModel(
            response = response(calculationType = "FIXED", amount = "14.00", fixedAmount = null),
            expected = fee(calculationType = FeeCalculationType.FIXED, amount = "14.00", fixedAmount = null),
        ),
    )

    private fun response(calculationType: String, amount: String, fixedAmount: String?) = FeesResponse.Fee(
        type = "SPEI_OFFRAMP",
        group = "OFFRAMP",
        name = "SPEI",
        paymentMethodId = null,
        calculationType = calculationType,
        amount = BigDecimal(amount),
        fixedAmount = fixedAmount?.let(::BigDecimal),
        currency = "USD",
        description = "SPEI offramp fee",
    )

    private fun fee(calculationType: FeeCalculationType, amount: String, fixedAmount: String?) = AccountFee(
        type = "SPEI_OFFRAMP",
        name = "SPEI",
        paymentMethodId = null,
        group = AccountFeeGroupType.OFFRAMP,
        calculationType = calculationType,
        amount = BigDecimal(amount),
        currency = "USD",
        fixedAmount = fixedAmount?.let(::BigDecimal),
    )

    data class FeeModel(val response: FeesResponse.Fee, val expected: AccountFee?)
}