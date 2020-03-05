package com.tangem.blockchain.bitcoin

import com.google.common.truth.Truth
import com.tangem.common.extensions.hexToBytes
import org.junit.Test
import org.junit.jupiter.api.Assertions.*

class BitcoinAddressTest {

    @Test
    fun makeAddressFromCorrectPublicKey() {
        val walletPublicKey = "04752A727E14BBA5BD73B6714D72500F61FFD11026AD1196D2E1C54577CBEEAC3D11FC68A64700F8D533F4E311964EA8FB3AA26C588295F2133868D69C3E628693".hexToBytes()
        val expected = "1D3vYSjCvzrsVVK5bNaPTjU3NxcN7NNXMN"

        Truth.assertThat(BitcoinAddressFactory.makeAddress(walletPublicKey))
                .isEqualTo(expected)
    }

    @Test
    fun validateCorrectAddress() {
        val address = "1D3vYSjCvzrsVVK5bNaPTjU3NxcN7NNXMN"
        Truth.assertThat(BitcoinAddressValidator.validate(address))
                .isTrue()
    }
}