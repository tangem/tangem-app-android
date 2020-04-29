package com.tangem.blockchain.blockchains.bitcoincash

import com.google.common.truth.Truth
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.extensions.toCompressedPublicKey
import org.junit.Test

class BitcoinCashAddressTest {

    private val addressService = BitcoinCashAddressService()

    @Test
    fun makeAddressFromCorrectPublicKey() {
        val walletPublicKey = "04BE37CD5251C8999EDBBFC759D800EB41E4DCB718289601EB15819404E1B2F2ED90FE50C2A481D06EC790D1EF6184974EB655ABAE4BE56A6D1C9E1A17B1EFDF02".hexToBytes()
        val expected = "bitcoincash:qp7atyzvetwq8a0x02y2snvnns5jfwnzacf9vfa4x3"

        Truth.assertThat(addressService.makeAddress(walletPublicKey.toCompressedPublicKey()))
                .isEqualTo(expected)
    }

    @Test
    fun validateCorrectAddress() {
        val address = "bitcoincash:qp7atyzvetwq8a0x02y2snvnns5jfwnzacf9vfa4x3"
        Truth.assertThat(addressService.validate(address))
                .isTrue()
    }
}