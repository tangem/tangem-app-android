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
            store.dispatch(NavigationAction.PopBackTo(AppScreen.Home))
        }
        setupWarningsRecyclerView()
        walletView.changeWalletView(this, binding)
    }

    private fun setupWarningsRecyclerView() {
        warningsAdapter = WarningMessagesAdapter()
        val layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        with(binding) {
            rvWarningMessages.layoutManager = layoutManager
            rvWarningMessages.addItemDecoration(SpacesItemDecoration(rvWarningMessages.dpToPx(16f).toInt()))
            rvWarningMessages.adapter = warningsAdapter
        }
    }


    override fun newState(state: WalletState) {
        if (activity == null || view == null) return

        if (state.isMultiwalletAllowed &&
            state.primaryWallet?.currencyData?.status != BalanceStatus.EmptyCard &&
            walletView is SingleWalletView
        ) {
            walletView = MultiWalletView()
            walletView.changeWalletView(this, binding)
        } else if (!state.isMultiwalletAllowed && walletView is MultiWalletView) {
            walletView = SingleWalletView()
            walletView.changeWalletView(this, binding)
        }
        walletView.onNewState(state)

        if (!state.shouldShowDetails) {
            binding.toolbar.menu.removeItem(R.id.details_menu)
        } else if (binding.toolbar.menu.findItem(R.id.details_menu) == null) {
            binding.toolbar.inflateMenu(R.menu.wallet)
        }

        setupNoInternetHandling(state)
        setupCardImage(state.cardImage)

        showWarningsIfPresent(state.mainWarningsList)

        binding.srlWallet.setOnRefreshListener {
            if (state.state != ProgressState.Loading) {
                store.dispatch(WalletAction.LoadData)
            }
        }

        if (state.state != ProgressState.Loading) {
            binding.srlWallet.isRefreshing = false
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
        Picasso.get()
                .load(cardImage?.artworkId)
                .placeholder(R.drawable.card_placeholder_black)
                ?.error(R.drawable.card_placeholder_black)
                ?.into(binding.ivCard)
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
                        store.state.globalState.appCurrency,
                        tangemTechService = store.state.domainNetworks.tangemTechService
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
