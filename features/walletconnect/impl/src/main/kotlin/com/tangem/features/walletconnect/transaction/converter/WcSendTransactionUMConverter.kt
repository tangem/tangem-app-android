package com.tangem.features.walletconnect.transaction.converter

import com.tangem.common.ui.account.AccountTitleUM
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.isHotWallet
import com.tangem.domain.walletconnect.model.WcBitcoinMethod
import com.tangem.domain.walletconnect.model.WcEthMethod
import com.tangem.domain.walletconnect.model.WcMethod
import com.tangem.domain.walletconnect.model.WcPsbtOutput
import com.tangem.domain.walletconnect.model.WcSolanaMethod
import com.tangem.domain.walletconnect.usecase.method.BlockAidTransactionCheck
import com.tangem.domain.walletconnect.usecase.method.WcMethodContext
import com.tangem.domain.walletconnect.usecase.method.WcSignState
import com.tangem.domain.walletconnect.usecase.method.WcSignStep
import com.tangem.features.send.api.entity.FeeSelectorUM
import com.tangem.features.walletconnect.transaction.entity.blockaid.WcSendReceiveTransactionCheckResultsUM
import com.tangem.features.walletconnect.transaction.entity.common.WcTransactionActionsUM
import com.tangem.features.walletconnect.transaction.entity.common.WcTransactionFeeState
import com.tangem.features.walletconnect.transaction.entity.common.WcTransactionRequestBlockUM
import com.tangem.features.walletconnect.transaction.entity.common.WcTransactionRequestInfoUM
import com.tangem.features.walletconnect.transaction.entity.send.WcSendTransactionItemUM
import com.tangem.features.walletconnect.transaction.entity.send.WcSendTransactionUM
import com.tangem.features.walletconnect.utils.WcNotificationsFactory
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import javax.inject.Inject

internal class WcSendTransactionUMConverter @Inject constructor(
    private val appInfoContentUMConverter: WcTransactionAppInfoContentUMConverter,
    private val networkInfoUMConverter: WcNetworkInfoUMConverter,
    private val requestBlockUMConverter: WcTransactionRequestBlockUMConverter,
    private val btcSendTransferRequestInfoConverter: WcBtcSendTransferRequestInfoConverter,
    private val signPsbtRequestInfoConverter: WcSignPsbtRequestInfoConverter,
    private val notificationsFactory: WcNotificationsFactory,
) : Converter<WcSendTransactionUMConverter.Input, WcSendTransactionUM?> {

    override fun convert(value: Input): WcSendTransactionUM? {
        val feeErrorNotification = notificationsFactory.createFeeNotifications(
            cryptoCurrencyStatus = value.cryptoCurrencyStatus,
            feeSelectorUM = value.feeSelectorUM,
            onFeeReload = value.onFeeReload,
        )
        return when (value.context.method) {
            is WcEthMethod.SendTransaction,
            is WcEthMethod.SignTransaction,
            is WcSolanaMethod.SignAllTransaction,
            is WcSolanaMethod.SignTransaction,
            is WcBitcoinMethod.SendTransfer,
            is WcBitcoinMethod.SignPsbt,
            is WcSolanaMethod.SignAndSendTransaction,
            is WcBitcoinMethod.SignMessage,
            -> WcSendTransactionUM(
                transaction = WcSendTransactionItemUM(
                    onDismiss = value.actions.onDismiss,
                    onSend = value.actions.onSign,
                    appInfo = appInfoContentUMConverter.convert(
                        WcTransactionAppInfoContentUMConverter.Input(
                            session = value.context.session,
                            onShowVerifiedAlert = value.actions.onShowVerifiedAlert,
                        ),
                    ),
                    feeState = value.feeState,
                    portfolioName = value.portfolioName,
                    networkInfo = networkInfoUMConverter.convert(value.context.network),
                    estimatedWalletChanges = WcSendReceiveTransactionCheckResultsUM(),
                    isLoading = value.signState.domainStep == WcSignStep.Signing,
                    address = WcAddressConverter.convert(value.context.derivationState),
                    transactionValidationResult = value.securityCheck?.result?.validation,
                    sendEnabled = when (value.feeState) {
                        WcTransactionFeeState.None -> feeErrorNotification == null
                        is WcTransactionFeeState.Success -> {
                            value.feeSelectorUM is FeeSelectorUM.Content && feeErrorNotification == null
                        }
                    },
                    feeErrorNotification = feeErrorNotification,
                    isHoldToConfirmEnabled = value.context.session.wallet.isHotWallet,
                ),
                feeSelectorUM = when (value.feeState) {
                    WcTransactionFeeState.None -> FeeSelectorUM.Loading
                    is WcTransactionFeeState.Success -> value.feeSelectorUM ?: FeeSelectorUM.Loading
                },
                transactionRequestInfo = WcTransactionRequestInfoUM(
                    blocks = buildRequestInfoBlocks(value),
                    onCopy = value.actions.onCopy,
                ),
            )
            is WcBitcoinMethod.GetAccountAddresses,
            is WcEthMethod.AddEthereumChain,
            is WcEthMethod.MessageSign,
            is WcEthMethod.SignTypedData,
            is WcEthMethod.SwitchEthereumChain,
            is WcMethod.Unsupported,
            is WcSolanaMethod.SignMessage,
            -> null
        }
    }

    private fun buildRequestInfoBlocks(value: Input): ImmutableList<WcTransactionRequestBlockUM> = buildList {
        addAll(
            requestBlockUMConverter.convert(
                WcTransactionRequestBlockUMConverter.Input(value.context.rawSdkRequest),
            ),
        )
        val method = value.context.method
        if (method is WcBitcoinMethod.SendTransfer) {
            add(
                btcSendTransferRequestInfoConverter.convert(
                    WcBtcSendTransferRequestInfoConverter.Input(
                        method = method,
                        decimals = value.cryptoCurrencyStatus.currency.decimals,
                        symbol = value.cryptoCurrencyStatus.currency.symbol,
                    ),
                ),
            )
        }
        if (method is WcBitcoinMethod.SignPsbt && value.psbtOutputs != null) {
            addAll(
                signPsbtRequestInfoConverter.convert(
                    WcSignPsbtRequestInfoConverter.Input(
                        outputs = value.psbtOutputs,
                        decimals = value.cryptoCurrencyStatus.currency.decimals,
                        symbol = value.cryptoCurrencyStatus.currency.symbol,
                    ),
                ),
            )
        }
    }.toImmutableList()

    data class Input(
        val context: WcMethodContext,
        val portfolioName: AccountTitleUM?,
        val feeState: WcTransactionFeeState,
        val signState: WcSignState<*>,
        val actions: WcTransactionActionsUM,
        val feeSelectorUM: FeeSelectorUM?,
        val cryptoCurrencyStatus: CryptoCurrencyStatus,
        val securityCheck: BlockAidTransactionCheck.Result?,
        val psbtOutputs: List<WcPsbtOutput>?,
        val onFeeReload: () -> Unit,
    )
}