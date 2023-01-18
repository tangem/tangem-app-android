package com.tangem.tap.common.shop.shopify.data

import com.shopify.buy3.Storefront

@Suppress("MagicNumber")
fun Storefront.CollectionConnectionQuery.collectionFieldsFragment() {
    edges { collectionEdgeQuery ->
        collectionEdgeQuery
            .node { collectionQuery ->
                collectionQuery
                    .title()
                    .products(
                        { arg -> arg.first(250) },
                    ) { productConnectionQuery ->
                        productConnectionQuery
                            .edges { productEdgeQuery ->
                                productEdgeQuery
                                    .node { productQuery ->
                                        productQuery.title()
                                            .productType()
                                            .description()
                                            .variants({ arg -> arg.first(10) }) { variantConnectionQuery ->
                                                variantConnectionQuery.edges { variantQuery ->
                                                    variantQuery.node {
                                                        it.title()
                                                        it.sku()
                                                        it.currentlyNotInStock()
                                                        it.priceV2 {
                                                            it.amount()
                                                            it.currencyCode()
                                                        }
                                                        it.compareAtPriceV2 {
                                                            it.amount()
                                                            it.currencyCode()
                                                        }
                                                        it.compareAtPriceV2 {
                                                            it.amount()
                                                        }
                                                        it.product {
                                                            it.title()
                                                                .productType()
                                                                .description()
                                                        }
                                                    }
                                                }
                                            }
                                    }
                            }
                    }
            }
    }
}
