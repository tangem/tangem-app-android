package com.tangem.feature.tester.presentation.addresses.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tangem.core.ui.clipboard.ClipboardManager
import com.tangem.domain.feedback.models.BlockchainInfo
import com.tangem.domain.feedback.models.FeedbackEmailType
import com.tangem.domain.feedback.models.WalletMetaInfo
import com.tangem.domain.feedback.repository.FeedbackRepository
import com.tangem.domain.feedback.utils.EmailMessageBodyResolver
import com.tangem.domain.wallets.usecase.GetSelectedWalletSyncUseCase
import com.tangem.feature.tester.impl.R
import com.tangem.feature.tester.presentation.addresses.model.TesterAddressJson
import com.tangem.feature.tester.presentation.addresses.state.AddressesInfoScreenUM
import com.tangem.feature.tester.presentation.addresses.state.CopyType
import com.tangem.feature.tester.presentation.navigation.InnerTesterRouter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * Tester screen for displaying addresses and tokens.
 * Uses the same data source as feedback emails to ensure identical output.
 */

@HiltViewModel
internal class AddressesInfoViewModel @Inject constructor(
    private val feedbackRepository: FeedbackRepository,
    private val getSelectedWalletSyncUseCase: GetSelectedWalletSyncUseCase,
    private val clipboardManager: ClipboardManager,
) : ViewModel() {

    private val emailMessageBodyResolver = EmailMessageBodyResolver(feedbackRepository)

    private val _uiState = MutableStateFlow(getInitialState())
    val uiState: StateFlow<AddressesInfoScreenUM> = _uiState.asStateFlow()

    fun setupNavigation(router: InnerTesterRouter) {
        _uiState.update { state ->
            state.copy(
                onBackClick = router::back,
                onCopyClick = ::onCopyClicked,
            )
        }

        loadAddresses()
    }

    private fun getInitialState(): AddressesInfoScreenUM = AddressesInfoScreenUM(
        title = R.string.addresses_info,
        addressesText = "Loading...",
        addressesJson = "[]",
        onBackClick = {},
        onCopyClick = {},
    )

    private fun loadAddresses() {
        viewModelScope.launch {
            try {
                val wallet = getSelectedWalletSyncUseCase()
                    .getOrNull() ?: run {
                    setEmptyState()
                    return@launch
                }

                val walletMetaInfo = WalletMetaInfo(
                    userWalletId = wallet.walletId,
                )

                val emailType = FeedbackEmailType.DirectUserRequest(
                    walletMetaInfo = walletMetaInfo,
                )

                val emailBody = emailMessageBodyResolver.resolve(emailType)

                val blockchainInfos = feedbackRepository.getBlockchainInfoList(wallet.walletId)

                val json = formatAddressesJson(blockchainInfos)

                _uiState.update { state ->
                    state.copy(
                        addressesText = emailBody,
                        addressesJson = json,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _uiState.update { state ->
                    state.copy(
                        addressesText = "Error loading addresses",
                        addressesJson = errorJson(e),
                    )
                }
            }
        }
    }

    private fun setEmptyState() {
        _uiState.update { state ->
            state.copy(
                addressesText = "No active addresses found",
                addressesJson = "[]",
            )
        }
    }

    @Suppress("JSON_FORMAT_REDUNDANT")
    private fun formatAddressesJson(list: List<BlockchainInfo>): String {
        val jsonList = list
            .flatMap { info ->
                val addresses = info
                    .extractAddresses()
                    .sortedDescending()

                val tokens = if (info.tokens.isNotEmpty()) {
                    info.tokens.map { it.name }
                } else {
                    listOf(info.blockchain)
                }

                tokens.map { token ->
                    TesterAddressJson(
                        addresses = addresses,
                        blockchain = info.blockchain,
                        derivationPath = info.derivationPath,
                        token = token,
                    )
                }
            }
            .sortedWith(
                compareBy<TesterAddressJson> { it.blockchain }
                    .thenBy { it.derivationPath }
                    .thenBy { it.token },
            )

        return Json { prettyPrint = true }
            .encodeToString(jsonList)
    }

    private fun BlockchainInfo.extractAddresses(): List<String> = when (val a = addresses) {
        is BlockchainInfo.Addresses.Single ->
            listOf(a.value)

        is BlockchainInfo.Addresses.Multiple ->
            a.values.map { it.value }
    }

    private fun errorJson(error: Throwable): String = Json.encodeToString(
        mapOf("error" to (error.message ?: "Unknown error")),
    )

    private fun onCopyClicked(type: CopyType) {
        val state = _uiState.value

        val text = when (type) {
            CopyType.TEXT -> state.addressesText
            CopyType.JSON -> state.addressesJson
        }

        if (text.isNotBlank()) {
            clipboardManager.setText(
                text = text,
                isSensitive = false,
                label = type.name,
            )
        }
    }
}