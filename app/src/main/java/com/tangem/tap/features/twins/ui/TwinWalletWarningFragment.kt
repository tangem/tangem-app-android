package com.tangem.tap.features.twins.ui

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.transition.TransitionInflater
import com.squareup.picasso.Picasso
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.features.twins.redux.CreateTwinWalletMode
import com.tangem.tap.features.twins.redux.TwinCardsAction
import com.tangem.tap.features.wallet.redux.Artwork
import com.tangem.tap.store
import com.tangem.wallet.R
import kotlinx.android.synthetic.main.fragment_twin_cards_warning.*
import kotlinx.android.synthetic.main.layout_twin_cards_orange.*

class TwinWalletWarningFragment : Fragment(R.layout.fragment_twin_cards_warning) {

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
        val createTwinWallet = store.state.twinCardsState.createWalletState?.mode
        if (createTwinWallet == CreateTwinWalletMode.CreateWallet) {
            tv_twin_cards_description.text = getText(R.string.details_twins_recreate_subtitle)
        } else if (createTwinWallet == CreateTwinWalletMode.RecreateWallet) {
            tv_twin_cards_description.text = getText(R.string.details_twins_recreate_warning)
        }

        btn_cancel.setOnClickListener { store.dispatch(NavigationAction.PopBackTo()) }
        btn_start.setOnClickListener { store.dispatch(TwinCardsAction.CreateWallet.Proceed) }
        Picasso.get()
                .load(Artwork.TWIN_CARD_1)
                .placeholder(R.drawable.card_placeholder)
                ?.error(R.drawable.card_placeholder)
                ?.into(iv_twin_card_1)

        Picasso.get()
                .load(Artwork.TWIN_CARD_2)
                .placeholder(R.drawable.card_placeholder)
                ?.error(R.drawable.card_placeholder)
                ?.into(iv_twin_card_2)
    }


}