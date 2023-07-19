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
import androidx.fragment.app.viewModels
import by.kirich1409.viewbindingdelegate.viewBinding
import com.tangem.core.navigation.NavigationAction
import com.tangem.tap.common.GlobalLayoutStateHandler
import com.tangem.tap.common.KeyboardObserver
import com.tangem.tap.common.extensions.getQuantityString
import com.tangem.tap.common.extensions.hide
import com.tangem.tap.common.extensions.show
import com.tangem.tap.features.BaseStoreFragment
import com.tangem.tap.features.shop.domain.models.ProductState
import com.tangem.tap.features.shop.domain.models.ProductType
import com.tangem.tap.features.shop.domain.models.SalesProduct
import com.tangem.tap.features.shop.presentation.ShopViewModel
import com.tangem.tap.features.shop.redux.ShopAction
import com.tangem.tap.features.shop.redux.ShopState
import com.tangem.tap.features.shop.toggles.ShopifyFeatureToggleManager
import com.tangem.tap.store
import com.tangem.wallet.R
import com.tangem.wallet.databinding.FragmentShopBinding
import dagger.hilt.android.AndroidEntryPoint
import org.rekotlin.StoreSubscriber
import javax.inject.Inject

@AndroidEntryPoint
internal class ShopFragment : BaseStoreFragment(R.layout.fragment_shop), StoreSubscriber<ShopState> {

    @Inject
    lateinit var shopifyFeatureToggleManager: ShopifyFeatureToggleManager

    private val binding: FragmentShopBinding by viewBinding(FragmentShopBinding::bind)
    private var cardTranslationY = 70f

    private lateinit var keyboardObserver: KeyboardObserver

    private val viewModel by viewModels<ShopViewModel>()

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

        if (shopifyFeatureToggleManager.isDynamicSalesProductsEnabled) {
            viewModel.getActualSalesInfo()
        } else {
            viewModel.checkOrderingDelayBlockVisibility()
        }

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

    override fun newState(state: ShopState) {
        if (activity == null || view == null) return

        animateProductSelection(state.selectedProduct)
        handlePriceState(state)
        handlePromoCodeState(state)
        if (shopifyFeatureToggleManager.isDynamicSalesProductsEnabled) {
            handleNotificationBlock(state)
        } else {
            handleOrderingDelayBlock(isVisible = state.isOrderingDelayBlockVisible)
        }
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

    private fun handleOrderingDelayBlock(isVisible: Boolean) {
        if (isVisible) binding.tvSoldOutDesc.show() else binding.tvSoldOutDesc.hide()
    }

    private fun handleNotificationBlock(state: ShopState) {
        if (isVisible) {
            binding.tvSoldOutDesc.show()
            getSelectedSalesProduct(state)?.notification?.let { notification ->
                binding.tvSoldOutDesc.text = notification.description
            }
        } else {
            binding.tvSoldOutDesc.hide()
        }
    }

    private fun handleButtonsState(state: ShopState) = with(binding) {
        btnPayGooglePay.root.show(state.isGooglePayAvailable)
        btnAlternativePayment.show(state.isGooglePayAvailable)
        btnMainAction.show(!state.isGooglePayAvailable)

        if (state.total != null) {
            btnAlternativePayment.setOnClickListener { store.dispatch(ShopAction.StartWebCheckout) }
            btnPayGooglePay.root.setOnClickListener { store.dispatch(ShopAction.BuyWithGooglePay) }
            btnMainAction.setOnClickListener { store.dispatch(ShopAction.StartWebCheckout) }
            if (state.salesProducts.isNotEmpty()) {
                getSelectedSalesProduct(state)?.let { selectedProduct ->
                    btnMainAction.text = getMainBtnTextByProductState(
                        productState = selectedProduct.state,
                    )
                }
            }
        }
    }

    override fun handleOnBackPressed() {
        store.dispatch(ShopAction.ResetState)
        super.handleOnBackPressed()
    }

    private fun getSelectedSalesProduct(state: ShopState): SalesProduct? {
        return state.salesProducts.find {
            it.productType == state.selectedProduct
        }
    }

    private fun getMainBtnTextByProductState(productState: ProductState): String = when (productState) {
        ProductState.ORDER -> getString(R.string.shop_buy_now)
        ProductState.SOLD_OUT -> "Sold out" // getString(R.string.sold_out) // todo finalize in next PR
        ProductState.PRE_ORDER -> "Pre order" // getString(R.string.pre_order) // todo finalize in next PR
    }
}
