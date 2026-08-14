package com.tangem.feature.swap.model

import com.google.common.truth.Truth.assertThat
import com.tangem.feature.swap.domain.models.domain.ExchangeProviderType
import com.tangem.feature.swap.domain.models.ui.SwapState
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class SwapModelDeeplinkProviderTest : SwapModelTestBase() {

    @BeforeEach
    fun setUp() = setUpBase()

    @Test
    fun `GIVEN pending id present in providers WHEN resolve THEN returns matching provider`() {
        val model = createModel()
        val changelly = swapProvider(id = "changelly", type = ExchangeProviderType.CEX)
        val oneInch = swapProvider(id = "1inch", type = ExchangeProviderType.DEX)

        val result = model.resolveDeeplinkProvider(pendingId = "changelly", providers = listOf(oneInch, changelly))

        assertThat(result).isEqualTo(changelly)
        model.onDestroy()
    }

    @Test
    fun `GIVEN pending id absent WHEN resolve THEN returns null`() {
        val model = createModel()
        val oneInch = swapProvider(id = "1inch", type = ExchangeProviderType.DEX)

        val result = model.resolveDeeplinkProvider(pendingId = "changelly", providers = listOf(oneInch))

        assertThat(result).isNull()
        model.onDestroy()
    }

    @Test
    fun `GIVEN null pending id WHEN resolve THEN returns null`() {
        val model = createModel()
        val oneInch = swapProvider(id = "1inch", type = ExchangeProviderType.DEX)

        val result = model.resolveDeeplinkProvider(pendingId = null, providers = listOf(oneInch))

        assertThat(result).isNull()
        model.onDestroy()
    }

    @Test
    fun `GIVEN deeplink provider present with quotes WHEN override THEN returns that provider`() {
        val model = createModel(createParams(providerId = "changelly"))
        val changelly = swapProvider(id = "changelly", type = ExchangeProviderType.CEX)
        val oneInch = swapProvider(id = "1inch", type = ExchangeProviderType.DEX)
        val oneInchState = quotesLoadedState(oneInch)
        val loadedStates = mapOf(changelly to quotesLoadedState(changelly), oneInch to oneInchState)

        val result = model.applyDeeplinkProviderOverride(selected = oneInch to oneInchState, loadedStates = loadedStates)

        assertThat(result.first).isEqualTo(changelly)
        model.onDestroy()
    }

    @Test
    fun `GIVEN deeplink provider errored WHEN override THEN keeps default`() {
        val model = createModel(createParams(providerId = "changelly"))
        val changelly = swapProvider(id = "changelly", type = ExchangeProviderType.CEX)
        val oneInch = swapProvider(id = "1inch", type = ExchangeProviderType.DEX)
        val oneInchState = quotesLoadedState(oneInch)
        val loadedStates = mapOf(changelly to mockk<SwapState.SwapError>(), oneInch to oneInchState)

        val result = model.applyDeeplinkProviderOverride(selected = oneInch to oneInchState, loadedStates = loadedStates)

        assertThat(result.first).isEqualTo(oneInch)
        model.onDestroy()
    }

    @Test
    fun `GIVEN deeplink provider absent WHEN override THEN keeps default`() {
        val model = createModel(createParams(providerId = "changelly"))
        val oneInch = swapProvider(id = "1inch", type = ExchangeProviderType.DEX)
        val oneInchState = quotesLoadedState(oneInch)
        val loadedStates = mapOf(oneInch to oneInchState)

        val result = model.applyDeeplinkProviderOverride(selected = oneInch to oneInchState, loadedStates = loadedStates)

        assertThat(result.first).isEqualTo(oneInch)
        model.onDestroy()
    }

    @Test
    fun `GIVEN override applied once WHEN second load THEN keeps default`() {
        val model = createModel(createParams(providerId = "changelly"))
        val changelly = swapProvider(id = "changelly", type = ExchangeProviderType.CEX)
        val oneInch = swapProvider(id = "1inch", type = ExchangeProviderType.DEX)
        val oneInchState = quotesLoadedState(oneInch)
        val loadedStates = mapOf(changelly to quotesLoadedState(changelly), oneInch to oneInchState)

        val firstResult = model.applyDeeplinkProviderOverride(selected = oneInch to oneInchState, loadedStates = loadedStates)
        val secondResult = model.applyDeeplinkProviderOverride(selected = oneInch to oneInchState, loadedStates = loadedStates)

        assertThat(firstResult.first).isEqualTo(changelly)
        assertThat(secondResult.first).isEqualTo(oneInch)
        model.onDestroy()
    }

    @Test
    fun `GIVEN only empty states WHEN override THEN keeps default and does not consume pending`() {
        val model = createModel(createParams(providerId = "changelly"))
        val changelly = swapProvider(id = "changelly", type = ExchangeProviderType.CEX)
        val oneInch = swapProvider(id = "1inch", type = ExchangeProviderType.DEX)
        val emptyOneInchState = mockk<SwapState.EmptyAmountState>()
        val emptyStates = mapOf(
            changelly to mockk<SwapState.EmptyAmountState>(),
            oneInch to emptyOneInchState,
        )

        val firstResult = model.applyDeeplinkProviderOverride(
            selected = oneInch to emptyOneInchState,
            loadedStates = emptyStates,
        )

        assertThat(firstResult.first).isEqualTo(oneInch)

        val oneInchState = quotesLoadedState(oneInch)
        val loadedStates = mapOf(changelly to quotesLoadedState(changelly), oneInch to oneInchState)
        val secondResult = model.applyDeeplinkProviderOverride(selected = oneInch to oneInchState, loadedStates = loadedStates)

        assertThat(secondResult.first).isEqualTo(changelly)
        model.onDestroy()
    }

    @Test
    fun `GIVEN no deeplink provider WHEN override THEN keeps default`() {
        val model = createModel()
        val oneInch = swapProvider(id = "1inch", type = ExchangeProviderType.DEX)
        val oneInchState = quotesLoadedState(oneInch)
        val loadedStates = mapOf(oneInch to oneInchState)

        val result = model.applyDeeplinkProviderOverride(selected = oneInch to oneInchState, loadedStates = loadedStates)

        assertThat(result.first).isEqualTo(oneInch)
        model.onDestroy()
    }
}