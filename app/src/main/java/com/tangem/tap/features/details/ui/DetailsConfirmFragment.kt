package com.tangem.tap.features.details.ui

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.transition.TransitionInflater
import com.tangem.tap.common.extensions.getDrawable
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.features.details.redux.ConfirmScreenState
import com.tangem.tap.features.details.redux.DetailsAction
import com.tangem.tap.features.details.redux.DetailsState
import com.tangem.tap.store
import com.tangem.wallet.R
import kotlinx.android.synthetic.main.fragment_details_confirm.*
import org.rekotlin.StoreSubscriber

class DetailsConfirmFragment : Fragment(R.layout.fragment_details_confirm),
        StoreSubscriber<DetailsState> {

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

    override fun onStart() {
        super.onStart()
        store.subscribe(this) { state ->
            state.skipRepeats { oldState, newState ->
                oldState.detailsState == newState.detailsState
            }.select { it.detailsState }
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
    }


    override fun newState(state: DetailsState) {
        if (activity == null) return

        when (state.confirmScreenState) {
            ConfirmScreenState.EraseWallet -> {
                toolbar.title = getString(R.string.details_row_title_erase_wallet)
                tv_warning_description.text = getString(R.string.details_erase_wallet_warning)
                btn_confirm.text = getString(R.string.details_row_title_erase_wallet)
                btn_confirm.setCompoundDrawablesRelativeWithIntrinsicBounds(
                        null, null, getDrawable(R.drawable.ic_send), null
                )
                btn_confirm.setOnClickListener { store.dispatch(DetailsAction.EraseWallet.Confirm) }
            }
            ConfirmScreenState.LongTap, ConfirmScreenState.AccessCode,
            ConfirmScreenState.PassCode -> {
                toolbar.title = getString(R.string.details_row_title_manage_security)
                tv_warning_description.text = getString(R.string.details_security_management_warning)
                btn_confirm.text = getString(R.string.common_save_changes)
                btn_confirm.setCompoundDrawablesRelativeWithIntrinsicBounds(
                        null, null, getDrawable(R.drawable.ic_save), null
                )
                btn_confirm.setOnClickListener { store.dispatch(DetailsAction.ManageSecurity.SaveChanges) }
            }
        }


    }

}
