package com.tangem.features.onboarding.v2.addresssync.model

import com.arkivanov.decompose.DelicateDecomposeApi
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.replaceCurrent
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.account.supplier.MultiAccountListSupplier
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.networks.multi.MultiNetworkStatusFetcher
import com.tangem.domain.settings.CanUseBiometryUseCase
import com.tangem.domain.settings.ShouldAskPermissionUseCase
import com.tangem.domain.settings.ShouldShowAskBiometryUseCase
import com.tangem.domain.staking.StakingIdFactory
import com.tangem.domain.staking.multi.MultiStakingBalanceFetcher
import com.tangem.domain.tokens.MultiWalletAccountListFetcher
import com.tangem.domain.wallets.usecase.DerivePublicKeysUseCase
import com.tangem.features.onboarding.v2.addresssync.navigation.AddressSyncStep
import com.tangem.features.onboarding.v2.multiwallet.api.OnboardingMultiWalletComponent
import com.tangem.features.onboarding.v2.multiwallet.impl.MultiWalletInnerNavigationState
import com.tangem.features.onboarding.v2.multiwallet.impl.child.MultiWalletChildParams
import com.tangem.features.onboarding.v2.title.OnboardingTitle
import com.tangem.features.pushnotifications.api.utils.PUSH_PERMISSION
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.joinAll
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
    private val derivePublicKeysUseCase: DerivePublicKeysUseCase,
    private val multiNetworkStatusFetcher: MultiNetworkStatusFetcher,
    private val multiStakingBalanceFetcher: MultiStakingBalanceFetcher,
    private val stakingIdFactory: StakingIdFactory,
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
        params.innerNavigation.value = MultiWalletInnerNavigationState(
            stackSize = AddressSyncStep.ASK_BIOMETRY.pageNumber,
            stackMaxSize = ADDRESS_SYNC_MAX_STEPS,
        )
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
        modelScope.launch { tryToSkipNotificationScreen(next.step) }
    }

    fun initConfiguration() {
        modelScope.launch {
            val shouldShowAskBiometry = canUseBiometryUseCase.strict() && shouldShowAskBiometryUseCase()
            val shouldShowAskNotification = shouldAskPermissionUseCase(PUSH_PERMISSION)
            val step = when {
                !shouldShowAskBiometry && !shouldShowAskNotification -> AddressSyncStep.ADDRESS_SYNC
                !shouldShowAskBiometry -> AddressSyncStep.ASK_NOTIFICATIONS
                else -> AddressSyncStep.ASK_BIOMETRY
            }
            nextScreen(AddressSyncIntent.Next(step))
        }
    }

    private suspend fun tryToSkipNotificationScreen(step: AddressSyncStep) {
        when (step) {
            AddressSyncStep.ASK_NOTIFICATIONS -> {
                val shouldShowAskNotification = shouldAskPermissionUseCase(PUSH_PERMISSION)
                if (shouldShowAskNotification.not()) {
                    nextScreen(AddressSyncIntent.Next(step = AddressSyncStep.ADDRESS_SYNC))
                }
            }
            AddressSyncStep.LOADING, AddressSyncStep.ASK_BIOMETRY, AddressSyncStep.ADDRESS_SYNC -> Unit
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
        next.step.stringId?.let { stringId ->
            params.parentParams.titleProvider.changeTitle(
                title = OnboardingTitle(
                    text = resourceReference(stringId),
                    shouldForceTitle = true,
                ),
            )
        }
    }

    private fun fetchWalletCrypto() {
        modelScope.launch {
            multiWalletAccountListFetcher(
                params = MultiWalletAccountListFetcher.Params(userWalletId = walletId),
            ).fold(
                ifLeft = {
                    state.value = AddressSyncState.Exit
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
                    AddressSyncState.Exit
                } else {
                    AddressSyncState.Success(currencies = currencies)
                }
                state.value = updatedState
            }
            .collect()
    }

    private fun startSyncing() {
        modelScope.launch {
            val successWithLoading = (state.value as AddressSyncState.Success).copy(isButtonLoading = true)
            state.value = successWithLoading
            val cryptoCurrencies = successWithLoading.currencies
            derivePublicKeysUseCase(
                userWalletId = walletId,
                currencies = cryptoCurrencies,
            ).fold(
                ifLeft = { throwable ->
                    state.value = successWithLoading.copy(
                        isButtonLoading = false,
                    )
                    TangemLogger.e("Failed to derive public keys", throwable)
                },
                ifRight = {
                    listOf(
                        launch { fetchNetworks(cryptoCurrencies) },
                        launch { fetchStaking(cryptoCurrencies) },
                    ).joinAll()
                    state.update { addressSyncState ->
                        addressSyncState as AddressSyncState.Success
                        addressSyncState.copy(shouldExit = true)
                    }
                },
            )
        }
    }

    private suspend fun fetchNetworks(cryptoCurrencies: List<CryptoCurrency>) {
        multiNetworkStatusFetcher.invoke(
            MultiNetworkStatusFetcher.Params(
                userWalletId = walletId,
                networks = cryptoCurrencies.map(CryptoCurrency::network).toSet(),
            ),
        )
            .onLeft { TangemLogger.e("Unable to fetch networks: $it") }
    }

    private suspend fun fetchStaking(cryptoCurrencies: List<CryptoCurrency>) {
        val stakingIds = cryptoCurrencies.mapNotNullTo(hashSetOf()) {
            stakingIdFactory.create(userWalletId = walletId, cryptoCurrency = it).getOrNull()
        }

        multiStakingBalanceFetcher(
            params = MultiStakingBalanceFetcher.Params(
                userWalletId = walletId,
                stakingIds = stakingIds,
            ),
        )
            .onLeft { TangemLogger.e("Unable to fetch yield balances: $it") }
    }

    private companion object {
        const val ADDRESS_SYNC_MAX_STEPS = 3
    }
}