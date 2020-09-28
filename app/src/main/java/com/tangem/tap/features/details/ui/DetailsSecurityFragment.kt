package com.tangem.tap.features.details.ui

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.transition.TransitionInflater
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.features.details.redux.DetailsAction
import com.tangem.tap.features.details.redux.DetailsState
import com.tangem.tap.features.details.redux.SecurityOption
import com.tangem.tap.store
import com.tangem.wallet.R
import kotlinx.android.synthetic.main.fragment_details_confirm.toolbar
import kotlinx.android.synthetic.main.fragment_details_security.*
import org.rekotlin.StoreSubscriber
import java.util.*

class DetailsSecurityFragment : Fragment(R.layout.fragment_details_security),
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
        setOnClickListeners()
    }

    private fun setOnClickListeners() {
        v_long_tap.setOnClickListener {
            store.dispatch(DetailsAction.ManageSecurity.SelectOption(SecurityOption.LongTap))
        }
        v_passcode.setOnClickListener {
            store.dispatch(DetailsAction.ManageSecurity.SelectOption(SecurityOption.PassCode))
        }
        v_access_code.setOnClickListener {
            store.dispatch(DetailsAction.ManageSecurity.SelectOption(SecurityOption.AccessCode))
        }
        btn_save_changes.setOnClickListener {
            store.state.detailsState.securityScreenState?.selectedOption?.let {
                store.dispatch(DetailsAction.ManageSecurity.ConfirmSelection(it))
            }
        }
    }

    override fun newState(state: DetailsState) {
        if (activity == null) return
        selectSecurityOption(state.securityScreenState?.selectedOption)
        for (option in SecurityOption.values()) {
            enableOptions(
                    option,
                    state.securityScreenState?.allowedOptions
                            ?: EnumSet.noneOf(SecurityOption::class.java)
            )
        }
    }

    private fun selectSecurityOption(securityOption: SecurityOption?) {
        radiobutton_long_tap.isChecked =
                securityOption == SecurityOption.LongTap && radiobutton_long_tap.isEnabled
        radiobutton_passcode.isChecked =
                securityOption == SecurityOption.PassCode && radiobutton_passcode.isEnabled
        radiobutton_access_code.isChecked =
                securityOption == SecurityOption.AccessCode && radiobutton_access_code.isEnabled
    }

    private fun enableOptions(option: SecurityOption, allowedOptions: EnumSet<SecurityOption>) {
        when (option) {
            SecurityOption.LongTap -> enableLongTap(allowedOptions.contains(option))
            SecurityOption.PassCode -> enablePasscode(allowedOptions.contains(option))
            SecurityOption.AccessCode -> enableAccessCode(allowedOptions.contains(option))
        }
    }

    private fun enableLongTap(enable: Boolean) {
        val alpha = if (enable) 1f else 0.5f
        tv_long_tap_description.alpha = alpha
        tv_long_tap_title.alpha = alpha
        radiobutton_long_tap.alpha = alpha
        radiobutton_long_tap.isEnabled = enable
    }

    private fun enablePasscode(enable: Boolean) {
        val alpha = if (enable) 1f else 0.5f
        tv_passcode_description.alpha = alpha
        tv_passcode_title.alpha = alpha
        radiobutton_passcode.alpha = alpha
        radiobutton_passcode.isEnabled = enable
    }

    private fun enableAccessCode(enable: Boolean) {
        val alpha = if (enable) 1f else 0.5f
        tv_access_code_description.alpha = alpha
        tv_access_code_title.alpha = alpha
        radiobutton_access_code.alpha = alpha
        radiobutton_access_code.isEnabled = enable
    }
}
