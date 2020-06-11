package com.tangem.data.network.model

import com.google.gson.annotations.SerializedName

data class PayIdResponse(
        @SerializedName("addresses")
        var addresses: List<PayIdAddress>? = null
)

data class PayIdAddress(
        @SerializedName("addressDetails")
        var addressDetails: PayIdAddressDetails? = null
)

data class PayIdAddressDetails(
        @SerializedName("address")
        var address: String? = null
)