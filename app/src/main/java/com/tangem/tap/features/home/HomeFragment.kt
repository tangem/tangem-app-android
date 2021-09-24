package com.tangem.tap.features.home

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.transition.TransitionInflater
import com.tangem.tap.common.redux.navigation.FragmentShareTransition
import com.tangem.tap.common.redux.navigation.ShareElement
import com.tangem.tap.common.toggleWidget.IndeterminateProgressButtonWidget
import com.tangem.tap.common.toggleWidget.ViewStateWidget
import com.tangem.tap.common.transitions.FrontCardEnterTransition
import com.tangem.tap.common.transitions.FrontCardExitTransition
import com.tangem.tap.features.home.redux.HomeAction
import com.tangem.tap.features.home.redux.HomeState
import com.tangem.tap.store
import com.tangem.wallet.R
import kotlinx.android.synthetic.main.fragment_home.*
import org.rekotlin.StoreSubscriber
import java.lang.ref.WeakReference

class HomeFragment : Fragment(R.layout.fragment_home), StoreSubscriber<HomeState> {

    private lateinit var btnScanCard: ViewStateWidget

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val inflater = TransitionInflater.from(requireContext())
        exitTransition = inflater.inflateTransition(R.transition.fade)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val shareTransition = FragmentShareTransition(
                listOf(ShareElement(WeakReference(imv_front_card), imv_front_card.transitionName)),
                FrontCardEnterTransition(),
                FrontCardExitTransition()
        )

        store.dispatch(HomeAction.SetFragmentShareTransition(shareTransition))
        store.dispatch(HomeAction.Init)

        btn_scan_card?.setOnClickListener { store.dispatch(HomeAction.ReadCard) }
        btn_get_new_card?.setOnClickListener { store.dispatch(HomeAction.GoToShop) }

        btnScanCard = IndeterminateProgressButtonWidget(btn_scan_card, progress)
    }

    override fun onStart() {
        super.onStart()
        store.subscribe(this) { state ->
            state.skipRepeats { oldState, newState ->
                oldState.homeState == newState.homeState
            }.select { it.homeState }
        }
    }

    override fun onStop() {
        super.onStop()
        store.unsubscribe(this)
    }


    override fun newState(state: HomeState) {
        if (activity == null) return

        btnScanCard.changeState(state.btnScanState.progressState)
        btnScanCard.mainView.isEnabled = state.btnScanState.enabled
    }

    override fun onDestroyView() {
        store.dispatch(HomeAction.SetFragmentShareTransition(null))
        super.onDestroyView()
    }
}