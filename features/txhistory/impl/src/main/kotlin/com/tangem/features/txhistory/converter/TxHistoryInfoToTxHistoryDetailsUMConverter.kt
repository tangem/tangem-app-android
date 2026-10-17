package com.tangem.features.txhistory.converter

import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.staking.model.StakingTarget
import com.tangem.domain.txhistory.model.ExpressTx
import com.tangem.domain.txhistory.model.OnChainTx
import com.tangem.domain.txhistory.model.TxHistoryInfo
import com.tangem.features.txhistory.entity.TxHistoryDetailsUM
import com.tangem.features.txhistory.impl.BuildConfig
import com.tangem.features.txhistory.impl.R
import com.tangem.features.txhistory.model.TxHistoryLookupContext
import com.tangem.utils.converter.Converter

/**
 * Converts a [TxHistoryInfo] row to a [TxHistoryDetailsUM] for the in-app transaction details card, dispatching to the
 * per-shape converters: an [OnChainTx.BSDK] renders as [TxHistoryDetailsUM.SingleAsset] (see
 * [OnChainTxToDetailsUMConverter]) while an [ExpressTx] (swap / onramp) renders as [TxHistoryDetailsUM.TwoAssets] (see
 * [ExpressTxToDetailsUMConverter]). The header overflow menu is built once here and shared by both; each shape then
 * prepends its own "Copy address" row for the counterparty it resolved.
 */
internal class TxHistoryInfoToTxHistoryDetailsUMConverter(
    currency: CryptoCurrency,
    onCopyAddress: (String) -> Unit,
    onGoToProvider: (String) -> Unit,
    isBalanceHidden: Boolean,
    onCopyTxId: (() -> Unit)? = null,
    copyIdTitle: TextReference = resourceReference(R.string.common_transaction_id),
    shareText: TextReference? = null,
    onShare: (String) -> Unit = {},
    onExplore: (() -> Unit)? = null,
    refundCurrency: CryptoCurrency? = null,
    onLearnMoreAboutRefundsClick: () -> Unit = {},
    onGoToRefundedTokenClick: (CryptoCurrency) -> Unit = {},
    lookup: TxHistoryLookupContext = TxHistoryLookupContext(
        ownAccountByNetwork = emptyMap(),
        isAccountsModeEnabled = false,
        walletInfoById = emptyMap(),
    ),
    targetsByAddress: Map<String, StakingTarget> = emptyMap(),
    onExploreProvider: (String) -> Unit = {},
    onAddContact: (() -> Unit)? = null,
    networkTokens: Set<CryptoCurrency.Token> = emptySet(),
    fiatConversion: FiatConversion = FiatConversion.NONE,
) : Converter<TxHistoryInfo, TxHistoryDetailsUM> {

    private val menu = buildDetailsMenu(
        onCopyTxId = onCopyTxId,
        copyIdTitle = copyIdTitle,
        shareText = shareText,
        onShare = onShare,
        onExplore = onExplore,
        onAddContact = onAddContact,
    )

    private val onChainConverter = OnChainTxToDetailsUMConverter(
        currency = currency,
        onCopyAddress = onCopyAddress,
        menu = menu,
        targetsByAddress = targetsByAddress,
        onExploreProvider = onExploreProvider,
        isBalanceHidden = isBalanceHidden,
        lookup = lookup,
        networkTokens = networkTokens,
        fiatConversion = fiatConversion,
    )

    private val expressConverter = ExpressTxToDetailsUMConverter(
        currency = currency,
        onGoToProvider = onGoToProvider,
        onCopyAddress = onCopyAddress,
        lookup = lookup,
        menu = menu,
        refundCurrency = refundCurrency,
        isBalanceHidden = isBalanceHidden,
        onLearnMoreAboutRefundsClick = onLearnMoreAboutRefundsClick,
        onGoToRefundedTokenClick = onGoToRefundedTokenClick,
    )

    override fun convert(value: TxHistoryInfo): TxHistoryDetailsUM {
        var um = when (value) {
            is OnChainTx.BSDK -> onChainConverter.convert(value.txInfo)
            // todo txHistory: build the details card for standalone TangemPay rows when TangemPay is wired in
            is OnChainTx.TangemPay -> TODO("TangemPay on-chain details rendering is not implemented yet")
            is ExpressTx -> expressConverter.convert(value)
        }
        if (isDebugMenuEnabled) {
            um = when (um) {
                is TxHistoryDetailsUM.SingleAsset -> um.copy(header = um.header.copy(debugModel = value))
                is TxHistoryDetailsUM.TwoAssets -> um.copy(header = um.header.copy(debugModel = value))
            }
        }
        return um
    }

    private companion object {
        private val isDebugMenuEnabled: Boolean =
            BuildConfig.BUILD_TYPE == "debug" || BuildConfig.BUILD_TYPE == "internal"
    }
}