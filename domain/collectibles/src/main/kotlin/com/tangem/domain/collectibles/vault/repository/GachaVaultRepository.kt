package com.tangem.domain.collectibles.vault.repository

import arrow.core.Either
import com.tangem.domain.collectibles.delegate.HotDelegatePublicKey
import com.tangem.domain.collectibles.CollectiblesAccountError
import com.tangem.domain.collectibles.vault.GachaVaultLimits
import com.tangem.domain.models.account.CollectiblesVaultBalance
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWallet

/**
 * Access to a wallet's `tangem_gacha_vault` account.
 *
 * The vault's `cold_owner` is the key at `COLLECTIBLES_OWNER_DERIVATION_PATH`, not the wallet's ordinary Solana
 * account, and the vault address is `PDA(["vault", cold_owner])`. "Cold" is the program's wording: a mobile wallet
 * derives and signs the same key locally.
 */
interface GachaVaultRepository {

    /**
     * Brings the vault to the state [hotDelegate] needs: creates it when it is absent, re-points it when it
     * belongs to another device, leaves it alone when it is already right. For a device that already holds the
     * owner key, so the read costs no card session.
     *
     * Reading before signing is what keeps this at a single signature: `init_vault` over an existing vault fails

     */
    suspend fun initOrUpdateVault(
        userWallet: UserWallet,
        hotDelegate: HotDelegatePublicKey,
        limits: GachaVaultLimits,
    ): Either<CollectiblesAccountError, Unit>

    /**
     * Derives the owner key, then creates the vault or re-points its `hot_delegate`, all under one signature. The
     * read stays in here because without the derived key there is no address to read.
     */
    suspend fun deriveOwnerAndSetUpVault(
        userWallet: UserWallet,
        hotDelegate: HotDelegatePublicKey,
        limits: GachaVaultLimits,
    ): Either<CollectiblesAccountError, Unit>

    /**
     * What the vault holds. Read among the token balances of the vault address — the vault account itself

     * with the vault.
     *
     * @param cachedToken the token a previous read answered with. Working it out costs a request for the
     * program config, and the mint cannot change while the program is deployed.
     */
    suspend fun getVaultBalance(
        userWallet: UserWallet,
        cachedToken: CryptoCurrency.Token? = null,
    ): Either<CollectiblesAccountError, CollectiblesVaultBalance>

    /**
     * The address a top-up of the vault is sent to — its associated token account, not the vault itself: the
     * vault is a PDA owned by the gacha program, and a transfer addressed to it is refused as an unknown
     * destination.
     */
    suspend fun getVaultTopUpAddress(userWallet: UserWallet): Either<CollectiblesAccountError, String>

    /**
     * Closes the vault and returns its rent. Refused only while a buyback delegate is live (6014 `VaultNotClean`);
     * a vault that still owns token accounts closes successfully and strands their rent, so emptying it first is
     * the caller's job.
     */
    suspend fun closeVault(userWallet: UserWallet): Either<CollectiblesAccountError, Unit>
}