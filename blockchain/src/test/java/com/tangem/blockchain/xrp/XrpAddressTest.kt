package com.tangem.blockchain.xrp

import com.google.common.truth.Truth
import com.tangem.common.extensions.hexToBytes
import org.junit.Test

internal class XrpAddressTest {
    @Test
    fun makeAddressFromCorrectSecpPublicKey() {
        val walletPublicKey = "04D2B9FB288540D54E5B32ECAF0381CD571F97F6F1ECD036B66BB11AA52FFE9981110D883080E2E255C6B1640586F7765E6FAA325D1340F49B56B83D9DE56BC7ED".hexToBytes()
        val expected = "rNxCXgKaCMAmowENKnYa5r8Ue78rjgrM6B"

        Truth.assertThat(XrpAddressFactory.makeAddress(walletPublicKey))
                .isEqualTo(expected)
    }

    @Test
    fun makeAddressFromCorrectEdPublicKey() {
        val walletPublicKey = "12CC4DE73BACF875D7423D152E46C1A665F1718CBE7CA0FEB2BA28C149E11909".hexToBytes()
        val expected = "rwWMNBs2GtJwfX7YNVV1sUYaPy6DRmDHB4"

        Truth.assertThat(XrpAddressFactory.makeAddress(walletPublicKey))
                .isEqualTo(expected)
    }

    @Test
    fun validateCorrectAddress() {
        val address = "rwWMNBs2GtJwfX7YNVV1sUYaPy6DRmDHB4"
        Truth.assertThat(XrpAddressValidator.validate(address))
                .isTrue()
    }
}