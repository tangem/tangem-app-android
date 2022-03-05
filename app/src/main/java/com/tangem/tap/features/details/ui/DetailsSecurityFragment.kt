package com.tangem.tap.features.details.ui

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.transition.TransitionInflater
import by.kirich1409.viewbindingdelegate.viewBinding
import com.tangem.common.card.Card
import com.tangem.tap.common.extensions.show
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.features.details.redux.DetailsAction
import com.tangem.tap.features.details.redux.DetailsState
import com.tangem.tap.features.details.redux.SecurityOption
import com.tangem.tap.store
import com.tangem.wallet.R
import com.tangem.wallet.databinding.FragmentDetailsSecurityBinding
import org.rekotlin.StoreSubscriber
import java.util.*

class DetailsSecurityFragment : Fragment(R.layout.fragment_details_security),
        StoreSubscriber<DetailsState> {

    private val binding: FragmentDetailsSecurityBinding by viewBinding(
        FragmentDetailsSecurityBinding::bind
    )

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
        binding.toolbar.setNavigationOnClickListener {
            store.dispatch(NavigationAction.PopBackTo())
        }
        setOnClickListeners()
    }

    private fun setOnClickListeners() {
        binding.btnSaveChanges.setOnClickListener {
            store.state.detailsState.securityScreenState?.selectedOption?.let {
                store.dispatch(DetailsAction.ManageSecurity.ConfirmSelection(it))
            }
        }
    }

    override fun newState(state: DetailsState) {
        if (activity == null || view == null) return

        selectSecurityOption(state.securityScreenState?.selectedOption)
        for (option in SecurityOption.values()) {
            setupOption(
                    option,
                    state.securityScreenState?.allowedOptions
                            ?: EnumSet.noneOf(SecurityOption::class.java)
            )
        }
        binding.tvAccessCodeUnavailableDisclaimer.show(
            state.scanResponse?.card?.backupStatus == Card.BackupStatus.NoBackup
        )
    }

    private fun selectSecurityOption(securityOption: SecurityOption?) = with(binding) {
        radiobuttonLongTap.isChecked =
                securityOption == SecurityOption.LongTap && radiobuttonLongTap.isEnabled
        radiobuttonPasscode.isChecked =
                securityOption == SecurityOption.PassCode && radiobuttonPasscode.isEnabled
        radiobuttonAccessCode.isChecked =
                securityOption == SecurityOption.AccessCode && radiobuttonAccessCode.isEnabled
    }

    private fun setupOption(option: SecurityOption, allowedOptions: EnumSet<SecurityOption>) {
        when (option) {
            SecurityOption.LongTap -> { enableLongTap(allowedOptions.contains(option)) }
            SecurityOption.PassCode -> { enablePasscode(allowedOptions.contains(option)) }
            SecurityOption.AccessCode -> { enableAccessCode(allowedOptions.contains(option)) }
        }
    }

    private fun enableLongTap(enable: Boolean) = with(binding) {
        groupLongTap.show(enable)
        val alpha = if (enable) 1f else 0.5f
        tvLongTapDescription.alpha = alpha
        tvLongTapTitle.alpha = alpha
        radiobuttonLongTap.alpha = alpha
        radiobuttonLongTap.isEnabled = enable
        vLongTap.isClickable = enable
        if (enable) {
            vLongTap.setOnClickListener {
                store.dispatch(DetailsAction.ManageSecurity.SelectOption(SecurityOption.LongTap))
            }
        }
    }

    private fun enablePasscode(enable: Boolean) = with(binding) {
        groupPasscode.show(enable)
        val alpha = if (enable) 1f else 0.5f
        tvPasscodeDescription.alpha = alpha
        tvPasscodeTitle.alpha = alpha
        radiobuttonPasscode.alpha = alpha
        radiobuttonPasscode.isEnabled = enable
        vPasscode.isClickable = enable
        if (enable) {
            vPasscode.setOnClickListener {
                store.dispatch(DetailsAction.ManageSecurity.SelectOption(SecurityOption.PassCode))
            }
        }
    }

    private fun enableAccessCode(enable: Boolean) = with(binding) {
        groupAccessCode.show(enable)
        val alpha = if (enable) 1f else 0.5f
        tvAccessCodeDescription.alpha = alpha
        tvAccessCodeTitle.alpha = alpha
        radiobuttonAccessCode.alpha = alpha
        radiobuttonAccessCode.isEnabled = enable
        vAccessCode.isClickable = enable
        if (enable) {
            vAccessCode.setOnClickListener {
                store.dispatch(DetailsAction.ManageSecurity.SelectOption(SecurityOption.AccessCode))
            }
        }
    }
}