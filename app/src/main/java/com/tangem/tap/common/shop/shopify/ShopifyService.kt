package com.tangem.tap.common.shop.shopify

import android.app.Application
import com.shopify.buy3.GraphCallResult
import com.shopify.buy3.GraphClient
import com.shopify.buy3.RetryHandler
import com.shopify.buy3.Storefront.*
import com.shopify.graphql.support.ID
import com.shopify.graphql.support.Input
import com.tangem.tap.common.shop.shopify.data.CheckoutItem
import com.tangem.tap.common.shop.shopify.data.checkoutFieldsFragment
import com.tangem.tap.common.shop.shopify.data.collectionFieldsFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@Suppress("LargeClass")
class ShopifyService(private val application: Application, val shop: ShopifyShop) {
    val client: GraphClient by lazy { initClient() }

    suspend fun getShopName(): Result<String> {
        val query = query { rootQuery: QueryRootQuery ->
            rootQuery
                .shop { shopQuery: ShopQuery ->
                    shopQuery
                        .name()
                }
        }
        return when (val result = queryAsync(query)) {
            is GraphCallResult.Success -> {
                val name = result.response.data!!.shop.name
                Result.success(name)
            }
            is GraphCallResult.Failure -> {
                Result.failure(result.error)
            }
        }
    }

    @Suppress("MagicNumber")
    suspend fun getProducts(collectionTitleFilter: String? = null): Result<List<Product>> {
        val filter = collectionTitleFilter?.let { "title:\"$it\"" }

        val query = query { rootQuery: QueryRootQuery ->
            rootQuery.collections(
                { arg -> arg.first(250).query(filter) },
                CollectionConnectionQuery::collectionFieldsFragment,
            )
        }
        return when (val result = queryAsync(query)) {
            is GraphCallResult.Success -> {
                val products = result.response.data!!.collections.edges
                    .map { it.node.products }
                    .flatMap { it.edges }
                    .map { it.node }
                Result.success(products)
            }
            is GraphCallResult.Failure -> {
                Result.failure(result.error)
            }
        }
    }

    suspend fun checkout(pollUntilOrder: Boolean, checkoutID: ID): Result<Checkout> {
        val query = query { rootQuery: QueryRootQuery ->
            rootQuery
                .node(checkoutID) { query ->
                    query.onCheckout { checkoutQuery ->
                        with(checkoutQuery) {
                            checkoutFieldsFragment()
                        }
                    }
                }
        }
        val retryHandler = RetryHandler.build<QueryRoot>(
            delay = 1,
            timeUnit = TimeUnit.SECONDS,
        ) {
            this.retryWhen { result ->
                when (result) {
                    is GraphCallResult.Success -> {
                        val checkout = result.response.data?.node as? Checkout
                        checkout?.order == null
                    }
                    is GraphCallResult.Failure -> false
                }
            }
        }

        val result = if (pollUntilOrder) queryAsync(query, retryHandler) else queryAsync(query)
        return when (result) {
            is GraphCallResult.Success -> {
                val checkout = result.response.data!!.node as? Checkout
                if (checkout != null) {
                    Result.success(checkout)
                } else {
                    Result.failure(ShopifyError.Unknown)
                }
            }
            is GraphCallResult.Failure -> {
                Result.failure(result.error)
            }
        }
    }

    suspend fun createCheckout(
        checkoutItems: List<CheckoutItem>,
        checkoutID: ID? = null,
    ): Result<Checkout> {
        val storefrontLineItems: MutableList<CheckoutLineItemInput> = checkoutItems
            .map { CheckoutLineItemInput(it.quantity, it.id) }.toMutableList()

        val query = if (checkoutID != null) {
            mutation { mutationQuery: MutationQuery ->
                mutationQuery
                    .checkoutLineItemsReplace(
                        storefrontLineItems,
                        checkoutID,
                    ) { payloadQuery: CheckoutLineItemsReplacePayloadQuery ->
                        payloadQuery
                            .checkout { checkoutQuery: CheckoutQuery ->
                                checkoutQuery.checkoutFieldsFragment()
                            }
                            .userErrors { userErrorQuery: CheckoutUserErrorQuery ->
                                userErrorQuery
                                    .field()
                                    .message()
                            }
                    }
            }
        } else {
            val input = CheckoutCreateInput()
                .setLineItemsInput(
                    Input.value(storefrontLineItems),
                )
            mutation { mutationQuery: MutationQuery ->
                mutationQuery
                    .checkoutCreate(
                        input,
                    ) { payloadQuery: CheckoutCreatePayloadQuery ->
                        payloadQuery
                            .checkout { checkoutQuery: CheckoutQuery ->
                                checkoutQuery.checkoutFieldsFragment()
                            }
                            .checkoutUserErrors { userErrorQuery: CheckoutUserErrorQuery ->
                                userErrorQuery
                                    .field()
                                    .message()
                            }
                    }
            }
        }
        return runCheckoutMutation(query)
    }

    suspend fun applyDiscount(discountCode: String, checkoutID: ID): Result<Checkout> {
        val query = mutation { mutationQuery: MutationQuery ->
            mutationQuery
                .checkoutDiscountCodeApplyV2(
                    discountCode,
                    checkoutID,
                ) { payloadQuery: CheckoutDiscountCodeApplyV2PayloadQuery ->
                    payloadQuery
                        .checkout { checkoutQuery: CheckoutQuery ->
                            checkoutQuery.checkoutFieldsFragment()
                        }
                        .checkoutUserErrors { userErrorQuery: CheckoutUserErrorQuery ->
                            userErrorQuery
                                .field()
                                .message()
                        }
                }
        }
        return runCheckoutMutation(query)
    }

    suspend fun removeDiscount(checkoutID: ID): Result<Checkout> {
        val query = mutation { mutationQuery: MutationQuery ->
            mutationQuery
                .checkoutDiscountCodeRemove(
                    checkoutID,
                ) { payloadQuery: CheckoutDiscountCodeRemovePayloadQuery ->
                    payloadQuery
                        .checkout { checkoutQuery: CheckoutQuery ->
                            checkoutQuery.checkoutFieldsFragment()
                        }
                        .checkoutUserErrors { userErrorQuery: CheckoutUserErrorQuery ->
                            userErrorQuery
                                .field()
                                .message()
                        }
                }
        }
        return runCheckoutMutation(query)
    }

    suspend fun completeWithTokenizedPayment(
        payment: TokenizedPaymentInputV3,
        checkoutID: ID,
    ): Result<Checkout> {
        val query = mutation { mutationQuery: MutationQuery ->
            mutationQuery
                .checkoutCompleteWithTokenizedPaymentV3(
                    checkoutID,
                    payment,
                ) { payloadQuery: CheckoutCompleteWithTokenizedPaymentV3PayloadQuery ->
                    payloadQuery
                        .payment { paymentQuery: PaymentQuery ->
                            paymentQuery
                                .ready()
                                .errorMessage()
                        }
                        .checkout { checkoutQuery: CheckoutQuery ->
                            checkoutQuery
                                .ready()
                        }
                        .checkoutUserErrors { userErrorQuery: CheckoutUserErrorQuery ->
                            userErrorQuery
                                .field()
                                .message()
                        }
                }
        }
        return runCheckoutMutation(query)
    }

    private suspend fun runCheckoutMutation(mutation: MutationQuery): Result<Checkout> {
        return when (val result = mutationQueryAsync(mutation)) {
            is GraphCallResult.Success -> {
                val checkout = result.response.data!!.checkoutCreate?.checkout
                    ?: result.response.data!!.checkoutDiscountCodeApplyV2?.checkout
                    ?: result.response.data!!.checkoutDiscountCodeRemove?.checkout
                    ?: result.response.data!!.checkoutShippingAddressUpdateV2?.checkout
                    ?: result.response.data!!.checkoutEmailUpdateV2?.checkout
                    ?: result.response.data!!.checkoutShippingLineUpdate?.checkout
                    ?: result.response.data!!.checkoutCompleteWithTokenizedPaymentV3.checkout

                Result.success(checkout)
            }
            is GraphCallResult.Failure -> Result.failure(result.error)
        }
    }

    private suspend fun queryAsync(
        query: QueryRootQuery,
        retryHandler: RetryHandler<QueryRoot>,
    ): GraphCallResult<QueryRoot> =
        withContext(Dispatchers.IO) {
            suspendCoroutine { continuation ->
                client.queryGraph(query).enqueue(retryHandler = retryHandler) { result ->
                    continuation.resume(result)
                }
            }
        }

    private suspend fun queryAsync(
        query: QueryRootQuery,
    ): GraphCallResult<QueryRoot> =
        withContext(Dispatchers.IO) {
            suspendCoroutine { continuation ->
                client.queryGraph(query).enqueue { result ->
                    continuation.resume(result)
                }
            }
        }

    private suspend fun mutationQueryAsync(query: MutationQuery): GraphCallResult<Mutation> =
        withContext(Dispatchers.IO) {
            suspendCoroutine { continuation ->
                client.mutateGraph(query).enqueue { result ->
                    continuation.resume(result)
                }
            }
        }

    private fun initClient(): GraphClient {
        return GraphClient.build(
            application,
            shop.domain,
            shop.storefrontApiKey,
        ) {
//            httpCache(application.filesDir) {
//                cacheMaxSizeBytes = (1024 * 1024 * 10)
//                defaultCachePolicy =
//                    HttpCachePolicy.Default.CACHE_FIRST.expireAfter(20, TimeUnit.MINUTES)
//            }
        }
    }
}

sealed class ShopifyError : Throwable() {
    object Unknown : ShopifyError()
}
