package com.tangem.features.onramp.main.entity.factory

import com.google.common.truth.Truth.assertThat
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.onramp.model.OnrampCurrency
import com.tangem.features.onramp.main.entity.OnrampMainComponentUM
import com.tangem.features.onramp.main.entity.OnrampOffersBlockUM
import com.tangem.features.onramp.main.entity.OnrampSecondaryFieldErrorUM
import com.tangem.utils.Provider
import io.mockk.mockk
import kotlinx.collections.immutable.persistentListOf
import org.junit.jupiter.api.Test
import java.math.BigDecimal

/**
 * Covers [OnrampAmountStateFactory.getSecondaryFieldRestrictedErrorState], the state shown when every
 * provider is region-restricted. Unlike its sibling `getSecondaryFieldAmountErrorState`, it must NOT
 * clear the offers block — restricted offers stay on screen, rendered disabled.
 */
internal class OnrampAmountStateFactoryTest {

    private lateinit var currentState: OnrampMainComponentUM

    private val cryptoCurrency = MockCryptoCurrencyFactory().ethereum
    private val buttonStateFactory = OnrampAmountButtonUMStateFactory()

    private val stateFactory = OnrampStateFactory(
        currentStateProvider = Provider { currentState },
        onrampAmountButtonUMStateFactory = buttonStateFactory,
        cryptoCurrency = cryptoCurrency,
        onrampIntents = mockk(relaxed = true),
    )

    private val factory = OnrampAmountStateFactory(
        currentStateProvider = Provider { currentState },
        analyticsEventHandler = mockk(relaxed = true),
        onrampIntents = mockk(relaxed = true),
        onrampAmountButtonUMStateFactory = buttonStateFactory,
    )

    @Test
    fun `GIVEN initial loading state WHEN getSecondaryFieldRestrictedErrorState THEN state unchanged`() {
        // Arrange
        currentState = buildInitialLoading()

        // Act
        val result = factory.getSecondaryFieldRestrictedErrorState()

        // Assert
        assertThat(result).isEqualTo(currentState)
    }

    @Test
    fun `GIVEN empty amount field WHEN getSecondaryFieldRestrictedErrorState THEN state unchanged`() {
        // Arrange — no entered amount yet, so there is nothing to complain about
        currentState = buildContent(fiatAmount = null)

        // Act
        val result = factory.getSecondaryFieldRestrictedErrorState()

        // Assert
        assertThat(result).isEqualTo(currentState)
    }

    @Test
    fun `GIVEN entered amount WHEN getSecondaryFieldRestrictedErrorState THEN restriction error shown`() {
        // Arrange
        currentState = buildContent(fiatAmount = BigDecimal("100"))

        // Act
        val result = factory.getSecondaryFieldRestrictedErrorState() as OnrampMainComponentUM.Content

        // Assert
        assertThat(result.amountBlockState.secondaryFieldModel)
            .isInstanceOf(OnrampSecondaryFieldErrorUM.Error::class.java)
        assertThat(result.errorNotification).isNull()
        // The amount input stays usable — only the secondary field carries the message
        assertThat(result.amountBlockState.amountFieldModel.isError).isFalse()
    }

    @Test
    fun `GIVEN offers block still loading WHEN getSecondaryFieldRestrictedErrorState THEN block becomes empty`() {
        // Arrange — getOffersState drops emissions while the block is Loading, so it would stay stuck
        currentState = buildContent(fiatAmount = BigDecimal("100"), offersBlockState = OnrampOffersBlockUM.Loading)

        // Act
        val result = factory.getSecondaryFieldRestrictedErrorState() as OnrampMainComponentUM.Content

        // Assert
        assertThat(result.offersBlockState).isEqualTo(OnrampOffersBlockUM.Empty)
    }

    @Test
    fun `GIVEN loaded offers block WHEN getSecondaryFieldRestrictedErrorState THEN offers stay visible`() {
        // Arrange
        val loadedOffers = OnrampOffersBlockUM.Content(
            recentOffer = null,
            recommended = persistentListOf(),
            onrampAllOffersButtonConfig = null,
        )
        currentState = buildContent(fiatAmount = BigDecimal("100"), offersBlockState = loadedOffers)

        // Act
        val result = factory.getSecondaryFieldRestrictedErrorState() as OnrampMainComponentUM.Content

        // Assert — restricted offers are rendered disabled rather than removed
        assertThat(result.offersBlockState).isEqualTo(loadedOffers)
    }

    private fun buildInitialLoading(): OnrampMainComponentUM.InitialLoading =
        stateFactory.getInitialState(currency = "BTC", onClose = {}, openSettings = {})

    private fun buildContent(
        fiatAmount: BigDecimal?,
        offersBlockState: OnrampOffersBlockUM = OnrampOffersBlockUM.Empty,
    ): OnrampMainComponentUM.Content {
        currentState = buildInitialLoading()
        val content = stateFactory.getReadyState(currency = USD_CURRENCY, initialFiatAmount = fiatAmount)
        return content.copy(offersBlockState = offersBlockState)
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