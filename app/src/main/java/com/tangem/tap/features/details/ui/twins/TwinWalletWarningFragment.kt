package com.tangem.tap.features.details.ui.twins

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.transition.TransitionInflater
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.features.details.redux.DetailsAction
import com.tangem.tap.features.details.redux.twins.CreateTwinWallet
import com.tangem.tap.store
import com.tangem.wallet.R
import kotlinx.android.synthetic.main.fragment_details_twin_cards_warning.*

class TwinWalletWarningFragment : Fragment(R.layout.fragment_details_twin_cards_warning) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity?.onBackPressedDispatcher?.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                store.dispatch(NavigationAction.PopBackTo())
            }
        })
        val inflater = TransitionInflater.from(requireContext())
        enterTransition = inflater.inflateTransition(R.transition.slide_right)
        exitTransition = inflater.inflateTransition(R.transition.fade)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        btn_cancel.setOnClickListener { store.dispatch(NavigationAction.PopBackTo()) }
        btn_start.setOnClickListener {
            store.dispatch(
                    DetailsAction.CreateTwinWalletAction.Proceed(
                            null, createTwinWallet = CreateTwinWallet.RecreateWallet
                    ))
        }
    }


}