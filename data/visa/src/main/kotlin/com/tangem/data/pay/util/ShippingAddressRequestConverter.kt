package com.tangem.data.pay.util

import com.tangem.domain.pay.model.ShippingAddress
import com.tangem.spend.datasource.pay.models.request.OrderRequest
import com.tangem.utils.converter.Converter

internal object ShippingAddressRequestConverter : Converter<ShippingAddress, OrderRequest.ShippingAddress> {

    override fun convert(value: ShippingAddress): OrderRequest.ShippingAddress = OrderRequest.ShippingAddress(
        firstName = value.firstName,
        lastName = value.lastName,
        line1 = value.line1,
        line2 = value.line2,
        city = value.city,
        region = value.region,
        postalCode = value.postalCode,
        phoneNumber = value.phone,
    )
}