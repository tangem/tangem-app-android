package com.tangem.domain.wallet

import android.os.Build
import com.tangem.data.NFCLocation

class DeviceNFCAntennaLocation {
    companion object {
        const val CARD_ON_BACK = 0
        const val CARD_ON_FRONT = 1
        const val CARD_ORIENTATION_HORIZONTAL = 0
        const val CARD_ORIENTATION_VERTICAL = 1
    }

    var orientation: Int = 0
    var fullName: String = ""
    var x: Float = 0.toFloat()
    var y: Float = 0.toFloat()
    var z: Int = 0

    fun getAntennaLocation() {
        val codename = Build.DEVICE

        // default values
        this.orientation = 0
        this.fullName = ""
        this.x = 0.5f
        this.y = 0.35f
        this.z = 0

        for (nfcLocation in NFCLocation.values()) {
            if (codename == nfcLocation.codename) {
                this.fullName = nfcLocation.fullName
                this.orientation = nfcLocation.orientation
                this.x = nfcLocation.x / 100f
                this.y = nfcLocation.y / 100f
                this.z = nfcLocation.z
            }
        }
    }

}