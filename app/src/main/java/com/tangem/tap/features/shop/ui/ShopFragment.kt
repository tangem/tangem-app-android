package com.tangem.tap.features.shop.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.View
import android.view.View.OnFocusChangeListener
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.activity.OnBackPressedCallback
import by.kirich1409.viewbindingdelegate.viewBinding
import com.tangem.tap.common.GlobalLayoutStateHandler
import com.tangem.tap.common.KeyboardObserver
import com.tangem.tap.common.extensions.getColorCompat
import com.tangem.tap.common.extensions.getQuantityString
import com.tangem.tap.common.extensions.hide
import com.tangem.tap.common.extensions.show
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.common.shop.data.ProductType
import com.tangem.tap.features.BaseStoreFragment
import com.tangem.tap.features.shop.redux.ShopAction
import com.tangem.tap.features.shop.redux.ShopState
import com.tangem.tap.store
import com.tangem.wallet.R
import com.tangem.wallet.databinding.FragmentShopBinding
import org.rekotlin.StoreSubscriber
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ShopFragment : BaseStoreFragment(R.layout.fragment_shop), StoreSubscriber<ShopState> {

    private val dateFormat = SimpleDateFormat("dd MMMM", Locale.getDefault())
    private val binding: FragmentShopBinding by viewBinding(FragmentShopBinding::bind)
    private val endPreorderTime = Calendar.getInstance()
    private val isBuyAvailable: Boolean

    private var cardTranslationY = 70f

    private lateinit var keyboardObserver: KeyboardObserver

    init {
        endPreorderTime.clear()
        endPreorderTime.set(END_DATE_SOLD_OUT_YEAR, END_DATE_SOLD_OUT_MONTH, END_DATE_SOLD_OUT_DAY)
        isBuyAvailable = System.currentTimeMillis() > endPreorderTime.timeInMillis
    }

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

        activity?.onBackPressedDispatcher?.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    store.dispatch(NavigationAction.PopBackTo())
                    store.dispatch(ShopAction.ResetState)
                }
            },
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        keyboardObserver.unregisterListener()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupCardsImages()
        setupProductSelection()
        setupPromoCodeEditText()
        setupSoldOutDescription()

        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressed()
        }

        keyboardObserver = KeyboardObserver(requireActivity()).apply {
            registerListener { isVisible ->
                binding.flCards.show(!isVisible)
            }
        }
    }

    @Suppress("MagicNumber")
    private fun setupCardsImages() {
        GlobalLayoutStateHandler(binding.imvSecond).apply {
            onStateChanged = {
                cardTranslationY = it.height * 0.15f
                binding.imvSecond.animate()
                    .translationY(cardTranslationY)
                    .scaleX(0.9f)
                    .scaleY(0.9f)
                    .start()
                binding.imvThird.animate()
                    .translationY(cardTranslationY * 2)
                    .scaleX(0.8f)
                    .scaleY(0.8f)
                    .start()
                detach()
            }
        }
    }

    private fun setupProductSelection() = with(binding) {
        chipProduct1.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) store.dispatch(ShopAction.SelectProduct(ProductType.WALLET_3_CARDS))
        }
        chipProduct2.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) store.dispatch(ShopAction.SelectProduct(ProductType.WALLET_2_CARDS))
        }
        chipProduct1.text = chipProduct1.getQuantityString(R.plurals.card_label_card_count, quantity = 3)
        chipProduct2.text = chipProduct2.getQuantityString(R.plurals.card_label_card_count, quantity = 2)
    }

    private fun setupPromoCodeEditText() = with(binding) {
        etPromoCode.setOnEditorActionListener { view, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val imm: InputMethodManager =
                    requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(view.windowToken, 0)
                view.clearFocus()
                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }

        etPromoCode.onFocusChangeListener = OnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                store.dispatch(ShopAction.ApplyPromoCode(etPromoCode.text.toString()))
            }
        }
    }

    private fun setupSoldOutDescription() = with(binding) {
        context?.let { cntx ->
            if (!isBuyAvailable) {
                val date = "${dateFormat.format(endPreorderTime.time)}!"
                val soldOutTitle = cntx.getString(R.string.shop_sold_out_description_prefix)
                val generalString = "$soldOutTitle $date"
                val wordToSpan: Spannable = SpannableString(generalString)
                wordToSpan.setSpan(
                    ForegroundColorSpan(cntx.getColorCompat(R.color.textBlack)),
                    generalString.length - date.length,
                    generalString.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
                )
                tvSoldOutDesc.text = wordToSpan
            } else {
                tvSoldOutDesc.hide()
            }
        }
    }

    override fun newState(state: ShopState) {
        if (activity == null || view == null) return

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

    private fun showOrHideThirdCardWithAnimation(show: Boolean) = with(binding) {
        val translationY = if (show) cardTranslationY * 2 else cardTranslationY
        if (show) imvThird.show()
        imvThird.animate()
            .translationY(translationY)
            .setListener(
                object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        super.onAnimationEnd(animation)
                        imvThird.show(show)
                    }
                },
            )
    }

    private fun handlePriceState(state: ShopState) = with(binding) {
        tvTotal.text = state.total
        tvTotalBeforeDiscount.text = state.priceBeforeDiscount

        pbPrice.show(state.total == null)
    }

    private fun handlePromoCodeState(state: ShopState) = with(binding) {
        if (state.promoCode == null && !etPromoCode.hasFocus()) {
            etPromoCode.setText("")
        }
        pbPromoCode.show(state.promoCodeLoading)
    }

    private fun handleButtonsState(state: ShopState) = with(binding) {
        if (isBuyAvailable) {
            btnPayGooglePay.root.show(state.isGooglePayAvailable)
            btnAlternativePayment.show(state.isGooglePayAvailable)
            btnMainAction.show(!state.isGooglePayAvailable)
        } else {
            btnPayGooglePay.root.hide()
            btnAlternativePayment.hide()
            btnMainAction.show()
            btnMainAction.text = context?.getString(R.string.shop_pre_order_now)
        }

        if (state.total != null) {
            btnAlternativePayment.setOnClickListener { store.dispatch(ShopAction.StartWebCheckout) }
            btnMainAction.setOnClickListener { store.dispatch(ShopAction.StartWebCheckout) }
            btnPayGooglePay.root.setOnClickListener { store.dispatch(ShopAction.BuyWithGooglePay) }
        }
    }

    override fun handleOnBackPressed() {
        store.dispatch(ShopAction.ResetState)
        super.handleOnBackPressed()
    }

    companion object {
        private const val END_DATE_SOLD_OUT_YEAR = 2023
        private const val END_DATE_SOLD_OUT_MONTH = 5 // June, month start counting from zero!
        private const val END_DATE_SOLD_OUT_DAY = 10
    }
}
