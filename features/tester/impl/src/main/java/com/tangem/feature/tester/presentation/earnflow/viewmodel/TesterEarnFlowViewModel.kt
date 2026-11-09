package com.tangem.feature.tester.presentation.earnflow.viewmodel

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.toNetworkId
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.account.supplier.SingleAccountListSupplier
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.feature.tester.presentation.earnflow.TesterEarnProducts
import com.tangem.feature.tester.presentation.earnflow.TesterEarnSessionSettings
import com.tangem.feature.tester.presentation.earnflow.state.TesterEarnFlowUM
import com.tangem.feature.tester.presentation.navigation.InnerTesterRouter
import com.tangem.features.earn.api.EarnEntrySource
import com.tangem.features.earn.flow.api.EarnFlowComponent
import com.tangem.features.earn.impl.fake.FakeEarnSessionBehavior
import com.tangem.features.earn.impl.fake.FakeEarnSessionPreset
import com.tangem.features.earn.impl.fake.fakeClaimAction
import com.tangem.features.earn.impl.fake.fakeWithdrawNotice
import com.tangem.features.earn.product.api.product.EarnProduct
import com.tangem.features.earn.product.api.session.Deposit
import com.tangem.features.earn.product.api.session.EarnFlowAction
import com.tangem.features.earn.product.api.session.EarnFlowExit
import com.tangem.features.earn.product.api.session.Withdraw
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Opens the shared Earn flow on a fake session against the ETH of the selected wallet's main account. */
@HiltViewModel
internal class TesterEarnFlowViewModel @Inject constructor(
    private val userWalletsListRepository: UserWalletsListRepository,
    private val singleAccountListSupplier: SingleAccountListSupplier,
    private val sessionSettings: TesterEarnSessionSettings,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val selection = MutableStateFlow(Selection())

    val uiState: StateFlow<TesterEarnFlowUM>
        field = MutableStateFlow(TesterEarnFlowUM())

    init {
        loadTarget()
        render()
    }

    fun setupNavigation(router: InnerTesterRouter) {
        uiState.update { it.copy(onBackClick = router::back) }
    }

    private fun loadTarget() {
        viewModelScope.launch {
            val wallet = userWalletsListRepository.selectedUserWalletSync()
            val ethereum = wallet?.let { singleAccountListSupplier.getSyncOrNull(it.walletId) }
                ?.mainAccount
                ?.cryptoCurrencies
                ?.firstOrNull { it is CryptoCurrency.Coin && it.network.rawId == Blockchain.Ethereum.toNetworkId() }
            val target = if (wallet != null && ethereum != null) {
                EarnProduct.Params(userWalletId = wallet.walletId, currency = ethereum)
            } else {
                null
            }
            val description = when {
                wallet == null -> "No selected wallet"
                ethereum == null -> "${wallet.name}: add Ethereum to the main account first"
                else -> "${wallet.name} · ${ethereum.symbol} (main account)"
            }
            selection.update { it.copy(target = target, targetDescription = description) }
            render()
        }
    }

    private fun render() {
        val current = selection.value
        uiState.update { state ->
            state.copy(
                target = current.targetDescription,
                presets = FakeEarnSessionPreset.entries.map { preset ->
                    TesterEarnFlowUM.OptionUM(
                        title = preset.name,
                        isSelected = preset == current.preset,
                        onClick = { select { it.copy(preset = preset) } },
                    )
                }.toImmutableList(),
                actions = TesterEarnAction.entries.map { action ->
                    TesterEarnFlowUM.OptionUM(
                        title = action.name,
                        isSelected = action == current.action,
                        onClick = { select { it.copy(action = action) } },
                    )
                }.toImmutableList(),
                switches = persistentListOf(
                    TesterEarnFlowUM.SwitchUM(
                        title = "Fee grows on verify",
                        isChecked = current.behavior.isFeeGrowingOnVerify,
                        onCheckedChange = { checked ->
                            select { it.copy(behavior = it.behavior.copy(isFeeGrowingOnVerify = checked)) }
                        },
                    ),
                    TesterEarnFlowUM.SwitchUM(
                        title = "Execute fails",
                        isChecked = current.behavior.isExecuteFailing,
                        onCheckedChange = { checked ->
                            select { it.copy(behavior = it.behavior.copy(isExecuteFailing = checked)) }
                        },
                    ),
                ),
                isOpenEnabled = current.target != null,
                onOpenClick = ::openFlow,
                onFlowClose = ::closeFlow,
            )
        }
    }

    private fun select(transform: (Selection) -> Selection) {
        selection.update(transform)
        render()
    }

    private fun openFlow() {
        val current = selection.value
        val target = current.target ?: return
        sessionSettings.behavior = current.behavior
        val params = EarnFlowComponent.Params(
            product = target,
            flowProduct = TesterEarnProducts.flowProduct(current.preset),
            action = current.action.toFlowAction(current.preset),
            source = EarnEntrySource.TokenPage,
            callback = object : EarnFlowComponent.ModelCallback {
                override fun onFinished(exit: EarnFlowExit) {
                    Toast.makeText(context, "Earn flow finished: $exit", Toast.LENGTH_SHORT).show()
                    closeFlow()
                }
            },
        )
        uiState.update { it.copy(launch = params) }
    }

    private fun closeFlow() {
        uiState.update { it.copy(launch = null) }
    }

    private fun TesterEarnAction.toFlowAction(preset: FakeEarnSessionPreset): EarnFlowAction = when (this) {
        TesterEarnAction.Deposit -> Deposit()
        TesterEarnAction.Withdraw -> Withdraw(notice = stringReference(fakeWithdrawNotice(preset)))
        TesterEarnAction.Claim -> fakeClaimAction()
    }

    private enum class TesterEarnAction { Deposit, Withdraw, Claim }

    private data class Selection(
        val target: EarnProduct.Params? = null,
        val targetDescription: String = "Loading the selected wallet…",
        val preset: FakeEarnSessionPreset = FakeEarnSessionPreset.Lending,
        val action: TesterEarnAction = TesterEarnAction.Deposit,
        val behavior: FakeEarnSessionBehavior = FakeEarnSessionBehavior(),
    )
}