package com.tangem.tap.common.shop.shopify.data

import com.shopify.buy3.Storefront

fun Storefront.CheckoutQuery.checkoutFieldsFragment() {
    //                            id()
    ready()
    webUrl()
    currencyCode()
    lineItemsSubtotalPrice { it.amount() }
    totalPriceV2 {
        it.currencyCode()
        it.amount() }
    lineItems({ arg -> arg.first(250) }) {
        it.edges {
            it.node {
//                                        it.id()
                it.title()
                it.quantity()
                it.variant() {
                    it.priceV2() { it.amount() }
                }
            }
        }
    }
    shippingLine {
        it.handle()
        it.title()
        it.priceV2 { it.amount() }
    }
    availableShippingRates {
        it.ready()
        it.shippingRates {
            it.handle()
            it.title()
            it.priceV2 { it.amount() }
        }
    }
    shippingAddress {
        it.address1()
        it.address2()
        it.city()
//            .company()
        it.country()
//            .countryCodeV2()
        it.firstName()
//            .formatted()
//            .formattedArea()
//            .id()
        it.lastName()
//            .latitude()
//            .longitude()
//            .name()
        it.phone()
        it.province()
//            .provinceCode()
        it.zip()
    }
    discountApplications({ arg -> arg.first(250) }) {
        it.edges {
            it.node {
                it.onDiscountCodeApplication {
                    it.code()
//            .applicable()
//            .allocationMethod()
//            .targetSelection()
//            .targetType()
                    it.value {
                        it.onMoneyV2 {
                            it.amount()
                        }
                        it.onPricingPercentageValue {
                            it.percentage()
                        }
                    }
                }
            }
        }
    }
    order {
        it.cancelReason()
        it.canceledAt()
        it.currencyCode()
//            .currentSubtotalPrice()
//            .currentTotalDuties()
//            .currentTotalPrice()
//            .currentTotalTax()
        it.customerLocale()
        it.customerUrl()
//            .discountApplications()
        it.edited()
        it.email()
        it.financialStatus()
        it.fulfillmentStatus()
//                                .id()
//            .lineItems()
//            .metafield()
//            .metafields()
        it.name()
        it.orderNumber()
//            .originalTotalDuties()
//            .originalTotalPrice()
        it.phone()
        it.processedAt()
        it.shippingAddress {
            it.address1()
            it.address2()
            it.city()
            it.company()
            it.country()
            it.countryCodeV2()
            it.firstName()
            it.formatted()
            it.formattedArea()
//                                    .id()
            it.lastName()
            it.latitude()
            it.longitude()
            it.name()
            it.phone()
            it.province()
            it.provinceCode()
            it.zip()
        }
//            .shippingDiscountAllocations()
        it.statusUrl()
//            .subtotalPriceV2()
//            .successfulFulfillments()
//            .totalPriceV2()
//            .totalRefundedV2()
//            .totalShippingPriceV2()
//            .totalTaxV2()
    }
}