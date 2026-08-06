package com.tangem.features.tangempay.multichain.receive

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.navigation.share.ShareManager
import com.tangem.core.ui.clipboard.ClipboardManager
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.domain.models.account.PaymentNetworkStatus
import com.tangem.domain.pay.flow.PaymentAccountStatusSupplier
import com.tangem.features.tangempay.details.impl.R
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@Stable
@ModelScoped
internal class PaymentReceiveModel @Inject constructor(
    paramsContainer: ParamsContainer,
    paymentAccountStatusSupplier: PaymentAccountStatusSupplier,
    private val clipboardManager: ClipboardManager,
    private val shareManager: ShareManager,
    private val uiMessageSender: UiMessageSender,
    override val dispatchers: CoroutineDispatcherProvider,
) : Model() {

    private val params = paramsContainer.require<PaymentReceiveComponent.Params>()

    private val converter = PaymentReceiveUMConverter(
        onCopy = ::onCopy,
        onShowQr = ::onShowQr,
        onShare = ::onShare,
        onDismiss = ::onDismiss,
    )

    val uiState: StateFlow<PaymentReceiveUM>
        field = MutableStateFlow(
            converter.convert(
                PaymentReceiveUMConverter.Input(
                    networkName = "",
                    address = "",
                    currencies = emptyList(),
                ),
            ),
        )

    init {
        paymentAccountStatusSupplier.invoke(params.walletId)
            .onEach { status ->
                val loaded = status.value as? PaymentAccountStatusValue.Loaded ?: return@onEach
                val available = loaded.networks
                    .filterIsInstance<PaymentNetworkStatus.Available>()
                    .firstOrNull { it.network.rawId == params.networkRawId }
                    ?: return@onEach
                uiState.value = converter.convert(
                    PaymentReceiveUMConverter.Input(
                        networkName = available.network.name,
                        address = available.depositAddress,
                        currencies = available.cryptoCurrencyStatuses.map { it.currency },
                    ),
                )
            }
            .launchIn(modelScope)
    }

    fun onDismiss() {
        params.onDismiss()
    }

    private fun onCopy() {
        clipboardManager.setText(text = uiState.value.address, isSensitive = true)
        uiMessageSender.send(
            SnackbarMessage(
                message = resourceReference(R.string.wallet_notification_address_copied),
                startIconId = R.drawable.ic_check_24,
            ),
        )
    }

    private fun onShowQr() {
        // No model-side effect yet; the sheet toggles its own local QR view. Kept as a callback so
        // analytics can be wired in without touching the UI layer.
    }

    private fun onShare() {
        shareManager.shareText(text = uiState.value.address)
    }
}