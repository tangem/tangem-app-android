package com.tangem.tap.features.details.ui.twins

import android.app.Dialog
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat.getColor
import androidx.fragment.app.Fragment
import androidx.transition.TransitionInflater
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.squareup.picasso.Picasso
import com.tangem.Message
import com.tangem.tap.domain.twins.TwinCardNumber
import com.tangem.tap.features.details.redux.DetailsAction
import com.tangem.tap.features.details.redux.DetailsState
import com.tangem.tap.features.details.redux.twins.CreateTwinWallet
import com.tangem.tap.features.details.redux.twins.CreateTwinWalletStep
import com.tangem.tap.features.wallet.redux.Artwork
import com.tangem.tap.store
import com.tangem.wallet.R
import kotlinx.android.synthetic.main.fragment_details_twin_cards.*
import org.rekotlin.StoreSubscriber

class CreateTwinWalletFragment : Fragment(R.layout.fragment_details_twin_cards),
        StoreSubscriber<DetailsState> {

    private var dialog: Dialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity?.onBackPressedDispatcher?.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                store.dispatch(DetailsAction.CreateTwinWalletAction.Cancel)
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
            store.dispatch(DetailsAction.CreateTwinWalletAction.Cancel)
        }

        Picasso.get()
                .load(Artwork.TWIN_CARD_1)
                .placeholder(R.drawable.card_placeholder)
                ?.error(R.drawable.card_placeholder)
                ?.into(iv_card_1)

        Picasso.get()
                .load(Artwork.TWIN_CARD_2)
                .placeholder(R.drawable.card_placeholder)
                ?.error(R.drawable.card_placeholder)
                ?.into(iv_card_2)
    }


    override fun newState(state: DetailsState) {
        if (activity == null) return

        toolbar.title = when (state.createTwinWalletState?.createTwinWallet) {
            CreateTwinWallet.CreateWallet -> getText(R.string.wallet_button_create_wallet)
            CreateTwinWallet.RecreateWallet, null -> getText(R.string.details_twins_recreate_toolbar)

        }

        val selectedColor = getColor(requireContext(), R.color.colorSecondary)
        val defaultColor = getColor(requireContext(), R.color.blue_pale)

        val twinCardNumber = state.createTwinWalletState?.twinCardNumber ?: TwinCardNumber.First

        val cardNumber = when (state.createTwinWalletState?.step) {
            CreateTwinWalletStep.FirstStep -> {
                val twinCardNumberString = twinCardNumber.number.toString()
                tv_step_number.text =
                        getString(R.string.details_twins_recreate_step_format, "1")
                v_step_1.setBackgroundColor(selectedColor)
                v_step_2.setBackgroundColor(defaultColor)
                v_step_3.setBackgroundColor(defaultColor)
                btn_tap.setOnClickListener {
                    store.dispatch(DetailsAction.CreateTwinWalletAction.LaunchFirstStep(
                        Message(getString(
                            R.string.details_twins_recreate_title_format,
                            twinCardNumberString
                        )),
                        requireContext()
                    ))
                }
                btn_tap.text = getString(R.string.details_twins_recreate_button_format,
                        twinCardNumber.number.toString())
                twinCardNumberString
            }
            CreateTwinWalletStep.SecondStep -> {
                val twinCardNumberString = twinCardNumber.pairNumber().number.toString()

                tv_step_number.text =
                        getString(R.string.details_twins_recreate_step_format, "2")
                v_step_1.setBackgroundColor(selectedColor)
                v_step_2.setBackgroundColor(selectedColor)
                v_step_3.setBackgroundColor(defaultColor)
                btn_tap.setOnClickListener {
                    store.dispatch(DetailsAction.CreateTwinWalletAction.LaunchSecondStep(
                            Message(getString(R.string.details_twins_recreate_title_format, twinCardNumberString)),
                            Message(getString(R.string.details_twins_recreate_title_preparing)),
                            Message(getString(R.string.details_twins_recreate_title_creating_wallet)),
                    ))
                }
                btn_tap.text = getString(R.string.details_twins_recreate_button_format, "2")
                twinCardNumberString
            }
            CreateTwinWalletStep.ThirdStep -> {
                val twinCardNumberString = twinCardNumber.number.toString()

                tv_step_number.text =
                        getString(R.string.details_twins_recreate_step_format, "3")
                v_step_1.setBackgroundColor(selectedColor)
                v_step_2.setBackgroundColor(selectedColor)
                v_step_3.setBackgroundColor(selectedColor)
                btn_tap.setOnClickListener {
                    store.dispatch(DetailsAction.CreateTwinWalletAction.LaunchThirdStep(
                            Message(getString(
                                    R.string.details_twins_recreate_title_format, twinCardNumberString)
                            )
                    ))
                }
                btn_tap.text = getString(R.string.details_twins_recreate_button_format, twinCardNumberString)
                twinCardNumberString
            }
            else -> null
        }
        btn_tap.text = getString(R.string.details_twins_recreate_button_format, cardNumber)
        tv_twin_title.text = getString(R.string.details_twins_recreate_title_format, cardNumber)

        if (state.createTwinWalletState?.showAlert == true) {
            if (dialog == null) {
                dialog = MaterialAlertDialogBuilder(requireContext())
                        .setMessage(R.string.details_twins_recreate_alert)
                        .setPositiveButton(R.string.common_ok) { _, _ ->
                            store.dispatch(DetailsAction.CreateTwinWalletAction.Cancel.Confirm)
                        }
                        .setNegativeButton(R.string.common_cancel) { _, _ ->
                            dialog?.cancel()
                        }
                        .setOnCancelListener {
                            store.dispatch(DetailsAction.CreateTwinWalletAction.HideAlert)
                        }
                        .create()
                dialog?.show()
            }
        } else {
            dialog?.cancel()
            dialog = null
        }
    }

}