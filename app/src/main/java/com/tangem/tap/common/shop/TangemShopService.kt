package com.tangem.tap.common.shop

import android.app.Application
import android.content.Intent
import com.google.android.gms.wallet.PaymentData
import com.shopify.buy3.Storefront
import com.tangem.tap.common.analytics.Analytics
import com.tangem.tap.common.analytics.converters.ShopOrderToEventConverter
import com.tangem.tap.common.extensions.filterNotNull
import com.tangem.tap.common.shop.data.ProductType
import com.tangem.tap.common.shop.data.TangemProduct
import com.tangem.tap.common.shop.data.TotalSum
import com.tangem.tap.common.shop.shopify.ShopifyService
import com.tangem.tap.common.shop.shopify.ShopifyShop
import com.tangem.tap.common.shop.shopify.data.CheckoutItem
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.math.BigDecimal
import java.util.*

class TangemShopService(application: Application, shopifyShop: ShopifyShop) {

    private val shopifyService = ShopifyService(application, shopifyShop)

    private val checkouts = mutableMapOf<ProductType, Storefront.Checkout>()
    private val variants = mutableMapOf<ProductType, Storefront.ProductVariant>()

    private lateinit var googlePayService: GooglePayService

    suspend fun getProducts(): Result<List<TangemProduct>> {
        val result = shopifyService.getProducts()

        return result.mapCatching { product ->
            val availableVariants = product
                .flatMap { it.variants.edges.map { it.node } }
                .filter { SKUS_TO_DISPLAY.contains(it.sku) }
                .associateBy { ProductType.fromSku(it.sku) }
                .filterNotNull()
            variants.putAll(availableVariants)

            if (variants.size < SKUS_TO_DISPLAY.size) {
                return Result.failure(
                    Exception(
                        "Shopify: products are missing, " +
                            "\nproducts available: ${variants.keys.map { it.sku }}",
                    ),
                )
            }

            val twoCardsProduct = TangemProduct(
                type = ProductType.WALLET_2_CARDS,
                totalSum = TotalSum(
                    finalValue = variants[ProductType.WALLET_2_CARDS]?.priceV2?.format(),
                    beforeDiscount = variants[ProductType.WALLET_2_CARDS]?.compareAtPriceV2?.format(),
                ),
            )
            val threeCardsProduct = TangemProduct(
                type = ProductType.WALLET_3_CARDS,
                totalSum = TotalSum(
                    finalValue = variants[ProductType.WALLET_3_CARDS]?.priceV2?.format(),
                    beforeDiscount = variants[ProductType.WALLET_3_CARDS]?.compareAtPriceV2?.format(),
                ),
            )
            createCheckouts()
            return Result.success(listOf(twoCardsProduct, threeCardsProduct))
        }
    }

    private suspend fun createCheckouts() {
        variants.keys.map { coroutineScope { async { createCheckout(it) } } }.awaitAll()
    }

    private suspend fun createCheckout(productType: ProductType) {
        val checkoutItem = CheckoutItem(variants[productType]!!.id, 1)
        val result = shopifyService.createCheckout(listOf(checkoutItem))
        result.onSuccess { checkout ->
            checkouts[productType] = checkout
        }
    }

    suspend fun checkIfGooglePayAvailable(googlePayService: GooglePayService): Result<Boolean> {
        this.googlePayService = googlePayService
        return googlePayService.checkIfGooglePayAvailable()
    }

    fun buyWithGooglePay(productType: ProductType) {
        val totalPrice = checkouts[productType]!!.totalPriceV2.amount
        googlePayService.payWithGooglePay(
            totalPriceCents = totalPrice, currencyCode = checkouts[productType]!!.currencyCode.name,
            merchantID = shopifyService.shop.merchantID,
        )
    }

//    fun subscribeToGooglePayResult(
//        productType: ProductType,
//        resultCallback: (Result<PaymentData>) -> Unit
//    ) {
//        googlePayService.responseCallback = { result ->
//            result.onFailure { }
//            result.onSuccess {
//                completeTokenizedPayment(it, productType)
//            }
//        }
//    }

    suspend fun handleGooglePayResult(
        resultCode: Int,
        data: Intent?,
        productType: ProductType,
    ): Result<Unit> {
        val result = googlePayService.handleResponseFromGooglePay(resultCode, data)
        result.onSuccess {
            val finalizePaymentResult = completeTokenizedPayment(it, productType)
            finalizePaymentResult.onSuccess {
                return Result.success(Unit)
            }
            return Result.failure(finalizePaymentResult.exceptionOrNull()!!)
        }
        return Result.failure(result.exceptionOrNull()!!)
    }

    private suspend fun completeTokenizedPayment(
        paymentData: PaymentData,
        productType: ProductType,
    ): Result<Storefront.Checkout> {
        val checkout = checkouts[productType]!!
        val googlePayResponse =
            googlePayService.parsePaymentData(paymentData)
                ?: return Result.failure(Exception("cannot parse GPay result"))

        val amount =
            Storefront.MoneyInput(checkout.totalPriceV2.amount, checkout.totalPriceV2.currencyCode)
        val idempotencyKey = UUID.randomUUID().toString()
        val addressGPay = googlePayResponse.billingAddress

        val address = Storefront.MailingAddressInput().apply {
            lastName = addressGPay.name
            address1 = addressGPay.address1
            address2 = addressGPay.address2 + addressGPay.address3
            province = addressGPay.administrativeArea
            zip = addressGPay.postalCode
            phone = addressGPay.phoneNumber
        }

        val payment = Storefront.TokenizedPaymentInputV3(
            amount,
            idempotencyKey,
            address,
            paymentData.toJson(),
            Storefront.PaymentTokenType.GOOGLE_PAY,
        )
            .setTest(true)

        return shopifyService.completeWithTokenizedPayment(
            payment = payment,
            checkoutID = checkout.id,
        )
    }

    suspend fun applyPromoCode(promoCode: String): Result<List<TangemProduct>> {
        val products = variants.keys
            .map { coroutineScope { async { applyPromoCode(promoCode, it) } } }
            .awaitAll()
            .map { result -> result.getOrElse { return Result.failure(it) } }

        return Result.success(products)
    }

    suspend fun applyPromoCode(promoCode: String, productType: ProductType): Result<TangemProduct> {
        val checkout = checkouts[productType] ?: return Result.failure(Exception("No checkout"))

        val result = if (promoCode.isBlank()) {
            shopifyService.removeDiscount(checkout.id)
        } else {
            shopifyService.applyDiscount(promoCode, checkout.id)
        }

        result.onSuccess {
            checkouts[productType] = it
            return Result.success(
                TangemProduct(
                    productType,
                    TotalSum(
                        finalValue = it.totalPriceV2.format(),
                        beforeDiscount = variants[productType]!!.compareAtPriceV2.format(),
                    ),
                    appliedDiscount = it.getAppliedDiscount(),
                ),

                )
        }
        return Result.failure(result.exceptionOrNull()!!)
    }

    fun getCheckoutUrl(productType: ProductType): String {
        return checkouts[productType]!!.webUrl
    }

    suspend fun waitForCheckout(productType: ProductType) {
        val result = shopifyService.checkout(true, checkouts[productType]!!.id)
        result.onSuccess { checkout ->
            checkout.order?.let {
                val event = ShopOrderToEventConverter().convert(it to productType)
                Analytics.send(event)
            }
        }
    }

    companion object {
        const val TANGEM_WALLET_2_CARDS_SKU = "TG115x2"
        const val TANGEM_WALLET_3_CARDS_SKU = "TG115x3"
        val SKUS_TO_DISPLAY = listOf(TANGEM_WALLET_2_CARDS_SKU, TANGEM_WALLET_3_CARDS_SKU)
    }
}

private fun Storefront.MoneyV2.format(): String {
    val currencySymbol = Currency.getInstance(currencyCode.name).symbol
    val amountFormatted = BigDecimal(amount).setScale(2)
    return currencySymbol + amountFormatted
}

private fun Storefront.Checkout.getAppliedDiscount(): String? {
    val discountApplication =
        discountApplications.edges.firstOrNull()?.node as? Storefront.DiscountCodeApplication
    return discountApplication?.code
}
