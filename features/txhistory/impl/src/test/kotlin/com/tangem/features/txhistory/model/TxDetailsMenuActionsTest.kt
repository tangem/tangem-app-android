package com.tangem.features.txhistory.model

import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.clipboard.ClipboardManager
import com.tangem.domain.models.network.TxInfo
import com.tangem.domain.txhistory.model.OnChainTx
import com.tangem.domain.txhistory.usecase.GetExplorerTransactionUrlUseCase
import com.tangem.features.txhistory.entity.TxHistoryDetailsUM
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class TxDetailsMenuActionsTest {

    private val clipboardManager: ClipboardManager = mockk(relaxed = true)
    private val uiMessageSender: UiMessageSender = mockk(relaxed = true)
    private val urlOpener: UrlOpener = mockk(relaxed = true)
    private val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxed = true)
    private val getExplorerTransactionUrlUseCase: GetExplorerTransactionUrlUseCase = mockk()
    private val currency = MockCryptoCurrencyFactory().ethereum

    private val menuActions = TxDetailsMenuActions(
        clipboardManager = clipboardManager,
        uiMessageSender = uiMessageSender,
        urlOpener = urlOpener,
        analyticsEventHandler = analyticsEventHandler,
        getExplorerTransactionUrlUseCase = getExplorerTransactionUrlUseCase,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(clipboardManager, uiMessageSender, urlOpener, analyticsEventHandler, getExplorerTransactionUrlUseCase)
    }

    @Test
    fun `GIVEN a tx with a hash WHEN menu built THEN it holds add contact, copy id and explore`() {
        // Arrange
        val onAddContact = {}

        // Act
        val menu = menuActions.menu(txInfo = onChainTx(), currency = currency, onAddContact = onAddContact)

        // Assert
        assertThat(menu).hasSize(3)
        assertThat((menu.first().action as TxHistoryDetailsUM.MenuItemUM.Action.Direct).onClick)
            .isSameInstanceAs(onAddContact)
    }

    @Test
    fun `GIVEN a tx without a hash WHEN menu built THEN it is empty`() {
        // Act
        val menu = menuActions.menu(txInfo = onChainTx(txHash = ""), currency = currency, onAddContact = null)

        // Assert
        assertThat(menu).isEmpty()
    }

    @Test
    fun `GIVEN a tx with a hash WHEN copy id clicked THEN the hash goes to the clipboard`() {
        // Arrange
        val copy = menuActions.copyTxIdAction(txInfo = onChainTx(), currency = currency)!!

        // Act
        copy()

        // Assert
        verify(exactly = 1) { clipboardManager.setText(text = TX_HASH, isSensitive = false) }
        verify(exactly = 1) { uiMessageSender.send(any()) }
    }

    @Test
    fun `GIVEN a tx with a hash WHEN explore clicked THEN the explorer url opens`() {
        // Arrange
        every { getExplorerTransactionUrlUseCase(txHash = TX_HASH, currency = currency) } returns EXPLORER_URL.right()
        val explore = menuActions.exploreAction(txInfo = onChainTx(), currency = currency)!!

        // Act
        explore()

        // Assert
        verify(exactly = 1) { urlOpener.openUrl(url = EXPLORER_URL) }
    }

    private fun onChainTx(txHash: String = TX_HASH) = OnChainTx.BSDK(
        TxInfo.Single(
            txHash = txHash,
            timestampInMillis = 0L,
            isOutgoing = true,
            destinationType = TxInfo.DestinationType.Single(addressType = TxInfo.AddressType.User(ADDRESS)),
            sourceType = TxInfo.SourceType.Single(address = ADDRESS),
            interactionAddressType = null,
            status = TxInfo.TransactionStatus.Confirmed,
            type = TxInfo.TransactionType.Transfer,
            amount = BigDecimal.ONE,
            fee = null,
        ),
    )

    private companion object {
        const val TX_HASH = "0xtxhash"
        const val ADDRESS = "0x1234567890abcdef1234"
        const val EXPLORER_URL = "https://etherscan.io/tx/0xtxhash"
    }
}