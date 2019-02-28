package com.tangem.data.network.model

import com.google.gson.annotations.SerializedName

data class AdaliteResponse(
        @SerializedName("Right")
        var right: AddressData? = null
)

data class AdaliteResponseUtxo(
        @SerializedName("Right")
        var right: List<UtxoData>
)

data class AddressData(
        @SerializedName("caAddress")
        var caAddress: String? = null,

        @SerializedName("caBalance")
        var caBalance: AdaliteCoins? = null
)

data class AdaliteCoins(
        @SerializedName("getCoin")
        var getCoin: Long? = null
)

data class UtxoData(
        @SerializedName("cuId")
        var cuId: String? = null,

        @SerializedName("cuOutIndex")
        var cuOutIndex: Int? = null,

        @SerializedName("cuCoins")
        var cuCoins: AdaliteCoins? = null
)