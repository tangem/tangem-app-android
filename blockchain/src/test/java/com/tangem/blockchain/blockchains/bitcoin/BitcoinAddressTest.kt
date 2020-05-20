package com.tangem.blockchain.blockchains.bitcoin

import com.google.common.truth.Truth
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.extensions.hexToBytes
import org.junit.Test

class BitcoinAddressTest {

    private val addressService = BitcoinAddressService(Blockchain.Bitcoin)

    @Test
    fun makeAddressFromCorrectPublicKey() {
        val walletPublicKey = "04752A727E14BBA5BD73B6714D72500F61FFD11026AD1196D2E1C54577CBEEAC3D11FC68A64700F8D533F4E311964EA8FB3AA26C588295F2133868D69C3E628693".hexToBytes()
        val expected = "1D3vYSjCvzrsVVK5bNaPTjU3NxcN7NNXMN"

        Truth.assertThat(addressService.makeAddress(walletPublicKey))
                .isEqualTo(expected)
    }

    @Test
    fun validateCorrectAddress() {
        val address = "1D3vYSjCvzrsVVK5bNaPTjU3NxcN7NNXMN"
        Truth.assertThat(addressService.validate(address))
                .isTrue()
    }
}