package com.tangem.features.walletconnect.transaction.converter

import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.walletconnect.model.WcEthMethod
import com.tangem.domain.walletconnect.model.WcSolanaMethod
import com.tangem.domain.walletconnect.usecase.method.BlockAidTransactionCheck
import com.tangem.domain.walletconnect.usecase.method.WcMethodContext
import com.tangem.domain.walletconnect.usecase.method.WcSignState
import com.tangem.domain.walletconnect.usecase.method.WcSignStep
import com.tangem.features.send.v2.api.entity.FeeSelectorUM
import com.tangem.features.walletconnect.transaction.entity.blockaid.WcSendReceiveTransactionCheckResultsUM
import com.tangem.features.walletconnect.transaction.entity.common.WcTransactionActionsUM
import com.tangem.features.walletconnect.transaction.entity.common.WcTransactionFeeState
import com.tangem.features.walletconnect.transaction.entity.common.WcTransactionRequestInfoUM
import com.tangem.features.walletconnect.transaction.entity.send.WcSendTransactionItemUM
import com.tangem.features.walletconnect.transaction.entity.send.WcSendTransactionUM
import com.tangem.features.walletconnect.utils.WcNotificationsFactory
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.toImmutableList
import javax.inject.Inject

internal class WcSendTransactionUMConverter @Inject constructor(
    private val appInfoContentUMConverter: WcTransactionAppInfoContentUMConverter,
    private val networkInfoUMConverter: WcNetworkInfoUMConverter,
    private val requestBlockUMConverter: WcTransactionRequestBlockUMConverter,
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
                    walletName = value.context.session.wallet.name.takeIf { value.context.session.showWalletInfo },
                    networkInfo = networkInfoUMConverter.convert(value.context.network),
                    estimatedWalletChanges = WcSendReceiveTransactionCheckResultsUM(),
                    isLoading = value.signState.domainStep == WcSignStep.Signing,
                    address = WcAddressConverter.convert(value.context.derivationState),
                    transactionValidationResult = value.securityCheck?.result?.validation,
                    sendEnabled = value.feeSelectorUM is FeeSelectorUM.Content && feeErrorNotification == null,
                    feeErrorNotification = feeErrorNotification,
                ),
                feeSelectorUM = value.feeSelectorUM ?: FeeSelectorUM.Loading,
                transactionRequestInfo = WcTransactionRequestInfoUM(
                    blocks = buildList {
                        addAll(
                            requestBlockUMConverter.convert(
                                WcTransactionRequestBlockUMConverter.Input(value.context.rawSdkRequest),
                            ),
                        )
                    }.toImmutableList(),
                    onCopy = value.actions.onCopy,
                ),
            )
            else -> null
        }
    }

    data class Input(
        val context: WcMethodContext,
        val feeState: WcTransactionFeeState,
        val signState: WcSignState<*>,
        val actions: WcTransactionActionsUM,
        val feeSelectorUM: FeeSelectorUM?,
        val cryptoCurrencyStatus: CryptoCurrencyStatus,
        val securityCheck: BlockAidTransactionCheck.Result?,
        val onFeeReload: () -> Unit,
    )
}