package com.tangem.tap.features.home

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.transition.TransitionInflater
import com.tangem.tap.features.home.redux.HomeAction
import com.tangem.tap.store
import com.tangem.wallet.R
import kotlinx.android.synthetic.main.fragment_home.*

class HomeFragment : Fragment(R.layout.fragment_home) {

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