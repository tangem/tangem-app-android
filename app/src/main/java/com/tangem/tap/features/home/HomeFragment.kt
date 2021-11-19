package com.tangem.tap.features.home

import android.os.Bundle
import android.view.View
import androidx.transition.TransitionInflater
import com.tangem.tap.common.extensions.hide
import com.tangem.tap.common.redux.navigation.FragmentShareTransition
import com.tangem.tap.common.redux.navigation.ShareElement
import com.tangem.tap.common.toggleWidget.IndeterminateProgressButtonWidget
import com.tangem.tap.common.toggleWidget.ViewStateWidget
import com.tangem.tap.common.transitions.FrontCardEnterTransition
import com.tangem.tap.common.transitions.FrontCardExitTransition
import com.tangem.tap.features.home.redux.HomeAction
import com.tangem.tap.features.home.redux.HomeState
import com.tangem.tap.features.onboarding.products.BaseOnboardingFragment
import com.tangem.tap.store
import com.tangem.wallet.R
import kotlinx.android.synthetic.main.fragment_onboarding_main.*
import kotlinx.android.synthetic.main.layout_onboarding_container_bottom.*
import kotlinx.android.synthetic.main.layout_onboarding_home_top.*
import kotlinx.android.synthetic.main.view_bg_home.*

class HomeFragment : BaseOnboardingFragment<HomeState>() {

    private lateinit var btnScanCard: ViewStateWidget

    override fun getOnboardingTopContainerId(): Int = R.layout.layout_onboarding_home_top

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val inflater = TransitionInflater.from(requireContext())
        exitTransition = inflater.inflateTransition(R.transition.fade)
        store.dispatch(HomeAction.Init)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbar.hide()
        val shareTransition = FragmentShareTransition(
            listOf(
                ShareElement(imv_front_card, ShareElement.imvFrontCard),
                ShareElement(imv_back_card, ShareElement.imvBackCard),
                ShareElement(bg_circle_large),
                ShareElement(bg_circle_medium),
                ShareElement(bg_circle_min),
            ),
            FrontCardEnterTransition(),
            FrontCardExitTransition()
        )

        store.dispatch(HomeAction.SetFragmentShareTransition(shareTransition))

        tv_header.setText(R.string.home_welcome_header)
        tv_body.setText(R.string.home_welcome_body)

        btn_main_action.setText(R.string.home_button_scan)
        btn_main_action.setOnClickListener { store.dispatch(HomeAction.ReadCard) }
        btn_alternative_action.setText(R.string.home_button_get_new_card)
        btn_alternative_action.setOnClickListener { store.dispatch(HomeAction.GoToShop) }

        btnScanCard = IndeterminateProgressButtonWidget(btn_main_action, progress)
    }

    override fun subscribeToStore() {
        store.subscribe(this) { state ->
            state.skipRepeats { oldState, newState ->
                oldState.homeState == newState.homeState
            }.select { it.homeState }
        }
        storeSubscribersList.add(this)
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