package com.tangem.features.txhistory.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_copy_24
import com.tangem.core.ui.res.generated.icons.ic_globe_24
import com.tangem.core.ui.res.generated.icons.ic_share_android_24
import com.tangem.domain.express.models.ExpressExchangeStatus
import com.tangem.domain.express.models.ExpressOnrampStatus
import com.tangem.domain.models.network.TxInfo
import com.tangem.domain.models.network.TxInfo.TransactionType
import com.tangem.domain.txhistory.model.TxHistoryInfo
import com.tangem.features.txhistory.entity.TxHistoryDetailsUM
import com.tangem.features.txhistory.impl.R
import com.tangem.features.txhistory.model.TxHistoryLookupContext
import org.junit.jupiter.api.Test

/**
 * The dispatcher owns three things: routing each [TxHistoryInfo] shape to its sub-converter, building the shared header
 * menu once from the callbacks, and deriving the on-chain own-address set from the lookup. Per-shape conversion detail
 * is covered by [OnChainTxToDetailsUMConverterTest] / [ExpressTxToDetailsUMConverterTest].
 */
internal class TxHistoryInfoToTxHistoryDetailsUMConverterTest : TxDetailsConverterTestBase() {

    // region Routing

    @Test
    fun `GIVEN on-chain tx WHEN convert THEN SingleAsset`() {
        // Act
        val result = dispatcher().convert(onChain(type = TransactionType.Transfer))

        // Assert
        assertThat(result).isInstanceOf(TxHistoryDetailsUM.SingleAsset::class.java)
    }

    @Test
    fun `GIVEN express swap WHEN convert THEN TwoAssets`() {
        // Act
        val result = dispatcher().convert(expressSwap(status = ExpressExchangeStatus.Exchanging))

        // Assert
        assertThat(result).isInstanceOf(TxHistoryDetailsUM.TwoAssets::class.java)
    }

    @Test
    fun `GIVEN express onramp WHEN convert THEN TwoAssets`() {
        // Act
        val result = dispatcher().convert(expressOnramp(status = ExpressOnrampStatus.Finished))

        // Assert
        assertThat(result).isInstanceOf(TxHistoryDetailsUM.TwoAssets::class.java)
    }

    // endregion

    // region Header menu building

    @Test
    fun `GIVEN all menu callbacks WHEN convert THEN header menu has copy-id, share and explore rows wired`() {
        // Arrange
        var copiedTxId = false
        var shared = false
        var explored = false
        val converter = dispatcher(
            onCopyTxId = { copiedTxId = true },
            onShare = { shared = true },
            onExplore = { explored = true },
        )

        // Act
        val menu = converter.convert(onChain(type = TransactionType.Transfer)).header.menu

        // Assert
        assertThat(menu).hasSize(3)
        assertThat(menu[0].icon).isEqualTo(Icons.ic_copy_24)
        assertThat(menu[0].title).isEqualTo(resourceReference(R.string.common_transaction_id))
        assertThat(menu[1].icon).isEqualTo(Icons.ic_share_android_24)
        assertThat(menu[1].title).isEqualTo(resourceReference(R.string.common_share))
        assertThat(menu[2].icon).isEqualTo(Icons.ic_globe_24)
        assertThat(menu[2].title).isEqualTo(resourceReference(R.string.common_explore))

        menu[0].onClick()
        menu[1].onClick()
        menu[2].onClick()
        assertThat(copiedTxId).isTrue()
        assertThat(shared).isTrue()
        assertThat(explored).isTrue()
    }

    @Test
    fun `GIVEN no share and explore callbacks WHEN convert THEN header menu drops the share and explore rows`() {
        // Arrange — onShare/onExplore are null (e.g. an express op with no on-chain leg to share or open yet).
        val converter = dispatcher(onCopyTxId = {}, onShare = null, onExplore = null)

        // Act
        val menu = converter.convert(onChain(type = TransactionType.Transfer)).header.menu

        // Assert
        assertThat(menu).hasSize(1)
        assertThat(menu[0].title).isEqualTo(resourceReference(R.string.common_transaction_id))
    }

    @Test
    fun `GIVEN no menu callbacks WHEN convert THEN header menu is empty`() {
        // Arrange — every menu action is absent (e.g. a blank tx id with no on-chain leg to share or open).
        val converter = dispatcher(onCopyTxId = null, onShare = null, onExplore = null)

        // Act
        val menu = converter.convert(onChain(type = TransactionType.Transfer)).header.menu

        // Assert
        assertThat(menu).isEmpty()
    }

    @Test
    fun `GIVEN menu WHEN convert express swap THEN same menu is shared on the express header`() {
        // Arrange — the menu is built once and handed to both sub-converters.
        val converter = dispatcher(onCopyTxId = {})

        // Act
        val menu = converter.convert(expressSwap(status = ExpressExchangeStatus.Exchanging)).header.menu

        // Assert
        assertThat(menu).hasSize(1)
        assertThat(menu[0].title).isEqualTo(resourceReference(R.string.common_transaction_id))
    }

    // endregion

    // region Lookup -> own addresses threading

    @Test
    fun `GIVEN incoming Transfer from an address owned on the currency network WHEN convert THEN transferred title`() {
        // Arrange — the dispatcher derives the own-address set from lookup[currency.network], driving the on-chain title.
        val converter = dispatcher(
            lookup = lookupOf(currency.network.id.rawId to mapOf(USER_ADDRESS to ownAccount)),
        )
        val tx = onChain(
            type = TransactionType.Transfer,
            interactionAddressType = TxInfo.InteractionAddressType.User(USER_ADDRESS),
        )

        // Act
        val header = converter.convert(tx).header

        // Assert
        assertThat(header.title).isEqualTo(resourceReference(R.string.common_transferred))
    }

    @Test
    fun `GIVEN incoming Transfer from an address owned only on another network WHEN convert THEN received title`() {
        // Arrange — the address is owned, but on bitcoin, not the viewed ethereum currency, so it is not "own" here.
        val converter = dispatcher(
            lookup = lookupOf(bitcoin.network.id.rawId to mapOf(USER_ADDRESS to ownAccount)),
        )
        val tx = onChain(
            type = TransactionType.Transfer,
            interactionAddressType = TxInfo.InteractionAddressType.User(USER_ADDRESS),
        )

        // Act
        val header = converter.convert(tx).header

        // Assert
        assertThat(header.title).isEqualTo(resourceReference(R.string.common_received))
    }

    // endregion

    private fun dispatcher(
        onCopyTxId: (() -> Unit)? = null,
        onShare: (() -> Unit)? = null,
        onExplore: (() -> Unit)? = null,
        lookup: TxHistoryLookupContext = lookupOf(),
    ) = TxHistoryInfoToTxHistoryDetailsUMConverter(
        currency = currency,
        onCopyAddress = copiedAddresses::add,
        onGoToProvider = openedUrls::add,
        onCopyTxId = onCopyTxId,
        onShare = onShare,
        onExplore = onExplore,
        lookup = lookup,
    )
}