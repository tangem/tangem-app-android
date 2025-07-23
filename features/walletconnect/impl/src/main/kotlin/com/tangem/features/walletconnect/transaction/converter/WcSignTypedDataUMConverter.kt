package com.tangem.features.walletconnect.transaction.converter

import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.walletconnect.model.WcEthMethod
import com.tangem.domain.walletconnect.usecase.method.WcMessageSignUseCase
import com.tangem.domain.walletconnect.usecase.method.WcSignState
import com.tangem.domain.walletconnect.usecase.method.WcSignStep
import com.tangem.features.walletconnect.impl.R
import com.tangem.features.walletconnect.transaction.entity.common.WcTransactionActionsUM
import com.tangem.features.walletconnect.transaction.entity.common.WcTransactionRequestBlockUM
import com.tangem.features.walletconnect.transaction.entity.common.WcTransactionRequestInfoItemUM
import com.tangem.features.walletconnect.transaction.entity.common.WcTransactionRequestInfoUM
import com.tangem.features.walletconnect.transaction.entity.sign.WcSignTransactionItemUM
import com.tangem.features.walletconnect.transaction.entity.sign.WcSignTransactionUM
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import javax.inject.Inject

private const val ADDRESS_FIRST_PART_LENGTH = 7
private const val ADDRESS_SECOND_PART_LENGTH = 4

internal class WcSignTypedDataUMConverter @Inject constructor(
    private val appInfoContentUMConverter: WcTransactionAppInfoContentUMConverter,
    private val networkInfoUMConverter: WcNetworkInfoUMConverter,
    private val requestBlockUMConverter: WcTransactionRequestBlockUMConverter,
) : Converter<WcSignTypedDataUMConverter.Input, WcSignTransactionUM> {

    override fun convert(value: Input): WcSignTransactionUM = WcSignTransactionUM(
        transaction = WcSignTransactionItemUM(
            onDismiss = value.actions.onDismiss,
            onSign = value.actions.onSign,
            appInfo = appInfoContentUMConverter.convert(
                WcTransactionAppInfoContentUMConverter.Input(
                    session = value.useCase.session,
                    onShowVerifiedAlert = value.actions.onShowVerifiedAlert,
                ),
            ),
            walletName = value.useCase.session.wallet.name.takeIf { value.useCase.session.showWalletInfo },
            networkInfo = networkInfoUMConverter.convert(value.useCase.network),
            addressText = value.useCase.walletAddress.toShortAddressText(),
            isLoading = value.signState.domainStep == WcSignStep.Signing,
        ),
        transactionRequestInfo = WcTransactionRequestInfoUM(
            blocks = buildList {
                add(
                    requestBlockUMConverter.convert(
                        WcTransactionRequestBlockUMConverter.Input(value.useCase.rawSdkRequest, value.signModel),
                    ),
                )
                (value.useCase.method as? WcEthMethod.SignTypedData)?.params?.message?.to?.let { to ->
                    add(
                        WcTransactionRequestBlockUM(
                            persistentListOf(
                                WcTransactionRequestInfoItemUM(
                                    title = resourceReference(R.string.wc_transaction_info_to_title),
                                ),
                                WcTransactionRequestInfoItemUM(
                                    title = resourceReference(R.string.settings_wallet_name_title),
                                    description = to.name,
                                ),
                                WcTransactionRequestInfoItemUM(
                                    title = resourceReference(R.string.wc_common_wallet),
                                    description = to.wallet,
                                ),
                            ),
                        ),
                    )
                }
            }.toImmutableList(),
            onCopy = value.actions.onCopy,
        ),
    )

    data class Input(
        val useCase: WcMessageSignUseCase,
        val signState: WcSignState<*>,
        val signModel: WcMessageSignUseCase.SignModel,
        val actions: WcTransactionActionsUM,
    )
}

internal fun String.toShortAddressText() =
    "${take(ADDRESS_FIRST_PART_LENGTH)}...${takeLast(ADDRESS_SECOND_PART_LENGTH)}"