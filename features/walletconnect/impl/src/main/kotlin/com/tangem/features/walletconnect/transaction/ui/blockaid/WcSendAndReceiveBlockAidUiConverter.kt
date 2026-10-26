package com.tangem.features.walletconnect.transaction.ui.blockaid

import com.domain.blockaid.models.transaction.CheckTransactionResult
import com.domain.blockaid.models.transaction.SimulationResult
import com.domain.blockaid.models.transaction.ValidationResult
import com.domain.blockaid.models.transaction.simultation.AmountInfo
import com.domain.blockaid.models.transaction.simultation.ApproveInfo
import com.domain.blockaid.models.transaction.simultation.SimulationData
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.walletconnect.model.WcApprovedAmount
import com.tangem.features.walletconnect.impl.R
import com.tangem.features.walletconnect.transaction.entity.blockaid.BlockAidNotificationUM
import com.tangem.features.walletconnect.transaction.entity.blockaid.WcEstimatedWalletChangeUM
import com.tangem.features.walletconnect.transaction.entity.blockaid.WcEstimatedWalletChangesUM
import com.tangem.features.walletconnect.transaction.entity.blockaid.WcSendReceiveTransactionCheckResultsUM
import com.tangem.utils.StringsSigns
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.toImmutableList
import java.math.BigDecimal

private const val DECIMALS_AMOUNT = 2

internal class WcSendAndReceiveBlockAidUiConverter :
    Converter<WcSendAndReceiveBlockAidUiConverter.Input, WcSendReceiveTransactionCheckResultsUM> {

    private val estimatedWalletChangeUMConverter = WcEstimatedWalletChangeUMConverter()
    private val spendAllowanceUMConverter = WcSpendAllowanceUMConverter()

    override fun convert(value: Input): WcSendReceiveTransactionCheckResultsUM {
        val description = value.result.description?.let { if (it.isNotEmpty()) TextReference.Str(it) else null }
        val simulation = value.result.simulation
        return WcSendReceiveTransactionCheckResultsUM(
            isLoading = false,
            notification = when (value.result.validation) {
                ValidationResult.SAFE, ValidationResult.FAILED_TO_VALIDATE -> null
                ValidationResult.UNSAFE -> BlockAidNotificationUM(
                    type = BlockAidNotificationUM.Type.ERROR,
                    title = TextReference.Res(R.string.wc_malicious_transaction),
                    text = description,
                )
                ValidationResult.WARNING -> BlockAidNotificationUM(
                    type = BlockAidNotificationUM.Type.WARNING,
                    title = TextReference.Res(R.string.wc_warning_transaction),
                    text = description,
                )
            },
            additionalNotification = (simulation as? SimulationResult.Success)?.data?.let { data ->
                if (data is SimulationData.NoWalletChangesDetected) {
                    TextReference.Res(R.string.wc_no_wallet_changes_detected)
                } else {
                    null
                }
            },
            estimatedWalletChanges = (simulation as? SimulationResult.Success)?.data
                ?.let(::convertEstimatedWalletChanges),
            spendAllowance = (value.result.simulation as? SimulationResult.Success)?.data?.let { data ->
                when (data) {
                    is SimulationData.SendAndReceive, SimulationData.NoWalletChangesDetected -> null
                    is SimulationData.Approve -> value.approvedAmount?.let {
                        spendAllowanceUMConverter.convert(
                            WcSpendAllowanceUMConverter.Input(
                                approvedAmount = it,
                                onLearnMoreClick = value.onApproveLearnMoreClick,
                            ),
                        )
                    }
                }
            },
        )
    }

    private fun convertEstimatedWalletChanges(data: SimulationData): WcEstimatedWalletChangesUM? {
        val items = when (data) {
            is SimulationData.NoWalletChangesDetected -> emptyList()
            is SimulationData.Approve -> data.items.mapNotNull(::convertNftApprove) +
                convertSendAndReceive(send = data.send, receive = data.receive)
            is SimulationData.SendAndReceive -> convertSendAndReceive(send = data.send, receive = data.receive)
        }
        return WcEstimatedWalletChangesUM(items.toImmutableList()).takeIf { items.isNotEmpty() }
    }

    private fun convertNftApprove(item: ApproveInfo): WcEstimatedWalletChangeUM? {
        if (item !is ApproveInfo.NonFungibleToken) return null

        return WcEstimatedWalletChangeUM(
            iconRes = R.drawable.img_approvale_new_24,
            title = TextReference.Res(R.string.common_approve),
            description = item.name,
            tokenIconUrl = item.logoUrl,
        )
    }

    private fun convertSendAndReceive(
        send: List<AmountInfo>,
        receive: List<AmountInfo>,
    ): List<WcEstimatedWalletChangeUM> {
        return send.map { amountInfo ->
            convertAmountInfo(
                amountInfo = amountInfo,
                iconRes = R.drawable.ic_send_new_24,
                titleRes = R.string.common_send,
                sign = StringsSigns.MINUS,
            )
        } + receive.map { amountInfo ->
            convertAmountInfo(
                amountInfo = amountInfo,
                iconRes = R.drawable.ic_receive_new_24,
                titleRes = R.string.common_receive,
                sign = StringsSigns.PLUS,
            )
        }
    }

    private fun convertAmountInfo(
        amountInfo: AmountInfo,
        iconRes: Int,
        titleRes: Int,
        sign: String,
    ): WcEstimatedWalletChangeUM {
        return estimatedWalletChangeUMConverter.convert(
            WcEstimatedWalletChangeUMConverter.Input(
                amountInfo = amountInfo,
                iconRes = iconRes,
                titleRes = titleRes,
                sign = sign,
            ),
        )
    }

    data class Input(
        val result: CheckTransactionResult,
        val approvedAmount: WcApprovedAmount?,
        val onApproveLearnMoreClick: () -> Unit,
    )
}

internal fun BigDecimal.amountText() = format { crypto("", DECIMALS_AMOUNT) }