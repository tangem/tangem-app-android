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
import com.squareup.picasso.Picasso
import com.tangem.tangem_sdk_new.extensions.dpToPx
import com.tangem.tap.MainActivity
import com.tangem.tap.common.extensions.show
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.common.redux.navigation.AppScreen
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.domain.configurable.warningMessage.WarningMessage
import com.tangem.tap.domain.termsOfUse.CardTou
import com.tangem.tap.features.details.redux.DetailsAction
import com.tangem.tap.features.wallet.redux.*
import com.tangem.tap.features.wallet.ui.adapters.SpacesItemDecoration
import com.tangem.tap.features.wallet.ui.adapters.WarningMessagesAdapter
import com.tangem.tap.features.wallet.ui.wallet.MultiWalletView
import com.tangem.tap.features.wallet.ui.wallet.SingleWalletView
import com.tangem.tap.features.wallet.ui.wallet.WalletView
import com.tangem.tap.store
import com.tangem.wallet.R
import kotlinx.android.synthetic.main.fragment_wallet.*
import kotlinx.android.synthetic.main.fragment_wallet.toolbar
import kotlinx.android.synthetic.main.fragment_wallet_details.*
import org.rekotlin.StoreSubscriber


class WalletFragment : Fragment(R.layout.fragment_wallet), StoreSubscriber<WalletState> {

    private lateinit var warningsAdapter: WarningMessagesAdapter

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
            state.skipRepeats { oldState, newState ->
                oldState.walletState == newState.walletState
            }.select { it.walletState }
        }
        walletView.setFragment(this)
        store.dispatch(WalletAction.UpdateWallet())
    }

    override fun onStop() {
        super.onStop()
        store.unsubscribe(this)
        walletView.removeFragment()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as? AppCompatActivity)?.setSupportActionBar(toolbar)

        toolbar.setNavigationOnClickListener {
            store.dispatch(NavigationAction.PopBackTo(AppScreen.Home))
        }
        setupWarningsRecyclerView()
        walletView.changeWalletView(this)
    }

    private fun setupWarningsRecyclerView() {
        warningsAdapter = WarningMessagesAdapter()
        val layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        rv_warning_messages.layoutManager = layoutManager
        rv_warning_messages.addItemDecoration(SpacesItemDecoration(rv_warning_messages.dpToPx(16f).toInt()))
        rv_warning_messages.adapter = warningsAdapter
    }


    override fun newState(state: WalletState) {
        if (activity == null) return

        if (state.isMultiwalletAllowed &&
            state.primaryWallet?.currencyData?.status != BalanceStatus.EmptyCard &&
            walletView is SingleWalletView
        ) {
            walletView = MultiWalletView()
            walletView.changeWalletView(this)
        } else if (!state.isMultiwalletAllowed && walletView is MultiWalletView) {
            walletView = SingleWalletView()
            walletView.changeWalletView(this)
        }
        walletView.onNewState(state)

        if (!state.shouldShowDetails) {
            toolbar.menu.removeItem(R.id.details_menu)
        } else if (toolbar.menu.findItem(R.id.details_menu) == null) {
            toolbar.inflateMenu(R.menu.wallet)
        }

        setupNoInternetHandling(state)
        setupCardImage(state.cardImage)

        showWarningsIfPresent(state.mainWarningsList)

        srl_wallet.setOnRefreshListener {
            if (state.state != ProgressState.Loading) {
                store.dispatch(WalletAction.LoadData)
            }
        }

        if (state.state != ProgressState.Loading) {
            srl_wallet.isRefreshing = false
        }
    }

    private fun showWarningsIfPresent(warnings: List<WarningMessage>) {
        warningsAdapter.submitList(warnings)
        rv_warning_messages.show(warnings.isNotEmpty())
    }

    private fun setupNoInternetHandling(state: WalletState) {
        if (state.state == ProgressState.Error) {
            if (state.error == ErrorType.NoInternetConnection) {
                srl_wallet_details?.isRefreshing = false
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
        Picasso.get()
                .load(cardImage?.artworkId)
                .placeholder(R.drawable.card_placeholder)
                ?.error(R.drawable.card_placeholder)
                ?.into(iv_card)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.details_menu -> {
                store.dispatch(GlobalAction.UpdateFeedbackInfo(store.state.walletState.walletManagers))
                store.state.globalState.scanResponse?.let { scanNoteResponse ->
                    store.dispatch(DetailsAction.PrepareScreen(
                            scanNoteResponse.card, scanNoteResponse,
                            store.state.walletState.walletManagers.map { it.wallet },
                            store.state.globalState.configManager?.config?.isCreatingTwinCardsAllowed,
                            CardTou(),
                            store.state.globalState.appCurrency
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