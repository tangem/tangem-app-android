package com.tangem.data.network.model

import com.google.gson.annotations.SerializedName

data class AdaliteResponse(
        @SerializedName("Right")
        var right: AdaliteAddressData? = null
)

data class AdaliteResponseUtxo(
        @SerializedName("Right")
        var right: List<AdaliteUtxoData>
)

data class AdaliteAddressData(
        @SerializedName("caAddress")
        var caAddress: String? = null,

        @SerializedName("caBalance")
        var caBalance: AdaliteCoins? = null,

        @SerializedName("caTxList")
        var caTxList: List<AdaliteTxData>
)

data class AdaliteCoins(
        @SerializedName("getCoin")
        var getCoin: Long? = null
)

data class AdaliteUtxoData(
        @SerializedName("cuId")
        var cuId: String? = null,

        @SerializedName("cuOutIndex")
        var cuOutIndex: Int? = null,

        @SerializedName("cuCoins")
        var cuCoins: AdaliteCoins? = null
)

data class AdaliteTxData(
        @SerializedName("ctbId")
        var ctbId: String? = null
)