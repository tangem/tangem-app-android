package com.tangem.features.onboarding.v2.addresssync.model

import com.arkivanov.decompose.DelicateDecomposeApi
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.replaceCurrent
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.account.supplier.MultiAccountListSupplier
import com.tangem.domain.settings.CanUseBiometryUseCase
import com.tangem.domain.settings.ShouldAskPermissionUseCase
import com.tangem.domain.settings.ShouldShowAskBiometryUseCase
import com.tangem.domain.tokens.MultiWalletAccountListFetcher
import com.tangem.features.onboarding.v2.addresssync.navigation.AddressSyncStep
import com.tangem.features.onboarding.v2.multiwallet.api.OnboardingMultiWalletComponent
import com.tangem.features.onboarding.v2.multiwallet.impl.child.MultiWalletChildParams
import com.tangem.features.pushnotifications.api.utils.PUSH_PERMISSION
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(DelicateDecomposeApi::class)
@Suppress("LongParameterList")
@ModelScoped
internal class AddressSyncModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val shouldShowAskBiometryUseCase: ShouldShowAskBiometryUseCase,
    private val canUseBiometryUseCase: CanUseBiometryUseCase,
    private val shouldAskPermissionUseCase: ShouldAskPermissionUseCase,
    private val multiWalletAccountListFetcher: MultiWalletAccountListFetcher,
    private val multiAccountListSupplier: MultiAccountListSupplier,
    paramsContainer: ParamsContainer,
) : Model() {

    private val params = paramsContainer.require<MultiWalletChildParams>()
    private val walletId = (params.parentParams.mode as OnboardingMultiWalletComponent.Mode.AddressSync).userWalletId
    val stackNavigation = StackNavigation<AddressSyncStep>()
    val state: StateFlow<AddressSyncState>
        field = MutableStateFlow<AddressSyncState>(
            value = AddressSyncState.Loading,
        )

    init {
        params.innerNavigation.update { innerNavigationState ->
            innerNavigationState.copy(
                stackSize = AddressSyncStep.ASK_BIOMETRY.pageNumber,
                stackMaxSize = ADDRESS_SYNC_MAX_STEPS,
            )
        }
        modelScope.launch {
            trySkippingScreen(AddressSyncStep.ASK_BIOMETRY)
        }
        fetchWalletCrypto()
    }

    fun onIntent(intent: AddressSyncIntent) {
        when (intent) {
            is AddressSyncIntent.Next -> nextScreen(intent)
            AddressSyncIntent.Sync -> startSyncing()
        }
    }

    private fun nextScreen(next: AddressSyncIntent.Next) {
        stackNavigation.replaceCurrent(configuration = next.step)
        updateStepperPage(next)
        updateTitle(next)
        modelScope.launch { trySkippingScreen(next.step) }
    }

    private suspend fun trySkippingScreen(step: AddressSyncStep) {
        when (step) {
            AddressSyncStep.ASK_BIOMETRY -> {
                val shouldShowAskBiometry = canUseBiometryUseCase.strict() && shouldShowAskBiometryUseCase()
                if (shouldShowAskBiometry.not()) {
                    nextScreen(AddressSyncIntent.Next(step = AddressSyncStep.ASK_NOTIFICATIONS))
                }
            }
            AddressSyncStep.ASK_NOTIFICATIONS -> {
                val shouldShowAskNotification = shouldAskPermissionUseCase(PUSH_PERMISSION)
                if (shouldShowAskNotification.not()) {
                    nextScreen(AddressSyncIntent.Next(step = AddressSyncStep.ADDRESS_SYNC))
                }
            }
            AddressSyncStep.ADDRESS_SYNC -> Unit
        }
    }

    private fun updateStepperPage(next: AddressSyncIntent.Next) {
        params.innerNavigation.update { innerNavigationState ->
            innerNavigationState.copy(
                stackSize = next.step.pageNumber,
            )
        }
    }

    private fun updateTitle(next: AddressSyncIntent.Next) {
        params.parentParams.titleProvider.changeTitle(
            text = resourceReference(next.step.stringId),
        )
    }

    private fun fetchWalletCrypto() {
        modelScope.launch {
            multiWalletAccountListFetcher(
                params = MultiWalletAccountListFetcher.Params(userWalletId = walletId),
            ).fold(
                ifLeft = {
                    state.value = AddressSyncState.NoTokens
                },
                ifRight = {
                    handleAddressSyncStep()
                },
            )
        }
    }

    private suspend fun handleAddressSyncStep() {
        multiAccountListSupplier()
            .map { accountLists ->
                accountLists
                    .first { it.userWalletId == walletId }
                    .flattenCurrencies()
            }
            .onEach { currencies ->
                val updatedState = if (currencies.isEmpty()) {
                    AddressSyncState.NoTokens
                } else {
                    AddressSyncState.Success(currenciesCount = currencies.size)
                }
                state.value = updatedState
            }
            .collect()
    }

    private fun startSyncing() {
        TODO("Will be implemented during [REDACTED_TASK_KEY]")
    }

    private companion object {
        const val ADDRESS_SYNC_MAX_STEPS = 3
    }
}