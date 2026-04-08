package com.tangem.feature.tokendetails.presentation.tokendetails.model

import com.tangem.common.core.TangemSdkError
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.res.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.domain.dynamicaddresses.EnableDynamicAddressesError
import com.tangem.domain.dynamicaddresses.EnableDynamicAddressesUseCase
import com.tangem.domain.dynamicaddresses.IsXpubDerivedUseCase
import com.tangem.domain.dynamicaddresses.repository.DynamicAddressesRepository
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.usecase.GetExtendedPublicKeyForCurrencyUseCase
import com.tangem.feature.tokendetails.presentation.tokendetails.ui.components.dynamicaddresses.DynamicAddressesBottomSheetConfig
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@Suppress("LongParameterList")
internal class DynamicAddressesDelegate(
    private val enableDynamicAddressesUseCase: EnableDynamicAddressesUseCase,
    private val isXpubDerivedUseCase: IsXpubDerivedUseCase,
    private val dynamicAddressesRepository: DynamicAddressesRepository,
    private val getExtendedPublicKeyUseCase: GetExtendedPublicKeyForCurrencyUseCase,
    private val uiMessageSender: UiMessageSender,
    private val userWalletId: UserWalletId,
    private val network: Network,
    private val coroutineScope: CoroutineScope,
    private val dispatchers: CoroutineDispatcherProvider,
    private val showBottomSheet: () -> Unit,
    private val dismissBottomSheet: () -> Unit,
    private val onDynamicAddressesEnabled: () -> Unit,
) {

    private val _bottomSheetConfig = MutableStateFlow<DynamicAddressesBottomSheetConfig>(
        DynamicAddressesBottomSheetConfig.Enable(
            isCardScanRequired = false,
            onEnableClick = {},
        ),
    )
    val bottomSheetConfig: StateFlow<DynamicAddressesBottomSheetConfig> = _bottomSheetConfig.asStateFlow()

    fun onDynamicAddressesClick() {
        coroutineScope.launch(dispatchers.main) {
            val hasConflicts = dynamicAddressesRepository.hasConflictingCustomTokens(userWalletId, network)
            if (hasConflicts) {
                _bottomSheetConfig.value = DynamicAddressesBottomSheetConfig.Unavailable(
                    onGotItClick = dismissBottomSheet,
                )
                showBottomSheet()
                return@launch
            }

            val isCardScanRequired = !isXpubAlreadyDerived()
            _bottomSheetConfig.value = DynamicAddressesBottomSheetConfig.Enable(
                isCardScanRequired = isCardScanRequired,
                onEnableClick = ::onEnableClick,
            )
            showBottomSheet()
        }
    }

    private fun onEnableClick() {
        coroutineScope.launch(dispatchers.main) {
            _bottomSheetConfig.value = DynamicAddressesBottomSheetConfig.Enable(
                isCardScanRequired = false,
                isLoading = true,
                onEnableClick = {},
            )

            val xpub = getExtendedPublicKeyUseCase(userWalletId, network).fold(
                ifLeft = { error ->
                    if (isUserCancellation(error)) {
                        dismissBottomSheet()
                    } else {
                        TangemLogger.e("Failed to get XPUB: ${error.message}")
                        _bottomSheetConfig.value = DynamicAddressesBottomSheetConfig.ServiceUnavailable(
                            onGotItClick = dismissBottomSheet,
                        )
                    }
                    return@launch
                },
                ifRight = { it },
            )

            enableDynamicAddressesUseCase(userWalletId, network, xpub).fold(
                ifLeft = { error ->
                    when (error) {
                        is EnableDynamicAddressesError.ConflictingCustomTokens -> {
                            _bottomSheetConfig.value = DynamicAddressesBottomSheetConfig.Unavailable(
                                onGotItClick = dismissBottomSheet,
                            )
                        }
                        is EnableDynamicAddressesError.ServiceError -> {
                            TangemLogger.e("Failed to enable DA: ${error.cause.message}")
                            _bottomSheetConfig.value = DynamicAddressesBottomSheetConfig.ServiceUnavailable(
                                onGotItClick = dismissBottomSheet,
                            )
                        }
                    }
                },
                ifRight = {
                    dismissBottomSheet()
                    onDynamicAddressesEnabled()
                    uiMessageSender.send(
                        SnackbarMessage(message = resourceReference(R.string.dynamic_addresses_enabled_toast_title)),
                    )
                },
            )
        }
    }

    private suspend fun isXpubAlreadyDerived(): Boolean {
        return isXpubDerivedUseCase(userWalletId, network)
    }

    private fun isUserCancellation(error: Throwable): Boolean {
        return error is TangemSdkError.UserCancelled || error.cause is TangemSdkError.UserCancelled
    }
}