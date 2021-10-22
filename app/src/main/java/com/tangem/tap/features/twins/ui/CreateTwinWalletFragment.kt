package com.tangem.tap.features.twins.ui

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat.getColor
import androidx.fragment.app.Fragment
import androidx.transition.TransitionInflater
import com.squareup.picasso.Picasso
import com.tangem.Message
import com.tangem.tap.common.extensions.readAssetAsString
import com.tangem.tap.domain.twins.AssetReader
import com.tangem.tap.domain.twins.TwinCardNumber
import com.tangem.tap.features.twins.redux.*
import com.tangem.tap.features.wallet.redux.Artwork
import com.tangem.tap.store
import com.tangem.wallet.R
import kotlinx.android.synthetic.main.fragment_twin_cards_create.*
import org.rekotlin.StoreSubscriber

class CreateTwinWalletFragment : Fragment(R.layout.fragment_twin_cards_create),
        StoreSubscriber<TwinCardsState> {

    private val assetReader: AssetReader by lazy {
        object : AssetReader {
            override fun readAssetAsString(name: String): String = requireContext().readAssetAsString(name)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity?.onBackPressedDispatcher?.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                store.dispatch(TwinCardsAction.Wallet.HandleOnBackPressed)
            }
        })

        initResources()
        val inflater = TransitionInflater.from(requireContext())
        enterTransition = inflater.inflateTransition(R.transition.slide_right)
        exitTransition = inflater.inflateTransition(R.transition.fade)
    }

    private fun initResources() {
        val resources = TwinCardsResources(
            TwinCardsResources.Strings(
                R.string.details_notification_erase_wallet_not_possible
            )
        )
        store.dispatch(TwinCardsAction.SetResources(resources))
    }

    override fun onStart() {
        super.onStart()
        store.subscribe(this) { state ->
            state.skipRepeats { oldState, newState ->
                oldState.twinCardsState == newState.twinCardsState
            }.select { it.twinCardsState }
        }
    }

    override fun onStop() {
        super.onStop()
        store.unsubscribe(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbar.setNavigationOnClickListener { store.dispatch(TwinCardsAction.Wallet.HandleOnBackPressed) }

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


    override fun newState(state: TwinCardsState) {
        if (activity == null) return

        toolbar.title = when (state.createWalletState?.mode) {
            CreateTwinWalletMode.CreateWallet -> getText(R.string.wallet_button_create_wallet)
            CreateTwinWalletMode.RecreateWallet, null -> getText(R.string.details_twins_recreate_toolbar)

        }

        val selectedColor = getColor(requireContext(), R.color.colorSecondary)
        val defaultColor = getColor(requireContext(), R.color.blue_pale)

        val twinCardNumber = state.createWalletState?.number ?: TwinCardNumber.First

        val cardNumber = when (state.createWalletState?.step) {
            CreateTwinWalletStep.FirstStep -> {
                val twinCardNumberString = twinCardNumber.number.toString()
                tv_step_number.text =
                        getString(R.string.details_twins_recreate_step_format, "1")
                v_step_1.setBackgroundColor(selectedColor)
                v_step_2.setBackgroundColor(defaultColor)
                v_step_3.setBackgroundColor(defaultColor)
                btn_tap.setOnClickListener {
                    store.dispatch(TwinCardsAction.Wallet.LaunchFirstStep(
                        Message(getString(
                            R.string.details_twins_recreate_title_format,
                            twinCardNumberString
                        )),
                        assetReader
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
                    store.dispatch(TwinCardsAction.Wallet.LaunchSecondStep(
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
                    store.dispatch(TwinCardsAction.Wallet.LaunchThirdStep(
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
    }
}