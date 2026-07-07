package com.tangem.features.onramp.main.entity.factory

import com.google.common.truth.Truth.assertThat
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.core.ui.ds.message.TangemMessageEffect
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.onramp.model.OnrampCurrency
import com.tangem.features.onramp.impl.R
import com.tangem.features.onramp.main.entity.OnrampAmountButtonUMState
import com.tangem.features.onramp.main.entity.OnrampMainComponentUM
import com.tangem.features.onramp.main.entity.OnrampOffersBlockUM
import com.tangem.features.onramp.main.entity.OnrampSecondaryFieldErrorUM
import com.tangem.utils.Provider
import io.mockk.mockk
import kotlinx.collections.immutable.persistentListOf
import org.junit.jupiter.api.Test

internal class OnrampStateFactoryTest {

    private lateinit var currentState: OnrampMainComponentUM

    private val cryptoCurrency = MockCryptoCurrencyFactory().ethereum

    private val factory = OnrampStateFactory(
        currentStateProvider = Provider { currentState },
        onrampAmountButtonUMStateFactory = OnrampAmountButtonUMStateFactory(),
        cryptoCurrency = cryptoCurrency,
        onrampIntents = mockk(relaxed = true),
    )

    @Test
    fun `GIVEN initial loading with error WHEN getBuyNotSupportedState THEN shows None message and clears error`() {
        // Arrange
        currentState = factory.getInitialState(currency = "BTC", onClose = {}, openSettings = {})
            .copy(errorNotification = mockk())

        // Act
        val result = factory.getBuyNotSupportedState()

        // Assert
        val message = result.buyNotSupportedMessage
        assertThat(message).isNotNull()
        assertThat(message!!.messageEffect).isEqualTo(TangemMessageEffect.None)
        assertThat(message.title).isEqualTo(
            resourceReference(
                id = R.string.onramp_token_is_not_supported_banner_title,
                formatArgs = wrappedList(cryptoCurrency.name),
            ),
        )
        assertThat(message.subtitle).isEqualTo(
            resourceReference(R.string.onramp_token_is_not_supported_banner_subtitle),
        )
        assertThat(result.errorNotification).isNull()
    }

    @Test
    fun `GIVEN content with errors WHEN getBuyNotSupportedState THEN message has priority and other errors hidden`() {
        // Arrange
        currentState = factory.getInitialState(currency = "BTC", onClose = {}, openSettings = {})
        val content = factory.getReadyState(currency = USD_CURRENCY) as OnrampMainComponentUM.Content
        currentState = content.copy(
            errorNotification = mockk(),
            offersBlockState = OnrampOffersBlockUM.Loading,
            onrampAmountButtonUMState = OnrampAmountButtonUMState.Loaded(persistentListOf()),
            amountBlockState = content.amountBlockState.copy(
                amountFieldModel = content.amountBlockState.amountFieldModel.copy(isError = true),
                secondaryFieldModel = OnrampSecondaryFieldErrorUM.Error(stringReference("error")),
            ),
        )

        // Act
        val result = factory.getBuyNotSupportedState() as OnrampMainComponentUM.Content

        // Assert
        assertThat(result.buyNotSupportedMessage).isNotNull()
        assertThat(result.errorNotification).isNull()
        assertThat(result.offersBlockState).isEqualTo(OnrampOffersBlockUM.Empty)
        assertThat(result.onrampAmountButtonUMState).isEqualTo(OnrampAmountButtonUMState.None)
        assertThat(result.amountBlockState.secondaryFieldModel).isEqualTo(OnrampSecondaryFieldErrorUM.Empty)
        // Amount input is locked (disabled via isError) — like the unsupported-country case.
        assertThat(result.amountBlockState.amountFieldModel.isError).isTrue()
    }

    private companion object {
        val USD_CURRENCY = OnrampCurrency(
            name = "US Dollar",
            code = "USD",
            image = null,
            precision = 2,
            unit = "$",
        )
    }
}