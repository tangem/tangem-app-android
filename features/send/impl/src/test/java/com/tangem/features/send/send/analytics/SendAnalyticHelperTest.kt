package com.tangem.features.send.send.analytics

import arrow.core.None
import arrow.core.some
import com.google.common.truth.Truth.assertThat
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.Basic
import com.tangem.domain.account.status.model.AccountCryptoCurrency
import com.tangem.domain.account.status.usecase.GetAccountCurrencyByAddressUseCase
import com.tangem.domain.models.account.Account
import com.tangem.features.send.api.subcomponents.destination.entity.DestinationUM
import com.tangem.features.send.api.subcomponents.feeSelector.entity.FeeNonce
import com.tangem.features.send.api.subcomponents.feeSelector.entity.FeeSelectorUM
import com.tangem.features.send.send.ui.state.SendUM
import com.tangem.test.mock.MockAccounts
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class SendAnalyticHelperTest {

    private val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxed = true)
    private val getAccountCurrencyByAddressUseCase: GetAccountCurrencyByAddressUseCase = mockk()

    private val helper = SendAnalyticHelper(
        analyticsEventHandler = analyticsEventHandler,
        getAccountCurrencyByAddressUseCase = getAccountCurrencyByAddressUseCase,
    )

    private val currency = MockCryptoCurrencyFactory().ethereum

    @BeforeEach
    fun resetMocks() {
        clearMocks(analyticsEventHandler, getAccountCurrencyByAddressUseCase)
        coEvery { getAccountCurrencyByAddressUseCase(DESTINATION) } returns None
    }

    @Test
    fun `GIVEN a send from a crypto account WHEN success sent THEN both events name the account type only`() =
        runTest {
            // Act
            helper.sendSuccessAnalytics(
                cryptoCurrency = currency,
                sendUM = createSendUM(),
                feeToken = currency,
                account = MockAccounts.createAccount(derivationIndex = 0),
            )

            // Assert
            val (screenOpened, transactionSent) = sentEvents()
            assertThat(screenOpened.params).containsEntry(AnalyticsParam.ACCOUNT_TYPE_FROM, "Crypto")
            assertThat(screenOpened.params).containsEntry(AnalyticsParam.FEE_TYPE, "Normal")
            assertThat(screenOpened.params).containsEntry(AnalyticsParam.NONCE, "True")
            assertThat(screenOpened.params.keys).containsNoneOf(
                AnalyticsParam.ACCOUNT_TYPE_TO,
                AnalyticsParam.MEMBERS_COUNT,
                AnalyticsParam.THRESHOLD,
            )
            assertThat(transactionSent.params).containsEntry(AnalyticsParam.ACCOUNT_TYPE, "Crypto")
            assertThat(transactionSent.params.keys).containsNoneOf(AnalyticsParam.MEMBERS_COUNT, AnalyticsParam.THRESHOLD)
        }

    @Test
    fun `GIVEN a request from a joint account WHEN success sent THEN the composition and a fixed fee are reported`() =
        runTest {
            // Act
            helper.sendSuccessAnalytics(
                cryptoCurrency = currency,
                sendUM = createSendUM(),
                feeToken = currency,
                account = MockAccounts.createJointAccount(),
                jointComposition = AnalyticsParam.JointComposition(membersCount = 3, threshold = 2),
            )

            // Assert
            val (screenOpened, transactionSent) = sentEvents()
            assertThat(screenOpened.params).containsAtLeast(
                AnalyticsParam.ACCOUNT_TYPE_FROM, "Joint",
                AnalyticsParam.MEMBERS_COUNT, "3",
                AnalyticsParam.THRESHOLD, "2",
                AnalyticsParam.FEE_TYPE, "Fixed",
                AnalyticsParam.NONCE, "False",
            )
            assertThat(screenOpened.params).doesNotContainKey(AnalyticsParam.ACCOUNT_DERIVATION_FROM)
            assertThat(transactionSent.params).containsAtLeast(
                AnalyticsParam.ACCOUNT_TYPE, "Joint",
                AnalyticsParam.MEMBERS_COUNT, "3",
                AnalyticsParam.THRESHOLD, "2",
                AnalyticsParam.FEE_TYPE, "Fixed",
            )
        }

    @Test
    fun `GIVEN the destination is an own joint account WHEN success sent THEN the destination account type is Joint`() =
        runTest {
            // Arrange
            val destinationAccount: AccountCryptoCurrency = mockk {
                every { account } returns MockAccounts.createJointAccount()
            }
            coEvery { getAccountCurrencyByAddressUseCase(DESTINATION) } returns destinationAccount.some()

            // Act
            helper.sendSuccessAnalytics(
                cryptoCurrency = currency,
                sendUM = createSendUM(),
                feeToken = currency,
                account = MockAccounts.createAccount(derivationIndex = 0),
            )

            // Assert
            val (screenOpened, _) = sentEvents()
            assertThat(screenOpened.params).containsEntry(AnalyticsParam.ACCOUNT_TYPE_TO, "Joint")
        }

    private fun sentEvents(): Pair<AnalyticsEvent, AnalyticsEvent> {
        val events = mutableListOf<AnalyticsEvent>()
        verify(exactly = 2) { analyticsEventHandler.send(capture(events)) }
        val screenOpened = events.single { it is SendAnalyticEvents.TransactionScreenOpened }
        val transactionSent = events.single { it is Basic.TransactionSent }
        return screenOpened to transactionSent
    }

    private fun createSendUM(): SendUM {
        val destination: DestinationUM.Content = mockk(relaxed = true) {
            every { addressTextField.actualAddress } returns DESTINATION
            every { addressTextField.isAddressEns } returns false
            every { memoTextField } returns null
        }
        val feeSelector: FeeSelectorUM.Content = mockk(relaxed = true) {
            every { toAnalyticType() } returns AnalyticsParam.FeeType.Normal
            every { feeNonce } returns mockk<FeeNonce.Nonce>()
        }
        return mockk(relaxed = true) {
            every { destinationUM } returns destination
            every { feeSelectorUM } returns feeSelector
        }
    }

    private companion object {
        const val DESTINATION = "0x6dbd0c1957dfe9d85f74b8fe3a355b268a39ec91"
    }
}