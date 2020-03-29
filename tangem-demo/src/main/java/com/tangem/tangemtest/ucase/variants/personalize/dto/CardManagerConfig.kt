package com.tangem.tangemtest.ucase.variants.personalize.dto

import com.tangem.Config
import com.tangem.commands.personalization.entities.Issuer
import com.tangem.common.KeyPair

/**
[REDACTED_AUTHOR]
 */

interface CardManagerConfig {
    companion object {
        fun default(): Config {
            val issuer = issuer()
            return Config(
                    true,
                    issuer = issuer(),
                    issuerPublicKey = null,
                    acquirerKeyPair = acquirer(),
                    manufacturerKeyPair = manufacturer()
            )
        }

        private fun issuer(): Issuer {
            return Issuer(
                    name = "TANGEM SDK",
                    id = "TANGEM SDK" + "\u0000",
                    dataKeyPair = KeyPair(
                            "".toByteArray(),
                            "11121314151617184771ED81F2BACF57479E4735EB1405083927372D40DA9E92".toByteArray()
                    ),
                    transactionKeyPair = KeyPair(
                            "".toByteArray(),
                            "11121314151617184771ED81F2BACF57479E4735EB1405081918171615141312".toByteArray()
                    )
            )
        }

        private fun acquirer(): KeyPair {
            return KeyPair(
                    "".toByteArray(),
                    "21222324252627284771ED81F2BACF57479E4735EB1405083927372D40DA9E92".toByteArray()
            )
        }

        private fun manufacturer(): KeyPair {
            val manufacturer = Manufacturer(Manufacturer.Mode.Developer)
            return KeyPair(
                    manufacturer.publicKey,
                    manufacturer.privateKey
            )
        }
    }
}