package com.tangem.features.feed.deeplink

import android.net.Uri
import arrow.core.left
import arrow.core.right
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.common.routing.deeplink.DeepLinkRequest
import com.tangem.common.routing.deeplink.DeeplinkConst.TOKEN_ID_KEY
import com.tangem.data.common.currency.getTokenIconUrlFromDefaultHost
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.error.SelectedAppCurrencyError
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.markets.GetTokenMarketInfoUseCase
import com.tangem.domain.markets.TokenMarketInfo
import com.tangem.domain.markets.TokenMarketParams
import com.tangem.domain.markets.TokenQuotes
import com.tangem.domain.models.currency.CryptoCurrency
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class MarketsTokenExchangesDeepLinkHandlerTest {

    private val appRouter: AppRouter = mockk(relaxed = true)
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase = mockk()
    private val getTokenMarketInfoUseCase: GetTokenMarketInfoUseCase = mockk()

    private val handler = MarketsTokenExchangesDeepLinkHandler(
        appRouter = appRouter,
        getSelectedAppCurrencyUseCase = getSelectedAppCurrencyUseCase,
        getTokenMarketInfoUseCase = getTokenMarketInfoUseCase,
    )

    private val tokenId = CryptoCurrency.RawID("bitcoin")
    private val appCurrency = AppCurrency(code = "EUR", name = "Euro", symbol = "€")
    private val tokenInfo = TokenMarketInfo(
        id = "bitcoin",
        name = "Bitcoin",
        symbol = "BTC",
        quotes = TokenQuotes(
            currentPrice = BigDecimal("100"),
            h24ChangePercent = BigDecimal("1"),
            weekChangePercent = BigDecimal("2"),
            monthChangePercent = BigDecimal("3"),
            m3ChangePercent = null,
            m6ChangePercent = null,
            yearChangePercent = null,
            allTimeChangePercent = null,
        ),
        networks = null,
        shortDescription = null,
        fullDescription = null,
        insights = null,
        metrics = null,
        securityData = null,
        links = null,
        pricePerformance = null,
        exchangesAmount = 42,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(appRouter, getSelectedAppCurrencyUseCase, getTokenMarketInfoUseCase)
        coEvery { getSelectedAppCurrencyUseCase.invokeSync() } returns appCurrency.right()
        coEvery { getTokenMarketInfoUseCase(appCurrency, tokenId, any()) } returns tokenInfo.right()
    }

    @Test
    fun `GIVEN token info loaded WHEN handled THEN token details open on the exchanges`() = runTest {
        // Act
        handle(queryParams = mapOf(TOKEN_ID_KEY to tokenId.value))

        // Assert
        verify(exactly = 1) { appRouter.push(createExpectedRoute(appCurrency), any()) }
    }

    @Test
    fun `GIVEN no selected app currency WHEN handled THEN token details open in the default currency`() = runTest {
        // Arrange
        coEvery { getSelectedAppCurrencyUseCase.invokeSync() } returns
            SelectedAppCurrencyError.NoAppCurrencySelected.left()
        coEvery { getTokenMarketInfoUseCase(AppCurrency.Default, tokenId, any()) } returns tokenInfo.right()

        // Act
        handle(queryParams = mapOf(TOKEN_ID_KEY to tokenId.value))

        // Assert
        verify(exactly = 1) { appRouter.push(createExpectedRoute(AppCurrency.Default), any()) }
    }

    @Test
    fun `GIVEN token info fails WHEN handled THEN markets open instead`() = runTest {
        // Arrange
        coEvery { getTokenMarketInfoUseCase(any(), any(), any()) } returns Unit.left()

        // Act
        handle(queryParams = mapOf(TOKEN_ID_KEY to tokenId.value))

        // Assert
        verify(exactly = 1) { appRouter.push(AppRoute.Markets(), any()) }
    }

    @Test
    fun `GIVEN no token id WHEN handled THEN markets open without loading the token`() = runTest {
        // Act
        handle(queryParams = emptyMap())

        // Assert
        verify(exactly = 1) { appRouter.push(AppRoute.Markets(), any()) }
        coVerify(exactly = 0) { getTokenMarketInfoUseCase(any(), any(), any()) }
    }

    @Test
    fun `GIVEN empty token id WHEN handled THEN markets open without loading the token`() = runTest {
        // Act
        handle(queryParams = mapOf(TOKEN_ID_KEY to ""))

        // Assert
        verify(exactly = 1) { appRouter.push(AppRoute.Markets(), any()) }
        coVerify(exactly = 0) { getTokenMarketInfoUseCase(any(), any(), any()) }
    }

    private fun TestScope.handle(queryParams: Map<String, String>) {
        handler.handle(
            DeepLinkRequest(uri = mockk<Uri>(), queryParams = queryParams, scope = this, isFromOnNewIntent = false),
        )
        advanceUntilIdle()
    }

    private fun createExpectedRoute(appCurrency: AppCurrency) = AppRoute.MarketsTokenDetails(
        token = TokenMarketParams(
            id = tokenId,
            name = "Bitcoin",
            symbol = "BTC",
            tokenQuotes = TokenMarketParams.Quotes(
                currentPrice = BigDecimal("100"),
                h24Percent = BigDecimal("1"),
                weekPercent = BigDecimal("2"),
                monthPercent = BigDecimal("3"),
            ),
            imageUrl = getTokenIconUrlFromDefaultHost(tokenId),
        ),
        appCurrency = appCurrency,
        shouldShowPortfolio = true,
        shouldOpenExchanges = true,
        exchangesCount = 42,
    )
}