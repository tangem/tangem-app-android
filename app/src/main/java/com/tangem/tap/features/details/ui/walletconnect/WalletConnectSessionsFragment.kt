package com.tangem.tap.features.details.ui.walletconnect

import android.app.Dialog
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.transition.TransitionInflater
import com.tangem.tap.common.extensions.show
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.features.details.redux.walletconnect.WalletConnectAction
import com.tangem.tap.features.details.redux.walletconnect.WalletConnectState
import com.tangem.tap.store
import com.tangem.wallet.R
import kotlinx.android.synthetic.main.fragment_wallet_connect_sessions.*
import org.rekotlin.StoreSubscriber


class WalletConnectSessionsFragment : Fragment(R.layout.fragment_wallet_connect_sessions),
    StoreSubscriber<WalletConnectState> {

    private lateinit var walletConnectSessionsAdapter: WalletConnectSessionsAdapter
    private var dialog: Dialog? = null


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
        toolbar.setNavigationOnClickListener {
            store.dispatch(NavigationAction.PopBackTo())
        }
        setOnClickListeners()
        setupTransactionsRecyclerView()
    }

    private fun setOnClickListeners() {
        fab_open_session.setOnClickListener {
            store.dispatch(WalletConnectAction.StartWalletConnect(activity = requireActivity()))
        }
    }

    private fun setupTransactionsRecyclerView() {
        walletConnectSessionsAdapter = WalletConnectSessionsAdapter()
        rv_wallet_connect_sessions.layoutManager = LinearLayoutManager(requireContext())
        rv_wallet_connect_sessions.adapter = walletConnectSessionsAdapter
        walletConnectSessionsAdapter.submitList(store.state.walletConnectState.sessions)
    }

    override fun newState(state: WalletConnectState) {
        if (activity == null) return

        fab_open_session.show(!state.loading)
        pb_wallet_connect.show(state.loading)

        walletConnectSessionsAdapter.submitList(state.sessions)
        rv_wallet_connect_sessions.show(state.sessions.isNotEmpty())
        tv_no_sessions.show(state.sessions.isEmpty())
        tv_no_sessions_title.show(state.sessions.isEmpty())
    }
}