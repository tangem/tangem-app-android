package com.tangem.blockchain.stellar

import com.google.common.truth.Truth
import com.tangem.common.extensions.hexToBytes
import org.junit.Test

internal class StellarAddressTest {
    @Test
    fun makeAddressFromCorrectPublicKey() {
        val walletPublicKey = "EC5387D8B38BD9EF80BDBC78D0D7E1C53F08E269436C99D5B3C2DF4B2CE73012".hexToBytes()
        val expected = "GDWFHB6YWOF5T34AXW6HRUGX4HCT6CHCNFBWZGOVWPBN6SZM44YBFUDZ"

        Truth.assertThat(StellarAddressFactory.makeAddress(walletPublicKey))
                .isEqualTo(expected)
    }

    @Test
    fun validateCorrectAddress() {
        val address = "GDWFHB6YWOF5T34AXW6HRUGX4HCT6CHCNFBWZGOVWPBN6SZM44YBFUDZ"
        Truth.assertThat(StellarAddressValidator.validate(address))
                .isTrue()
    }
}