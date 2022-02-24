package com.tangem.tap.features.details.ui.walletconnect

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.transition.TransitionInflater
import by.kirich1409.viewbindingdelegate.viewBinding
import com.tangem.tap.common.extensions.show
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.features.details.redux.walletconnect.WalletConnectAction
import com.tangem.tap.features.details.redux.walletconnect.WalletConnectSession
import com.tangem.tap.features.details.redux.walletconnect.WalletConnectState
import com.tangem.tap.store
import com.tangem.wallet.R
import com.tangem.wallet.databinding.FragmentWalletConnectSessionsBinding
import org.rekotlin.StoreSubscriber


class WalletConnectSessionsFragment : Fragment(R.layout.fragment_wallet_connect_sessions),
    StoreSubscriber<WalletConnectState> {

    private lateinit var walletConnectSessionsAdapter: WalletConnectSessionsAdapter
    private val binding: FragmentWalletConnectSessionsBinding by viewBinding(
        FragmentWalletConnectSessionsBinding::bind
    )


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity?.onBackPressedDispatcher?.addCallback(
            this, object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    store.dispatch(NavigationAction.PopBackTo())
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
                oldState.walletConnectState == newState.walletConnectState
            }.select { it.walletConnectState }
        }
    }

    override fun onStop() {
        super.onStop()
        store.unsubscribe(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.setNavigationOnClickListener {
            store.dispatch(NavigationAction.PopBackTo())
        }
        setOnClickListeners()
        setupTransactionsRecyclerView()
    }

    private fun setOnClickListeners() {
        binding.fabOpenSession.setOnClickListener {
            store.dispatch(WalletConnectAction.StartWalletConnect(activity = requireActivity()))
        }
    }

    private fun setupTransactionsRecyclerView() = with(binding) {
        walletConnectSessionsAdapter = WalletConnectSessionsAdapter()
        rvWalletConnectSessions.layoutManager = LinearLayoutManager(requireContext())
        rvWalletConnectSessions.adapter = walletConnectSessionsAdapter
        walletConnectSessionsAdapter.submitList(store.state.walletConnectState.sessions)
    }

    override fun newState(state: WalletConnectState) {
        if (activity == null || view == null) return

        binding.fabOpenSession.show(!state.loading)
        binding.pbWalletConnect.show(state.loading)

        showUpdatedList(state.sessions)
    }

    private fun showUpdatedList(sessions: List<WalletConnectSession>) = with(binding) {
        walletConnectSessionsAdapter.submitList(sessions)
        rvWalletConnectSessions.show(sessions.isNotEmpty())
        tvNoSessions.show(sessions.isEmpty())
        tvNoSessionsTitle.show(sessions.isEmpty())
    }
}