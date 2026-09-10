package com.tangem.domain.jointaccount.model

/** Why creating a joint operation stopped. Every variant blocks the creation — none is skippable. */
sealed interface JointOperationCreateError {

    /** The (account, network) slot already holds a non-terminal operation — one at a time */
    data object SlotBusy : JointOperationCreateError

    /** The account is not `active`, or its Safe address is not known yet */
    data object AccountNotReady : JointOperationCreateError

    /** The on-chain Safe does not match the composition the app knows — never sign against such a contract */
    data object CompositionMismatch : JointOperationCreateError

    /** The transfer is not a plain native send or an ERC-20 `transfer`, or the Safe would call itself */
    data object InvalidPayload : JointOperationCreateError

    /**
     * The network state moved between building and sending (`NONCE_MISMATCH` / `GAS_PRICE_TOO_LOW`) and
     * the one rebuild-and-resign retry hit it again.
     */
    data object NetworkStateMoved : JointOperationCreateError

    /** The user dismissed the signing dialog */
    data object UserCancelled : JointOperationCreateError

    data class Failed(val cause: Throwable) : JointOperationCreateError
}