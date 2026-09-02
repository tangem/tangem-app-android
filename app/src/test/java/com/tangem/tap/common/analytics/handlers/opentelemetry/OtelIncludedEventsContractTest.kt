package com.tangem.tap.common.analytics.handlers.opentelemetry

import com.google.common.truth.Truth.assertThat
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.Basic
import com.tangem.core.analytics.models.OtelIncludedEvent
import com.tangem.core.analytics.models.event.OnboardingAnalyticsEvent
import com.tangem.data.markets.analytics.MarketsDataAnalyticsEvent
import com.tangem.domain.onramp.analytics.OnrampAnalyticsEvent
import com.tangem.domain.staking.analytics.StakingAnalyticsEvent
import com.tangem.domain.staking.model.stakekit.StakingError
import com.tangem.domain.walletconnect.WcAnalyticEvents
import com.tangem.domain.walletconnect.model.sdkcopy.WcAppMetaData
import com.tangem.domain.walletconnect.model.sdkcopy.WcSdkSessionRequest
import com.tangem.feature.swap.analytics.SwapEvents
import com.tangem.feature.swap.domain.models.domain.ExchangeProviderType
import com.tangem.feature.swap.domain.models.domain.SwapProvider
import com.tangem.feature.swap.domain.models.ui.FeeBucket
import com.tangem.feature.wallet.presentation.wallet.analytics.WalletScreenAnalyticsEvent
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

/**
 * Pins the allowlist contract of every [OtelIncludedEvent] reachable from the app module: each
 * allowed param must exist on the event (or be injected by a params interceptor), and every event
 * param that is not exported must be listed as deliberately dropped — so a renamed or newly added
 * param fails here instead of silently disappearing from the OTLP metric.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class OtelIncludedEventsContractTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN marked event WHEN params classified THEN allowed and dropped cover exactly the event params`(
        model: ContractModel,
    ) {
        // Arrange
        val event = model.event
        val allowedParams = (event as OtelIncludedEvent).otelMetric.allowedParams

        // Act
        val droppedParams = event.params.keys - allowedParams
        val interceptorParams = allowedParams - event.params.keys

        // Assert
        assertThat(droppedParams).isEqualTo(model.expectedDroppedParams)
        assertThat(interceptorParams).isEqualTo(model.expectedInterceptorParams)
        assertThat(event.otelParamOverrides).isEqualTo(model.expectedParamOverrides)
        assertThat(allowedParams).containsAtLeastElementsIn(event.otelParamOverrides.keys)
    }

    internal data class ContractModel(
        val event: AnalyticsEvent,
        val expectedDroppedParams: Set<String> = emptySet(),
        val expectedInterceptorParams: Set<String> = emptySet(),
        val expectedParamOverrides: Map<String, String> = emptyMap(),
    ) {
        init {
            check(event is OtelIncludedEvent) { "${event::class.simpleName} is not marked with OtelIncludedEvent" }
        }
    }

    private fun provideTestModels() = listOf(
        ContractModel(
            event = Basic.TransactionSent(
                sentFrom = AnalyticsParam.TxSentFrom.Send(
                    blockchain = "Ethereum",
                    token = "ETH",
                    feeType = AnalyticsParam.FeeType.Fixed,
                    feeToken = "ETH",
                    feeAssetType = AnalyticsParam.FeeAssetType.Coin,
                ),
                memoType = Basic.TransactionSent.MemoType.Empty,
                isCustomToken = false,
                isCustomFeeToken = false,
            ),
            expectedDroppedParams = setOf(AnalyticsParam.BLOCKCHAIN, AnalyticsParam.MEMO),
        ),
        ContractModel(
            event = Basic.TransactionSent(
                sentFrom = AnalyticsParam.TxSentFrom.Send(
                    blockchain = "Ethereum",
                    token = "WEIRDCOIN",
                    feeType = AnalyticsParam.FeeType.Fixed,
                    feeToken = "WEIRDCOIN",
                    feeAssetType = AnalyticsParam.FeeAssetType.Token,
                ),
                memoType = Basic.TransactionSent.MemoType.Empty,
                isCustomToken = true,
                isCustomFeeToken = true,
            ),
            expectedDroppedParams = setOf(AnalyticsParam.BLOCKCHAIN, AnalyticsParam.MEMO),
            expectedParamOverrides = mapOf(
                AnalyticsParam.TOKEN_PARAM to OtelIncludedEvent.CUSTOM_TOKEN,
                AnalyticsParam.FEE_TOKEN to OtelIncludedEvent.CUSTOM_TOKEN,
            ),
        ),
        ContractModel(
            event = WalletScreenAnalyticsEvent.MainScreen.ScreenOpened(
                hasMobileWallet = true,
                accountsCount = 1,
                isBackedUp = true,
                completedBackups = setOf(AnalyticsParam.BackupType.Manual),
                theme = "Dark",
                isImported = false,
                referralId = "referral",
                appCurrency = "USD",
            ),
            expectedDroppedParams = setOf(
                "Mobile Wallet",
                "Accounts Count",
                "App Theme",
                AnalyticsParam.WALLET_TYPE,
                "App Currency",
                AnalyticsParam.BACKUPED,
                AnalyticsParam.COMPLETED_BACKUPS,
                AnalyticsParam.REFERRAL,
                AnalyticsParam.REFERRAL_ID,
            ),
            expectedInterceptorParams = setOf(AnalyticsParam.PRODUCT_TYPE),
        ),
        ContractModel(
            event = SwapEvents.SwapInProgressScreen(
                provider = swapProvider,
                commission = FeeBucket.MARKET,
                sendBlockchain = "Ethereum",
                receiveBlockchain = "Polygon",
                sendToken = "ETH",
                receiveToken = "MATIC",
                feeToken = "ETH",
                feeAssetType = AnalyticsParam.FeeAssetType.Coin,
                fromDerivationIndex = 0,
                toDerivationIndex = 1,
                referralId = "referral",
            ),
            expectedDroppedParams = setOf(
                "Commission",
                AnalyticsParam.SEND_TOKEN,
                AnalyticsParam.RECEIVE_TOKEN,
                AnalyticsParam.SEND_BLOCKCHAIN,
                AnalyticsParam.RECEIVE_BLOCKCHAIN,
                AnalyticsParam.ACCOUNT_DERIVATION_FROM,
                AnalyticsParam.ACCOUNT_DERIVATION_TO,
                AnalyticsParam.FEE_TOKEN,
                AnalyticsParam.FEE_ASSET_TYPE,
                AnalyticsParam.REFERRAL,
                AnalyticsParam.REFERRAL_ID,
            ),
        ),
        ContractModel(
            event = SwapEvents.NoticeProviderError(
                sendToken = "ETH",
                receiveToken = "MATIC",
                provider = swapProvider,
                errorCode = 500,
                errorMessage = "error",
            ),
            expectedDroppedParams = setOf(
                AnalyticsParam.SEND_TOKEN,
                AnalyticsParam.RECEIVE_TOKEN,
                AnalyticsParam.ERROR_MESSAGE,
            ),
        ),
        ContractModel(
            event = OnrampAnalyticsEvent.SuccessScreenOpened(
                providerName = "Mercuryo",
                currency = "USD",
                tokenSymbol = "BTC",
                residence = "Germany",
                paymentMethod = "Card",
            ),
            expectedDroppedParams = setOf(AnalyticsParam.TOKEN_PARAM, AnalyticsParam.RESIDENCE, "Currency Type"),
        ),
        ContractModel(
            event = OnrampAnalyticsEvent.Errors(
                tokenSymbol = "BTC",
                errorCode = "500",
                providerName = "Mercuryo",
                paymentMethod = "Card",
            ),
        ),
        ContractModel(
            event = StakingAnalyticsEvent.TransactionError(errorCode = "42"),
            expectedInterceptorParams = setOf(AnalyticsParam.TOKEN_PARAM),
        ),
        ContractModel(
            event = StakingAnalyticsEvent.DomainError(StakingError.DomainError(message = "boom")),
            expectedDroppedParams = setOf(AnalyticsParam.ERROR_DESCRIPTION),
            expectedInterceptorParams = setOf(AnalyticsParam.TOKEN_PARAM),
        ),
        ContractModel(
            event = OnboardingAnalyticsEvent.CreateWallet.WalletCreatedSuccessfully(
                creationType = AnalyticsParam.WalletCreationType.PrivateKey,
                seedPhraseLength = 12,
                passPhraseState = AnalyticsParam.EmptyFull.Empty,
                referralId = "referral",
                source = AnalyticsParam.ScreensSources.Onboarding,
            ),
            expectedDroppedParams = setOf(
                "Creation Type",
                "Passphrase",
                "Seed Phrase Length",
                AnalyticsParam.SOURCE,
                AnalyticsParam.REFERRAL,
                AnalyticsParam.REFERRAL_ID,
            ),
            expectedInterceptorParams = setOf(AnalyticsParam.PRODUCT_TYPE),
        ),
        ContractModel(
            event = MarketsDataAnalyticsEvent.List.Error(
                errorType = MarketsDataAnalyticsEvent.Type.Http,
                errorCode = 500,
                errorMessage = "boom",
            ),
            expectedDroppedParams = setOf(AnalyticsParam.ERROR_TYPE, AnalyticsParam.ERROR_MESSAGE),
        ),
        ContractModel(
            event = MarketsDataAnalyticsEvent.Details.Error(
                request = MarketsDataAnalyticsEvent.Details.Error.Request.Chart,
                tokenSymbol = "BTC",
                errorType = MarketsDataAnalyticsEvent.Type.Http,
                errorCode = 500,
                errorMessage = "boom",
            ),
            expectedDroppedParams = setOf(
                AnalyticsParam.SOURCE,
                AnalyticsParam.TOKEN_PARAM,
                AnalyticsParam.ERROR_TYPE,
                AnalyticsParam.ERROR_MESSAGE,
            ),
        ),
        ContractModel(
            event = MarketsDataAnalyticsEvent.ChartNullValuesError(
                requestPath = "coins/history",
                errorType = MarketsDataAnalyticsEvent.Type.Custom,
                errorCode = null,
            ),
            expectedDroppedParams = setOf("Request path", AnalyticsParam.ERROR_TYPE, AnalyticsParam.ERROR_MESSAGE),
        ),
        ContractModel(
            event = WcAnalyticEvents.SignatureRequestFailed(
                rawRequest = WcSdkSessionRequest(
                    topic = "topic",
                    chainId = "eip155:1",
                    dAppMetaData = WcAppMetaData(
                        name = "DApp",
                        description = "description",
                        url = "https://dapp.example",
                        icons = emptyList(),
                        redirect = null,
                    ),
                    request = WcSdkSessionRequest.JSONRPCRequest(id = 1, method = "eth_sign", params = "{}"),
                ),
                network = MockCryptoCurrencyFactory().ethereum.network,
                errorCode = "code",
                errorMessage = "message",
                accountDerivation = 0,
            ),
            expectedDroppedParams = setOf(
                AnalyticsParam.DAPP_NAME,
                AnalyticsParam.DAPP_URL,
                AnalyticsParam.METHOD_NAME,
                AnalyticsParam.BLOCKCHAIN,
                AnalyticsParam.ERROR_CODE,
                AnalyticsParam.ERROR_DESCRIPTION,
                AnalyticsParam.ACCOUNT_DERIVATION,
            ),
        ),
    )

    private companion object {
        val swapProvider = SwapProvider(
            providerId = "provider-id",
            name = "ChangeNow",
            type = ExchangeProviderType.CEX,
            imageLarge = "",
            termsOfUse = null,
            privacyPolicy = null,
            slippage = null,
        )
    }
}