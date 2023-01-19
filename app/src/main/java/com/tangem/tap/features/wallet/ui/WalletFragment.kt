package com.tangem.tap.features.wallet.ui

import android.content.Context
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.TransitionInflater
import by.kirich1409.viewbindingdelegate.viewBinding
import coil.load
import coil.size.Scale
import com.tangem.core.analytics.Analytics
import com.tangem.core.ui.fragments.setStatusBarColor
import com.tangem.core.ui.utils.OneTouchClickListener
import com.tangem.domain.common.TapWorkarounds.isSaltPay
import com.tangem.tap.MainActivity
import com.tangem.tap.common.analytics.converters.BasicSignInEventConverter
import com.tangem.tap.common.analytics.converters.BasicTopUpEventConverter
import com.tangem.tap.common.analytics.events.MainScreen
import com.tangem.tap.common.analytics.events.Portfolio
import com.tangem.tap.common.extensions.show
import com.tangem.tap.common.recyclerView.SpaceItemDecoration
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.common.redux.navigation.AppScreen
import com.tangem.tap.common.redux.navigation.NavigationAction
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
import com.tangem.tap.features.wallet.ui.wallet.SaltPaySingleWalletView
import com.tangem.tap.features.wallet.ui.wallet.SingleWalletView
import com.tangem.tap.features.wallet.ui.wallet.WalletView
import com.tangem.tap.store
import com.tangem.tap.userWalletsListManager
import com.tangem.wallet.BuildConfig
import com.tangem.wallet.R
import com.tangem.wallet.databinding.FragmentWalletBinding
import org.rekotlin.StoreSubscriber

class WalletFragment : Fragment(R.layout.fragment_wallet), StoreSubscriber<WalletState> {

    private lateinit var warningsAdapter: WarningMessagesAdapter

    private val binding: FragmentWalletBinding by viewBinding(FragmentWalletBinding::bind)

    private var walletView: WalletView = SingleWalletView()

    private val viewModel by viewModels<WalletViewModel>()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity?.lifecycleScope?.launchWhenCreated {
            viewModel.launch()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        Analytics.send(MainScreen.ScreenOpened())
        activity?.onBackPressedDispatcher?.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    val popBackTo = if (userWalletsListManager.hasSavedUserWallets) {
                        userWalletsListManager.lock()
                        AppScreen.Welcome
                    } else {
                        AppScreen.Home
                    }
                    store.dispatch(NavigationAction.PopBackTo(popBackTo))
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
    override fun newState(state: WalletState) {
        if (activity == null || view == null) return

        handleBasicAnalyticsEvent(state)
        val isSaltPay = store.state.globalState.scanResponse?.card?.isSaltPay == true

        when {
            isSaltPay && walletView !is SaltPaySingleWalletView -> {
                walletView = SaltPaySingleWalletView()
                walletView.changeWalletView(this, binding)
            }
            state.isMultiwalletAllowed && state.primaryWallet?.currencyData?.status != BalanceStatus.EmptyCard &&
                walletView !is MultiWalletView -> {
                walletView = MultiWalletView()
                walletView.changeWalletView(this, binding)
            }
            !state.isMultiwalletAllowed && !isSaltPay && walletView !is SingleWalletView -> {
                walletView = SingleWalletView()
                walletView.changeWalletView(this, binding)
            }
            else -> {} // we keep the same view unless we scan a card that requires a different view
        }

        walletView.onNewState(state)

        if (!state.shouldShowDetails) {
            binding.toolbar.menu.removeItem(R.id.details_menu)
        } else if (binding.toolbar.menu.findItem(R.id.details_menu) == null) {
            binding.toolbar.inflateMenu(R.menu.menu_wallet)
        }

        setupNoInternetHandling(state)
        setupCardImage(state)

        if (!isSaltPay) showWarningsIfPresent(state.mainWarningsList)

        binding.srlWallet.isRefreshing = state.state == ProgressState.Refreshing
        binding.srlWallet.setOnRefreshListener {
            if (state.state != ProgressState.Loading &&
                state.state != ProgressState.Refreshing
            ) {
                Analytics.send(Portfolio.Refreshed())
                store.dispatch(WalletAction.LoadData.Refresh)
            }
        }

        val navigationIconRes = if (state.hasSavedWallets) R.drawable.ic_wallet_24 else R.drawable.ic_tap_card_24
        binding.toolbar.setNavigationIcon(navigationIconRes)
    }

    private fun showWarningsIfPresent(warnings: List<WarningMessage>) {
        warningsAdapter.submitList(warnings)
        binding.rvWarningMessages.show(warnings.isNotEmpty())
    }

    private fun setupNoInternetHandling(state: WalletState) {
        if (state.state == ProgressState.Error) {
            if (state.error == ErrorType.NoInternetConnection) {
                binding.srlWallet.isRefreshing = false
                (activity as? MainActivity)?.showSnackbar(
                    text = R.string.wallet_notification_no_internet,
                    buttonTitle = R.string.common_retry,
                ) { store.dispatch(WalletAction.LoadData) }
            }
        } else {
            (activity as? MainActivity)?.dismissSnackbar()
        }
    }

    private fun setupCardImage(state: WalletState) {
        // TODO: SaltPay: remove hardCode
        if (store.state.globalState.scanResponse?.cardTypesResolver?.isSaltPay() == true) {
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.details_menu -> {
                store.dispatch(GlobalAction.UpdateFeedbackInfo(store.state.walletState.walletManagers))
                store.state.globalState.scanResponse?.let { scanNoteResponse ->
                    store.dispatch(
                        DetailsAction.PrepareScreen(
                            scanResponse = scanNoteResponse,
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

    private fun handleBasicAnalyticsEvent(state: WalletState) {
        val scanResponse = store.state.globalState.scanResponse ?: return

        BasicSignInEventConverter(scanResponse).convert(state)?.let { Analytics.send(it) }
        BasicTopUpEventConverter(scanResponse).convert(state)?.let { Analytics.send(it) }
    }
}
