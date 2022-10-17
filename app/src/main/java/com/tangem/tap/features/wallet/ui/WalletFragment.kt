package com.tangem.tap.features.wallet.ui

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.TransitionInflater
import by.kirich1409.viewbindingdelegate.viewBinding
import coil.load
import coil.size.Scale
import com.tangem.domain.common.TapWorkarounds.isSaltPay
import com.tangem.tap.MainActivity
import com.tangem.tap.common.extensions.show
import com.tangem.tap.common.recyclerView.SpaceItemDecoration
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.common.redux.navigation.AppScreen
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.domain.configurable.warningMessage.WarningMessage
import com.tangem.tap.domain.statePrinter.printScanResponseState
import com.tangem.tap.domain.statePrinter.printWalletState
import com.tangem.tap.domain.termsOfUse.CardTou
import com.tangem.tap.features.details.redux.DetailsAction
import com.tangem.tap.features.wallet.redux.Artwork
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
import com.tangem.wallet.BuildConfig
import com.tangem.wallet.R
import com.tangem.wallet.databinding.FragmentWalletBinding
import org.rekotlin.StoreSubscriber

class WalletFragment : Fragment(R.layout.fragment_wallet), StoreSubscriber<WalletState> {

    private lateinit var warningsAdapter: WarningMessagesAdapter

    private val binding: FragmentWalletBinding by viewBinding(FragmentWalletBinding::bind)

    private var walletView: WalletView = SingleWalletView()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        activity?.onBackPressedDispatcher?.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                store.dispatch(NavigationAction.PopBackTo(AppScreen.Home))
            }
        })
        val inflater = TransitionInflater.from(requireContext())
        enterTransition = inflater.inflateTransition(R.transition.slide_right)
        exitTransition = inflater.inflateTransition(R.transition.fade)
    }

    override fun onStart() {
        super.onStart()
        store.subscribe(this) { state ->
            state.select { it.walletState }
        }
        walletView.setFragment(this, binding)
//        store.dispatch(WalletAction.UpdateWallet(force = false))
//        store.dispatch(WalletAction.LoadWallet())
    }

    override fun onStop() {
        super.onStop()
        store.unsubscribe(this)
        walletView.removeFragment()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as? AppCompatActivity)?.setSupportActionBar(binding.toolbar)

        binding.toolbar.setNavigationOnClickListener {
            store.dispatch(WalletAction.Scan)
        }
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

    private fun setupWarningsRecyclerView() {
        warningsAdapter = WarningMessagesAdapter()
        val layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        with(binding) {
            rvWarningMessages.layoutManager = layoutManager
            rvWarningMessages.addItemDecoration(SpaceItemDecoration.all(16f))
            rvWarningMessages.adapter = warningsAdapter
        }
    }


    override fun newState(state: WalletState) {
        if (activity == null || view == null) return

        val isSaltPay = store.state.globalState.scanResponse?.card?.isSaltPay == true

        when {
            isSaltPay && (walletView !is SaltPaySingleWalletView) -> {
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
            binding.toolbar.inflateMenu(R.menu.wallet)
        }

        setupNoInternetHandling(state)
        setupCardImage(state.cardImage)

        if (!isSaltPay) showWarningsIfPresent(state.mainWarningsList)

        binding.srlWallet.isRefreshing = state.state == ProgressState.Refreshing
        binding.srlWallet.setOnRefreshListener {
            if (state.state != ProgressState.Loading ||
                state.state != ProgressState.Refreshing
            ) {
                store.dispatch(WalletAction.LoadData.Refresh)
            }
        }
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
                    buttonTitle = R.string.common_retry
                ) { store.dispatch(WalletAction.LoadData) }
            }
        } else {
            (activity as? MainActivity)?.dismissSnackbar()
        }
    }

    private fun setupCardImage(cardImage: Artwork?) {
        binding.ivCard.load(cardImage?.artworkId) {
            scale(Scale.FIT)
            crossfade(enable = true)
            placeholder(R.drawable.card_placeholder_black)
            error(R.drawable.card_placeholder_black)
            fallback(R.drawable.card_placeholder_black)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.details_menu -> {
                store.dispatch(GlobalAction.UpdateFeedbackInfo(store.state.walletState.walletManagers))
                store.state.globalState.scanResponse?.let { scanNoteResponse ->
                    store.dispatch(DetailsAction.PrepareScreen(
                        scanNoteResponse,
                        store.state.walletState.walletManagers.map { it.wallet },
                        CardTou(),
                    ))
                    store.dispatch(NavigationAction.NavigateTo(AppScreen.Details))
                    true
                }
                false
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        if (store.state.walletState.shouldShowDetails) inflater.inflate(R.menu.wallet, menu)
    }

}