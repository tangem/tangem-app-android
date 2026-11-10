package com.tangem.data.tangempay.util

import com.tangem.domain.tangempay.model.ShippingAddress
import com.tangem.spend.datasource.tangempay.models.request.OrderRequest
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