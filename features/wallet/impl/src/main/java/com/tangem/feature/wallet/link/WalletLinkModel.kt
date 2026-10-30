package com.tangem.feature.wallet.link

import com.tangem.common.ui.userwallet.ext.walletInterationIcon
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.modalstatus.TangemModalStatus
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.modal.MessageModalUM
import com.tangem.core.ui.message.modal.messageModalUM
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.wallets.registration.WalletLinkError
import com.tangem.domain.wallets.registration.WalletLinkRequester
import com.tangem.domain.wallets.usecase.LinkWalletToDeviceUseCase
import com.tangem.feature.wallet.impl.R
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@ModelScoped
internal class WalletLinkModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val linkWalletToDeviceUseCase: LinkWalletToDeviceUseCase,
    private val uiMessageSender: UiMessageSender,
) : Model() {

    /** `null` while the sheet is hidden */
    val uiState: StateFlow<MessageModalUM?>
        field = MutableStateFlow(null)

    private val result = MutableStateFlow<WalletLinkRequester.Result?>(null)
    private val linkJobHolder = JobHolder()

    fun show(userWallet: UserWallet) {
        result.value = null
        uiState.value = createState(userWallet, isLoading = false)
    }

    suspend fun waitResult(): WalletLinkRequester.Result = result.filterNotNull().first()

    fun onDismiss() {
        linkJobHolder.cancel()
        close(WalletLinkRequester.Result.Dismissed)
    }

    private fun onLinkClick(userWallet: UserWallet) {
        uiState.value = createState(userWallet, isLoading = true)

        modelScope.launch {
            linkWalletToDeviceUseCase(userWallet).fold(
                ifLeft = { error ->
                    if (error is WalletLinkError.Cancelled) {
                        uiState.value = createState(userWallet, isLoading = false)
                    } else {
                        close(WalletLinkRequester.Result.Dismissed)
                        showErrorDialog()
                    }
                },
                ifRight = { close(WalletLinkRequester.Result.Linked) },
            )
        }.saveIn(linkJobHolder)
    }

    private fun close(result: WalletLinkRequester.Result) {
        uiState.value = null
        this.result.value = result
    }

    private fun showErrorDialog() {
        uiMessageSender.send(
            DialogMessage(
                title = resourceReference(R.string.common_something_went_wrong),
                message = resourceReference(R.string.common_try_again_later),
            ),
        )
    }

    private fun createState(userWallet: UserWallet, isLoading: Boolean): MessageModalUM {
        val isHotWallet = userWallet is UserWallet.Hot

        return messageModalUM {
            status {
                variant = TangemModalStatus.Variant.Warning
                title = resourceReference(R.string.warning_link_wallet_title)
                description = resourceReference(
                    if (isHotWallet) {
                        R.string.warning_link_wallet_hot_message
                    } else {
                        R.string.warning_link_wallet_cold_message
                    },
                )
            }
            primaryButton {
                text = resourceReference(if (isHotWallet) R.string.common_confirm else R.string.common_scan_card)
                iconEnd = walletInterationIcon(userWallet)?.let { icon -> TangemIconUM.Icon(iconRes = icon) }
                this.isLoading = isLoading
                onClick { if (!isLoading) onLinkClick(userWallet) }
            }
        }
    }
}