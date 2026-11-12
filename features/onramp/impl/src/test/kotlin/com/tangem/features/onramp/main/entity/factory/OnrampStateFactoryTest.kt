package com.tangem.features.onramp.main.entity.factory

import com.google.common.truth.Truth.assertThat
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.core.ui.ds.message.TangemMessageEffect
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.blockchain.common.Blockchain
import com.tangem.domain.models.currency.CryptoCurrency
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

    @Test
    fun `GIVEN initial loading WHEN getCountryFlagState THEN flag set and button state kept`() {
        // Arrange
        currentState = factory.getInitialState(
            currency = "BTC",
            onClose = {},
            openSettings = {},
            isCountryByIpEnabled = true,
        )

        // Act
        val result = factory.getCountryFlagState(
            state = currentState,
            flagUrl = "https://flag.png",
            countryName = "Country",
        )

        // Assert
        assertThat(result.topBarConfig.countryButtonUM?.flagUrl).isEqualTo("https://flag.png")
        assertThat(result.topBarConfig.countryButtonUM?.isEnabled).isFalse()
    }

    @Test
    fun `GIVEN ready state WHEN getCountryNotSupportedState THEN country warning shown and input locked`() {
        // Arrange
        currentState = factory.getInitialState(
            currency = "BTC",
            onClose = {},
            openSettings = {},
            isCountryByIpEnabled = true,
        )
        currentState = factory.getReadyState(currency = USD_CURRENCY)

        // Act
        val result = factory.getCountryNotSupportedState() as OnrampMainComponentUM.Content

        // Assert
        assertThat(result.buyNotSupportedMessage?.id).isEqualTo("country_not_supported")
        assertThat(result.buyNotSupportedMessage?.title)
            .isEqualTo(resourceReference(R.string.onramp_residency_bottomsheet_country_not_supported))
        assertThat(result.onrampAmountButtonUMState).isEqualTo(OnrampAmountButtonUMState.None)
        assertThat(result.amountBlockState.amountFieldModel.isError).isTrue()
        assertThat(result.topBarConfig.countryButtonUM?.isEnabled).isTrue()
    }

    @Test
    fun `GIVEN token with standard WHEN getInitialState THEN subtitle has standard and network name`() {
        // Arrange
        val token = MockCryptoCurrencyFactory().createToken(Blockchain.Ethereum)

        // Act
        val result = createFactory(token).getInitialState(currency = "USDC", onClose = {}, openSettings = {})

        // Assert
        assertThat(result.topBarConfig.subtitle).isEqualTo(
            resourceReference(
                id = R.string.token_details_toolbar_subtitle_standard,
                formatArgs = wrappedList("ERC20", token.network.name),
            ),
        )
    }

    @Test
    fun `GIVEN token without standard WHEN getInitialState THEN subtitle has network name only`() {
        // Arrange
        val token = MockCryptoCurrencyFactory().createToken(Blockchain.Solana)

        // Act
        val result = createFactory(token).getInitialState(currency = "USDC", onClose = {}, openSettings = {})

        // Assert
        assertThat(result.topBarConfig.subtitle).isEqualTo(
            resourceReference(
                id = R.string.token_details_toolbar_subtitle_network,
                formatArgs = wrappedList(token.network.name),
            ),
        )
    }

    @Test
    fun `GIVEN coin WHEN getInitialState THEN subtitle is main network`() {
        // Arrange
        val coin: CryptoCurrency = MockCryptoCurrencyFactory().ethereum

        // Act
        val result = createFactory(coin).getInitialState(currency = "ETH", onClose = {}, openSettings = {})

        // Assert
        assertThat(result.topBarConfig.subtitle).isEqualTo(resourceReference(R.string.common_main_network))
    }

    private fun createFactory(currency: CryptoCurrency) = OnrampStateFactory(
        currentStateProvider = Provider { currentState },
        onrampAmountButtonUMStateFactory = OnrampAmountButtonUMStateFactory(),
        cryptoCurrency = currency,
        onrampIntents = mockk(relaxed = true),
    )

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