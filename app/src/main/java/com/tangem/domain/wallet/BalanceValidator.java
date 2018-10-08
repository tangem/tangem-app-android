package com.tangem.domain.wallet;

import com.tangem.wallet.R;

import java.math.BigInteger;

public class BalanceValidator {
    private String firstLine;
    private String secondLine;
    private int score;

    public String getFirstLine() {
        return firstLine;
    }

    public String getSecondLine(Boolean recommend) {
        if (!recommend) return secondLine;
        if (score > 89) {
            return "Safe to accept. " + secondLine;
        } else if (score > 74) {
            return "Not fully safe to accept. " + secondLine;
        } else if (score > 30) {
            return "Not safe to accept. " + secondLine;
        } else {
            return "Do not accept! " + secondLine;
        }
    }

    public int getColor() {
        if (score > 89) {
            return R.color.confirmed;
        } else if (score > 74) {
            return android.R.color.holo_orange_light;
        } else if (score > 0) {
            return android.R.color.holo_orange_dark;
        } else {
            return android.R.color.holo_red_light;
        }
    }

    public void Check(TangemCard card, Boolean attest) {
        firstLine = "Verification failed";
        secondLine = "";

        if (card.getBlockchain() == Blockchain.Bitcoin || card.getBlockchain() == Blockchain.BitcoinTestNet) {

            if (((card.getOfflineBalance() == null) && !card.isBalanceReceived()) || (!card.isBalanceReceived() && (card.getRemainingSignatures() != card.getMaxSignatures()))) {
                score = 0;
                firstLine = "Unknown balance";
                secondLine = "Balance cannot be verified. Swipe down to refresh.";
                return;
            }

            // Workaround before new back-end
//        if (card.getRemainingSignatures() == card.getMaxSignatures()) {
//            firstLine = "Verified balance";
//            secondLine = "Balance confirmed in blockchain. ";
//            secondLine += "Verified note identity. ";
//            return;
//        }

            if (card.getBalanceUnconfirmed() != 0) {
                score = 0;
                firstLine = "Transaction in progress";
                secondLine = "Wait for full confirmation in blockchain. ";
                return;
            }

            if (card.isBalanceReceived() && card.isBalanceEqual()) {
                score = 100;
                firstLine = "Verified balance";
                secondLine = "Balance confirmed in blockchain. ";
                if (card.getBalance() == 0) {
                    firstLine = "Empty wallet";
                    secondLine = "";
                }
            }

            // rule 4 TODO: need to check SignedHashed against number of outputs in blockchain
//        if((card.getRemainingSignatures() != card.getMaxSignatures()) && card.getBalance() != 0)
//        {
//            score = 80;
//            firstLine = "Unguaranteed balance";
//            secondLine = "Potential unsent transaction. Redeem immediately if accept. ";
//            return;
//        }

            if ((card.getOfflineBalance() != null) && !card.isBalanceReceived() && (card.getRemainingSignatures() == card.getMaxSignatures()) && card.getBalance() != 0) {
                score = 80;
                firstLine = "Verified offline balance";
                secondLine = "Can't obtain balance from blockchain. Restore internet connection to be more confident. ";
            }

//            if(card.getFailedBalanceRequestCounter()!=0) {
//                score -= 5 * card.getFailedBalanceRequestCounter();
//                secondLine += "Not all nodes have returned balance. Swipe down or tap again. ";
//                if(score <= 0)
//                    return;
//            }

            //
//            if(card.isBalanceReceived() && !card.isBalanceEqual()) {
//                score = 0;
//                firstLine = "Disputed balance";
//                secondLine += " Cannot obtain trusted balance at the moment. Try to tap and check this banknote later.";
//                return;
//            }
        } else if ((card.getBlockchain()  == Blockchain.Ethereum) || (card.getBlockchain()  == Blockchain.Token)) {

            if (card.getBalance() == null) {
                score = 0;
                firstLine = "Unknown balance";
                secondLine = "Balance cannot be verified. Swipe down to refresh.";
                return;
            }

            if (!card.getUnconfirmedTXCount().equals(card.getConfirmedTXCount())) {
                score = 0;
                firstLine = "Unguaranteed balance";
                secondLine = "Transaction is in progress. Wait for confirmation in blockchain. ";
                return;
            }

            if (card.isBalanceReceived()) {
                score = 100;
                firstLine = "Verified balance";
                secondLine = "Balance confirmed in blockchain. ";
                if (card.getBalance() == 0) {
                    firstLine = "Empty wallet";
                    secondLine = "";
                }
            }

            if ((card.getOfflineBalance() != null) && !card.isBalanceReceived() && (card.getRemainingSignatures() == card.getMaxSignatures()) && card.getBalance() != 0) {
                score = 80;
                firstLine = "Verified offline balance";
                secondLine = "Restore internet connection to obtain trusted balance from blockchain";
            }
        }

        // Verify card?
        if (attest) {

            if (!card.isWalletPublicKeyValid()) {
                score = 0;
                firstLine = "Verification failed";
                secondLine = "Wallet verification failed. Tap again.";
                return;
            }

            if (card.isOnlineVerified() != null && !card.isOnlineVerified()) {
                score = 0;
                firstLine = "Not genuine banknote";
                secondLine = "Tangem Attestation service says the banknote is not genuine.";
                return;
            }

            if (card.isCodeConfirmed() != null && !card.isCodeConfirmed()) {
                score = 0;
                firstLine = "Not genuine banknote";
                secondLine = "Firmware binary code verification failed";
                return;
            }

            if (card.PIN2 == TangemCard.PIN2_Mode.CustomPIN2) {
                score = 0;
                firstLine = "Locked with PIN2";
                secondLine = "Ask the holder to disable PIN2 before accepting";
                return;
            }

            // rule 2.b
            if (card.isOnlineVerified()) {
                secondLine += "Verified note identity. ";
            } else {
                score = 80;
                secondLine += "Card identity was not verified. Cannot reach Tangem attestation service. ";
            }
        }

    }
}
