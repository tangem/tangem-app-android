package com.tangem.tap.features.wallet.ui

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.mutableStateOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.TransitionInflater
import by.kirich1409.viewbindingdelegate.viewBinding
import coil.load
import coil.size.Scale
import com.badoo.mvicore.modelWatcher
import com.tangem.core.analytics.Analytics
import com.tangem.core.ui.fragments.setStatusBarColor
import com.tangem.core.ui.utils.OneTouchClickListener
import com.tangem.datasource.connection.NetworkConnectionManager
import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.feature.swap.api.SwapFeatureToggleManager
import com.tangem.feature.swap.domain.SwapInteractor
import com.tangem.tap.MainActivity
import com.tangem.tap.common.analytics.events.Portfolio
import com.tangem.tap.common.extensions.show
import com.tangem.tap.common.recyclerView.SpaceItemDecoration
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.common.redux.navigation.AppScreen
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.common.utils.SafeStoreSubscriber
import com.tangem.tap.domain.configurable.warningMessage.WarningMessage
import com.tangem.tap.domain.statePrinter.printScanResponseState
import com.tangem.tap.domain.statePrinter.printWalletState
import com.tangem.tap.features.details.redux.DetailsAction
import com.tangem.tap.features.wallet.redux.ErrorType
import com.tangem.tap.features.wallet.redux.ProgressState
import com.tangem.tap.features.wallet.redux.WalletAction
import com.tangem.tap.features.wallet.redux.WalletState
import com.tangem.tap.features.wallet.ui.adapters.WarningMessagesAdapter
import com.tangem.tap.features.wallet.ui.wallet.MultiWalletView
import com.tangem.tap.features.wallet.ui.wallet.SingleWalletView
import com.tangem.tap.features.wallet.ui.wallet.WalletView
import com.tangem.tap.features.wallet.ui.wallet.saltPay.SaltPayWalletView
import com.tangem.tap.store
import com.tangem.wallet.BuildConfig
import com.tangem.wallet.R
import com.tangem.wallet.databinding.FragmentWalletBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class WalletFragment : Fragment(R.layout.fragment_wallet), SafeStoreSubscriber<WalletState> {

    @Inject
    lateinit var swapInteractor: SwapInteractor

    @Inject
    lateinit var swapFeatureToggleManager: SwapFeatureToggleManager

    @Inject
    lateinit var networkConnectionManager: NetworkConnectionManager

    private lateinit var warningsAdapter: WarningMessagesAdapter

    private val binding: FragmentWalletBinding by viewBinding(FragmentWalletBinding::bind)

    private var walletView: WalletView = MultiWalletView()

    private val viewModel by viewModels<WalletViewModel>()

    private val totalBalanceWatcher = modelWatcher {
        (WalletState::totalBalance) { totalBalance ->
            totalBalance?.let {
                viewModel.onBalanceLoaded(totalBalance)
                store.state.globalState.topUpController?.totalBalanceStateChanged(it)
            }
        }
    }

    private val isNetworkConnectionError = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        activity?.lifecycle?.addObserver(viewModel)

        activity?.onBackPressedDispatcher?.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    store.dispatch(WalletAction.PopBackToInitialScreen)
                }
            },
        )
        val inflater = TransitionInflater.from(requireContext())
        enterTransition = inflater.inflateTransition(R.transition.slide_right)
        exitTransition = inflater.inflateTransition(R.transition.fade)
    }

    override fun onStart() {
        super.onStart()

        setStatusBarColor(R.color.background_secondary)

        subscribeOnNetworkStateChanging()

        store.subscribe(this) { state ->
            state.select { it.walletState }
        }
        walletView.setFragment(this, binding)
    }

    override fun onStop() {
        super.onStop()
        store.unsubscribe(this)
        walletView.removeFragment()
    }

    override fun onDestroy() {
        walletView.onDestroyFragment()
        super.onDestroy()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as? AppCompatActivity)?.setSupportActionBar(binding.toolbar)

        binding.toolbar.setNavigationOnClickListener(
            OneTouchClickListener { store.dispatch(WalletAction.ChangeWallet) },
        )
        setupWarningsRecyclerView()
        walletView.changeWalletView(this, binding)
        addCustomActionOnCard()
    }

    private fun addCustomActionOnCard() {
        if (!BuildConfig.TEST_ACTION_ENABLED) return

        binding.ivCard.setOnClickListener {
            printScanResponseState()
            printWalletState()
        }
    }

    @Suppress("MagicNumber")
    private fun setupWarningsRecyclerView() {
        warningsAdapter = WarningMessagesAdapter()
        val layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        with(binding) {
            rvWarningMessages.layoutManager = layoutManager
            rvWarningMessages.addItemDecoration(SpaceItemDecoration.all(16f))
            rvWarningMessages.adapter = warningsAdapter
        }
    }

    @Suppress("ComplexMethod")
    override fun newStateOnMain(state: WalletState) {
        if (activity == null || view == null) return

        val isSaltPay = store.state.globalState.scanResponse?.cardTypesResolver?.isSaltPay() == true

        when {
            isSaltPay && walletView !is SaltPayWalletView -> {
                walletView.onViewDestroy()
                walletView = SaltPayWalletView()
                walletView.changeWalletView(this, binding)
            }
            state.isMultiwalletAllowed && walletView !is MultiWalletView -> {
                walletView.onViewDestroy()
                walletView = MultiWalletView()
                walletView.changeWalletView(this, binding)
            }
            !state.isMultiwalletAllowed && !isSaltPay && walletView !is SingleWalletView -> {
                walletView.onViewDestroy()
                walletView = SingleWalletView()
                walletView.changeWalletView(this, binding)
            }
            else -> {} // we keep the same view unless we scan a card that requires a different view
        }
        totalBalanceWatcher.invoke(state)

        walletView.swapInteractor = swapInteractor
        walletView.swapFeatureToggleManager = swapFeatureToggleManager

        walletView.onNewState(state)

        if (!state.shouldShowDetails) {
            binding.toolbar.menu.removeItem(R.id.details_menu)
        } else if (binding.toolbar.menu.findItem(R.id.details_menu) == null) {
            binding.toolbar.inflateMenu(R.menu.menu_wallet)
        }

        setupNoInternetHandling(state)
        setupCardImage(state, isSaltPay)

        if (!isSaltPay) showWarningsIfPresent(state.mainWarningsList)

        binding.srlWallet.isRefreshing = state.state == ProgressState.Refreshing
        binding.srlWallet.setOnRefreshListener {
            if (state.state != ProgressState.Loading &&
                state.state != ProgressState.Refreshing
            ) {
                refreshWalletData()
            }
        }

        val navigationIconRes = if (state.canSaveUserWallets) {
            R.drawable.ic_wallet_24
        } else {
            R.drawable.ic_tap_card_24
        }
        binding.toolbar.setNavigationIcon(navigationIconRes)
    }

    private fun refreshWalletData() {
        Analytics.send(Portfolio.Refreshed())
        store.dispatch(WalletAction.LoadData.Refresh)
    }

    private fun showWarningsIfPresent(warnings: List<WarningMessage>) {
        warningsAdapter.submitList(warnings)
        binding.rvWarningMessages.show(warnings.isNotEmpty())
    }

    private fun setupNoInternetHandling(state: WalletState) {
        if (state.state == ProgressState.Error) {
            if (state.error == ErrorType.NoInternetConnection) {
                isNetworkConnectionError.value = true
                binding.srlWallet.isRefreshing = false
                (activity as? MainActivity)?.showSnackbar(
                    text = R.string.wallet_notification_no_internet,
                    buttonTitle = R.string.common_retry,
                )
                // because was added logic of autoupdate mainscreen data, remove retry
                // TODO("remove comment after release 4.6")
                // { store.dispatch(WalletAction.LoadData) }
            } else {
                isNetworkConnectionError.value = false
            }
        } else {
            isNetworkConnectionError.value = false
            (activity as? MainActivity)?.dismissSnackbar()
        }
    }

    private fun setupCardImage(state: WalletState, isSaltPay: Boolean) {
        // TODO: SaltPay: remove hardCode
        if (isSaltPay) {
            binding.ivCard.load(R.drawable.img_salt_pay_visa) {
                scale(Scale.FIT)
                crossfade(enable = true)
                placeholder(R.drawable.card_placeholder_black)
                error(R.drawable.card_placeholder_black)
                fallback(R.drawable.card_placeholder_black)
            }
        } else {
            binding.ivCard.load(state.cardImage?.artworkId) {
                scale(Scale.FIT)
                crossfade(enable = true)
                placeholder(R.drawable.card_placeholder_black)
                error(R.drawable.card_placeholder_black)
                fallback(R.drawable.card_placeholder_black)
            }
        }
    }

    private fun subscribeOnNetworkStateChanging() {
        viewLifecycleOwner.lifecycleScope.launch {
            networkConnectionManager.isOnlineFlow
                .flowWithLifecycle(viewLifecycleOwner.lifecycle, Lifecycle.State.STARTED)
                .distinctUntilChanged()
                .collect { isOnline ->
                    if (isOnline && isNetworkConnectionError.value) {
                        refreshWalletData()
                    }
                }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.details_menu -> {
                store.dispatch(GlobalAction.UpdateFeedbackInfo(store.state.walletState.walletManagers))
                store.state.globalState.scanResponse?.let { scanResponse ->
                    store.dispatch(
                        DetailsAction.PrepareScreen(
                            scanResponse = scanResponse,
                            wallets = store.state.walletState.walletManagers.map { it.wallet },
                        ),
                    )
                    store.dispatch(NavigationAction.NavigateTo(AppScreen.Details))
                    true
                }
                false
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        if (store.state.walletState.shouldShowDetails) inflater.inflate(R.menu.menu_wallet, menu)
    }
}