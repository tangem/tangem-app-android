package com.tangem.tap.features.shop.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.View.OnFocusChangeListener
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.activity.OnBackPressedCallback
import com.tangem.tap.common.KeyboardObserver
import com.tangem.tap.common.extensions.show
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.common.shop.data.ProductType
import com.tangem.tap.features.BaseStoreFragment
import com.tangem.tap.features.shop.redux.ShopAction
import com.tangem.tap.features.shop.redux.ShopState
import com.tangem.tap.store
import com.tangem.wallet.R
import kotlinx.android.synthetic.main.fragment_shop.*
import org.rekotlin.StoreSubscriber


class ShopFragment : BaseStoreFragment(R.layout.fragment_shop), StoreSubscriber<ShopState> {


    override fun subscribeToStore() {
        store.subscribe(this) { state ->
            state.skipRepeats { oldState, newState ->
                oldState.shopState == newState.shopState
            }.select { it.shopState }
        }
        storeSubscribersList.add(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity?.onBackPressedDispatcher?.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                store.dispatch(NavigationAction.PopBackTo())
                store.dispatch(ShopAction.ResetState)
            }
        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupCardsImages()
        setupProductSelection()
        setupPromoCodeEditText()

        toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressed()
        }


        val keyboardObserver = KeyboardObserver(requireActivity())
        keyboardObserver.registerListener { isVisible ->
            fl_cards.show(!isVisible)
        }
    }

    private fun setupCardsImages() {
        imv_second.animate()
            .translationY(70f)
            .scaleX(0.9f)
            .scaleY(0.9f)
            .start()
        imv_third.animate()
            .translationY(140f)
            .scaleX(0.8f)
            .scaleY(0.8f)
            .start()
    }

    private fun setupProductSelection() {
        chip_product_1.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) store.dispatch(ShopAction.SelectProduct(ProductType.WALLET_3_CARDS))
        }
        chip_product_2.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) store.dispatch(ShopAction.SelectProduct(ProductType.WALLET_2_CARDS))
        }
    }

    private fun setupPromoCodeEditText() {
        et_promo_code.setOnEditorActionListener { view, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val imm: InputMethodManager =
                    requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(view.windowToken, 0)
                view.clearFocus()
                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }

        et_promo_code.onFocusChangeListener = OnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                store.dispatch(ShopAction.ApplyPromoCode(et_promo_code.text.toString()))
            }
        }
    }

    override fun newState(state: ShopState) {
        if (activity == null) return

        animateProductSelection(state.selectedProduct)
        handlePriceState(state)
        handlePromoCodeState(state)
        handleButtonsState(state)
    }

    private fun animateProductSelection(selectedProduct: ProductType) {
        val show = when (selectedProduct) {
            ProductType.WALLET_2_CARDS -> false
            ProductType.WALLET_3_CARDS -> true
        }
        showOrHideThirdCardWithAnimation(show)
    }

    private fun showOrHideThirdCardWithAnimation(show: Boolean) {
        val translationY = if (show) 140f else 0f
        if (show) imv_third.show()
        imv_third.animate()
            .translationY(translationY)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    imv_third?.show(show)
                }
            })
    }

    private fun handlePriceState(state: ShopState) {
        tv_total.text = state.total
        tv_total_before_discount.text = state.priceBeforeDiscount

        pb_price.show(state.total == null)

    }

    private fun handlePromoCodeState(state: ShopState) {
        if (state.promoCode == null && !et_promo_code.hasFocus()) {
            et_promo_code.setText("")
        }
        pb_promo_code.show(state.promoCodeLoading)
    }

    private fun handleButtonsState(state: ShopState) {
        btn_pay_google_pay.show(state.isGooglePayAvailable)
        btn_alternative_payment.show(state.isGooglePayAvailable)
        btn_main_action.show(!state.isGooglePayAvailable)

        if (state.total != null) {
            btn_alternative_payment.setOnClickListener { store.dispatch(ShopAction.StartWebCheckout) }
            btn_main_action.setOnClickListener { store.dispatch(ShopAction.StartWebCheckout) }
            btn_pay_google_pay.setOnClickListener { store.dispatch(ShopAction.BuyWithGooglePay) }
        }
    }

    override fun handleOnBackPressed() {
        store.dispatch(ShopAction.ResetState)
        super.handleOnBackPressed()
    }
}