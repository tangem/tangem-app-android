package com.tangem.domain.wallet;

import android.util.Pair;

public class BalanceValidator {
    private String firstLine;
    private String secondLine;
    private int score;
    public String GetFirstLine()
    {
        return firstLine;
    }
    public String GetSecondLine()
    {
        if(score < 0) score = 0;
        if (score ==0) {
            return "Do not accept. " + secondLine;
        } else {
            return score + "% safe. " + secondLine;
        }
    }

    public void Check(TangemCard card)
    {
        String defaultFirstLine = "Unknown balance";
        String successFirstLine = "Verified balance";

        firstLine = successFirstLine;
        secondLine = "";

        // rule 1
        if(!CheckOfflineBalance(card) && !CheckOnlineBalance(card)) {
            score = 0;
            secondLine = "Balance cannot be verified.";
            firstLine = defaultFirstLine;
            return;
        }
        // rule 2.a
        if(!VerificationWalletKey(card)) {
            score = 0;
            secondLine = "Wallet verification failed.";
            firstLine = defaultFirstLine;
            return;
        }


        // rule 2.c
        if(!CheckAttestationServiceResult(card)) {
            score = 0;
            secondLine = "Do not accept. Tangem Attestation service says card is not genuine. ";
            firstLine = successFirstLine;
            return;
        }

        // rule 3
        if(CheckOnlineBalance(card) && !NotConfirmTransaction(card))
        {
            if(card.isBalanceRecieved() && card.isBalanceEqual()) {
                score = 100;
                firstLine = "Verified balance";
                secondLine += " Confirmed balance and banknote identity. ";
            }

            if(card.getFailedBalanceRequestCounter()!=0) {
                score = 100 - 5 * card.getFailedBalanceRequestCounter();
                firstLine = "Verified balance";
                secondLine += "Not all nodes have returned balance. Swipe down to refresh. ";
                if(score <= 0)
                    return;
            }
//
//            if(card.isBalanceRecieved() && !card.isBalanceEqual()) {
//                score = 0;
//                firstLine = "Disputed balance";
//                secondLine += " Cannot obtain trusted balance at the moment. Try to tap and check this banknote later.";
//                return;
//            }
        }

        // rule 7
        if(CheckOfflineBalance(card) && !CheckOnlineBalance(card))
        {
            score -= 10;
            String firstLine = "Verified offline balance";
            secondLine += "Restore internet connection to be more confidient. ";
            if(score <= 0)
                return;
        }

        // rule 2.b
        if(!CheckAttestationServiceAvailable(card))
        {
            score -= 15;
            secondLine += "Card identity was not verified. Cannot reach attestation service. ";
            firstLine = successFirstLine;
            if(score <= 0)
                return;
        }

        // rule 5
        if(IsLostSecondRead(card))
        {
            score -= 30;
            secondLine += "Wallet and banknote keys were not verified (tap again). ";
            if(score <= 0)
                return;
        }

        // rule 4
        if(!CheckSignHashes(card))
        {
            score -= 50;
            secondLine = "Unguaranteed balance";
            firstLine += "Potential unsent transaction at the moment. Try to tap and check this banknote later. ";
            if(score <= 0)
                return;
        }

        // rule 6
        if(NotConfirmTransaction(card))
        {
            score -= 50;
            String firstLine = "Unguaranted balance";
            secondLine += " Loading in progress. Wait for full confirmation in blockchain. ";
            if(score <= 0)
                return;
        }

    }

    boolean CheckOfflineBalance(TangemCard card)
    {
        return card.getOfflineBalance() != null;
    }

    boolean CheckOnlineBalance(TangemCard card)
    {
        return card.isBalanceRecieved();
    }

    boolean VerificationWalletKey(TangemCard card)
    {
        return card.isWalletPublicKeyValid();
    }

    boolean CheckSignHashes(TangemCard card)
    {
        return card.getSignHashes()==null;
    }

    boolean CheckAttestationServiceAvailable(TangemCard card)
    {
        return card.isOnlineVerified() != null;
    }

    boolean CheckAttestationServiceResult(TangemCard card)
    {
        return card.isOnlineVerified() != null && card.isOnlineVerified() == true;
    }

    boolean NotConfirmTransaction(TangemCard card)
    {
        return card.getBalanceUnconfirmed()!=0;
    }

    boolean IsLostSecondRead(TangemCard card)
    {
        return card.isCodeConfirmed() != null;
    }
}
