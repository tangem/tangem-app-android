package com.tangem.features.onramp.root

import com.google.common.truth.Truth.assertThat
import com.tangem.common.routing.AppRoute
import com.tangem.core.decompose.navigation.Route
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.onramp.model.OnrampSource
import com.tangem.test.core.ProvideTestModels
import io.mockk.mockk
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class OnrampSuccessStackTest {

    private val userWalletId = UserWalletId(stringValue = "0011")
    private val currency: CryptoCurrency = mockk(relaxed = true)
    private val tokenDetails = AppRoute.CurrencyDetails(userWalletId = userWalletId, currency = currency)
    private val buyCrypto = AppRoute.BuyCrypto(userWalletId = userWalletId)
    private val tokenOnramp = onramp(OnrampSource.TOKEN_DETAILS)
    private val bannerOnramp = onramp(OnrampSource.MARKETING_BANNER)
    private val buyOnramp = onramp(OnrampSource.ACTION_BUTTONS)
    private val success = AppRoute.OnrampSuccess(txId = TX_ID)

    @ParameterizedTest
    @ProvideTestModels
    fun withOnrampSuccess(model: WithOnrampSuccessModel) {
        // Act
        val actual = model.stack.withOnrampSuccess(TX_ID)

        // Assert
        assertThat(actual).containsExactlyElementsIn(model.expected).inOrder()
    }

    private fun provideTestModels() = listOf(
        WithOnrampSuccessModel(
            reason = "a single onramp flow",
            stack = listOf(AppRoute.Wallet, tokenDetails, tokenOnramp),
            expected = listOf(AppRoute.Wallet, tokenDetails, success),
        ),
        WithOnrampSuccessModel(
            reason = "a marketing banner onramp over another one",
            stack = listOf(AppRoute.Wallet, tokenDetails, tokenOnramp, bannerOnramp),
            expected = listOf(AppRoute.Wallet, tokenDetails, success),
        ),
        WithOnrampSuccessModel(
            reason = "a buy deep link onramp over another one",
            stack = listOf(AppRoute.Wallet, tokenDetails, tokenOnramp, buyCrypto, buyOnramp),
            expected = listOf(AppRoute.Wallet, tokenDetails, buyCrypto, success),
        ),
    )

    private fun onramp(source: OnrampSource) =
        AppRoute.Onramp(source = source, userWalletId = userWalletId, currency = currency)

    internal data class WithOnrampSuccessModel(
        val reason: String,
        val stack: List<Route>,
        val expected: List<Route>,
    ) {
        override fun toString(): String = reason
    }

    private companion object {
        const val TX_ID = "tx_id"
    }
}