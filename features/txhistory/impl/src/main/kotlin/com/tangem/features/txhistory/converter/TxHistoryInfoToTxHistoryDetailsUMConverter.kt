package com.tangem.features.txhistory.converter

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.staking.model.stakekit.Yield
import com.tangem.domain.txhistory.model.ExpressTx
import com.tangem.domain.txhistory.model.OnChainTx
import com.tangem.domain.txhistory.model.TxHistoryInfo
import com.tangem.features.txhistory.entity.TxHistoryDetailsUM
import com.tangem.features.txhistory.impl.BuildConfig
import com.tangem.features.txhistory.model.TxHistoryLookupContext
import com.tangem.utils.converter.Converter

/**
 * Converts a [TxHistoryInfo] row to a [TxHistoryDetailsUM] for the in-app transaction details card, dispatching to the
 * per-shape converters: an [OnChainTx.BSDK] renders as [TxHistoryDetailsUM.SingleAsset] (see
 * [OnChainTxToDetailsUMConverter]) while an [ExpressTx] (swap / onramp) renders as [TxHistoryDetailsUM.TwoAssets] (see
 * [ExpressTxToDetailsUMConverter]). The header overflow menu is built once here and shared by both.
 */
internal class TxHistoryInfoToTxHistoryDetailsUMConverter(
    currency: CryptoCurrency,
    onCopyAddress: (String) -> Unit,
    onGoToProvider: (String) -> Unit,
    onCopyTxId: (() -> Unit)? = null,
    onShare: (() -> Unit)? = null,
    onExplore: (() -> Unit)? = null,
    refundCurrency: CryptoCurrency? = null,
    onLearnMoreAboutRefundsClick: () -> Unit = {},
    onGoToRefundedTokenClick: (CryptoCurrency) -> Unit = {},
    lookup: TxHistoryLookupContext = TxHistoryLookupContext(
        ownAccountByNetwork = emptyMap(),
        isAccountsModeEnabled = false,
        walletInfoById = emptyMap(),
    ),
    validatorsByAddress: Map<String, Yield.Validator> = emptyMap(),
    onOpenValidator: (String) -> Unit = {},
) : Converter<TxHistoryInfo, TxHistoryDetailsUM> {

    private val menu = buildDetailsMenu(onCopyTxId, onShare, onExplore)

    private val onChainConverter = OnChainTxToDetailsUMConverter(
        currency = currency,
        onCopyAddress = onCopyAddress,
        menu = menu,
        validatorsByAddress = validatorsByAddress,
        onOpenValidator = onOpenValidator,
        lookup = lookup,
    )

    private val expressConverter = ExpressTxToDetailsUMConverter(
        onGoToProvider = onGoToProvider,
        lookup = lookup,
        menu = menu,
        refundCurrency = refundCurrency,
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