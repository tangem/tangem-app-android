package com.tangem.tap.features.home

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.transition.TransitionInflater
import com.tangem.tap.features.home.redux.HomeAction
import com.tangem.tap.features.home.redux.HomeState
import com.tangem.tap.store
import com.tangem.wallet.R
import kotlinx.android.synthetic.main.fragment_home.*
import org.rekotlin.StoreSubscriber

class HomeFragment : Fragment(R.layout.fragment_home), StoreSubscriber<HomeState> {

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
        if (state.firstLaunch) {
            tv_home_description.text = getText(R.string.home_welcome)
            btn_shop.text = getText(R.string.home_button_no_card)
            btn_yes.text = getText(R.string.home_button_yes)
        } else {
            tv_home_description.text = getText(R.string.home_welcome_back)
            btn_shop.text = getText(R.string.home_button_shop)
            btn_yes.text = getText(R.string.home_button_scan)
        }
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btn_yes?.setOnClickListener { store.dispatch(HomeAction.ReadCard) }
        btn_shop?.setOnClickListener { store.dispatch(HomeAction.GoToShop(requireContext())) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val inflater = TransitionInflater.from(requireContext())
        exitTransition = inflater.inflateTransition(R.transition.fade)
    }


}