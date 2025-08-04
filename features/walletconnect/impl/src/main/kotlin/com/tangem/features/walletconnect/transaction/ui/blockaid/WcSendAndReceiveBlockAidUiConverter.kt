package com.tangem.features.walletconnect.transaction.ui.blockaid

import com.domain.blockaid.models.transaction.CheckTransactionResult
import com.domain.blockaid.models.transaction.SimulationResult
import com.domain.blockaid.models.transaction.ValidationResult
import com.domain.blockaid.models.transaction.simultation.AmountInfo
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
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import java.math.BigDecimal
import javax.inject.Inject

internal const val DECIMALS_AMOUNT = 2

@Suppress("CyclomaticComplexMethod", "LongMethod")
internal class WcSendAndReceiveBlockAidUiConverter @Inject constructor(
    private val estimatedWalletChangeUMConverter: WcEstimatedWalletChangeUMConverter,
    private val spendAllowanceUMConverter: WcSpendAllowanceUMConverter,
) : Converter<WcSendAndReceiveBlockAidUiConverter.Input, WcSendReceiveTransactionCheckResultsUM> {
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
            estimatedWalletChanges = (simulation as? SimulationResult.Success)?.data?.let { data ->
                when (data) {
                    is SimulationData.Approve, SimulationData.NoWalletChangesDetected -> null
                    is SimulationData.SendAndReceive -> {
                        val items: ImmutableList<WcEstimatedWalletChangeUM> = (
                            data.send.map {
                                when (it) {
                                    is AmountInfo.FungibleTokens -> estimatedWalletChangeUMConverter.convert(
                                        WcEstimatedWalletChangeUMConverter.Input(
                                            amountInfo = it,
                                            iconRes = R.drawable.ic_send_new_24,
                                            titleRes = R.string.common_send,
                                            sign = StringsSigns.MINUS,
                                        ),
                                    )
                                    is AmountInfo.NonFungibleTokens -> estimatedWalletChangeUMConverter.convert(
                                        WcEstimatedWalletChangeUMConverter.Input(
                                            amountInfo = it,
                                            iconRes = R.drawable.ic_send_new_24,
                                            titleRes = R.string.common_send,
                                        ),
                                    )
                                }
                            } + data.receive.map {
                                when (it) {
                                    is AmountInfo.FungibleTokens -> estimatedWalletChangeUMConverter.convert(
                                        WcEstimatedWalletChangeUMConverter.Input(
                                            amountInfo = it,
                                            iconRes = R.drawable.ic_receive_new_24,
                                            titleRes = R.string.common_receive,
                                            sign = StringsSigns.PLUS,
                                        ),
                                    )
                                    is AmountInfo.NonFungibleTokens -> estimatedWalletChangeUMConverter.convert(
                                        WcEstimatedWalletChangeUMConverter.Input(
                                            amountInfo = it,
                                            iconRes = R.drawable.ic_receive_new_24,
                                            titleRes = R.string.common_receive,
                                        ),
                                    )
                                }
                            }
                            ).toImmutableList()
                        WcEstimatedWalletChangesUM(items)
                    }
                }
            },
            spendAllowance = (value.result.simulation as? SimulationResult.Success)?.data?.let { data ->
                when (data) {
                    is SimulationData.SendAndReceive, SimulationData.NoWalletChangesDetected -> null
                    is SimulationData.Approve -> value.approvedAmount?.let {
                        spendAllowanceUMConverter.convert(it)
                    }
                }
            },
        )
    }

    data class Input(
        val result: CheckTransactionResult,
        val approvedAmount: WcApprovedAmount?,
    )
}

internal fun BigDecimal.amountText() = format { crypto("", DECIMALS_AMOUNT) }