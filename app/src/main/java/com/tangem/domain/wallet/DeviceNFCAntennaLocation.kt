package com.tangem.domain.wallet

import android.os.Build
import com.tangem.domain.NFCLocation

class DeviceNFCAntennaLocation {
    val TAG: String = DeviceNFCAntennaLocation::class.java.simpleName

    var orientation: Int = 0
    var fullName: String = ""
    var x: Float = 0.toFloat()
    var y: Float = 0.toFloat()
    var z: Float = 0.toFloat()
    var onBackSide: Boolean = false
    var strength: Float = 0.toFloat()

    fun getAntennaLocation() {
        val codename = Build.DEVICE

        // default values
        this.orientation = 0
        this.fullName = ""
        this.x = 0.5f
        this.y = 0.35f
        this.z = 0f
        this.onBackSide = true
        this.strength = 1.0f

        for (nfcLocation in NFCLocation.values()) {
            if (codename == nfcLocation.codename) {
                this.fullName = nfcLocation.fullName
                this.orientation = nfcLocation.orientation
                this.x = nfcLocation.x / 100f
                this.y = nfcLocation.y / 100f
                this.z = nfcLocation.z / 100f
            }
        }
    }

}