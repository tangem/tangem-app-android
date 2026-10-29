package com.tangem.common.ui.amountScreen.converters.field

import com.google.common.truth.Truth.assertThat
import com.tangem.common.ui.amountScreen.models.AmountFieldModel
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.common.ui.amountScreen.models.EnterAmountBoundary
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.tokens.model.Amount
import com.tangem.test.core.ProvideTestModels
import io.mockk.mockk
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class AmountFieldChangeTransformerTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN entered amount WHEN transform THEN primary button enabled only for positive amount within balance`(
        model: ButtonEnabledModel,
    ) {
        // Arrange
        val transformer = AmountFieldChangeTransformer(
            cryptoCurrencyStatus = mockk(relaxed = true),
            maxEnterAmount = EnterAmountBoundary(amount = BigDecimal.TEN, fiatRate = null),
            minimumTransactionAmount = null,
            value = model.value,
        )

        // Act
        val actual = transformer.transform(createAmountData())

        // Assert
        assertThat(actual.isPrimaryButtonEnabled).isEqualTo(model.isPrimaryButtonEnabled)
    }

    private fun provideTestModels() = listOf(
        ButtonEnabledModel(value = "-10", isPrimaryButtonEnabled = false),
        ButtonEnabledModel(value = "-1", isPrimaryButtonEnabled = false),
        ButtonEnabledModel(value = "0", isPrimaryButtonEnabled = false),
        ButtonEnabledModel(value = "5", isPrimaryButtonEnabled = true),
        ButtonEnabledModel(value = "11", isPrimaryButtonEnabled = false),
    )

    private fun createAmountData() = AmountState.Data(
        isPrimaryButtonEnabled = false,
        accountTitleUM = mockk(relaxed = true),
        availableBalanceCrypto = TextReference.EMPTY,
        availableBalanceFiat = TextReference.EMPTY,
        tokenName = TextReference.EMPTY,
        tokenIconState = mockk(relaxed = true),
        amountTextField = AmountFieldModel(
            value = "",
            onValueChange = {},
            keyboardOptions = mockk(relaxed = true),
            keyboardActions = mockk(relaxed = true),
            cryptoAmount = Amount(currencySymbol = "ETH", value = BigDecimal.ZERO, decimals = 18),
            fiatAmount = Amount(currencySymbol = AppCurrency.Default.code, value = null, decimals = 2),
            isFiatValue = false,
            fiatValue = "",
            isFiatUnavailable = true,
            isValuePasted = false,
            onValuePastedTriggerDismiss = {},
            isError = false,
            isWarning = false,
            error = TextReference.EMPTY,
        ),
        appCurrency = AppCurrency.Default,
    )

    data class ButtonEnabledModel(val value: String, val isPrimaryButtonEnabled: Boolean)
}