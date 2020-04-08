package com.tangem.wallet.ducatus;

import android.net.Uri;
import android.text.InputFilter;
import android.util.Log;

import com.tangem.App;
import com.tangem.data.network.ServerApiBitcore;
import com.tangem.data.network.ServerApiInsight;
import com.tangem.data.network.model.BitcoreBalanceAndUnspents;
import com.tangem.data.network.model.BitcoreSendResponse;
import com.tangem.data.network.model.BitcoreUtxo;
import com.tangem.data.network.model.InsightResponse;
import com.tangem.data.network.model.InsightUtxo;
import com.tangem.tangem_card.data.TangemCard;
import com.tangem.tangem_card.reader.CardProtocol;
import com.tangem.tangem_card.tasks.SignTask;
import com.tangem.tangem_card.util.Util;
import com.tangem.util.CryptoUtil;
import com.tangem.util.DecimalDigitsInputFilter;
import com.tangem.util.DerEncodingUtil;
import com.tangem.wallet.BTCUtils;
import com.tangem.wallet.BalanceValidator;
import com.tangem.wallet.Base58;
import com.tangem.wallet.CoinData;
import com.tangem.wallet.R;
import com.tangem.wallet.TangemContext;
import com.tangem.wallet.Transaction;
import com.tangem.wallet.UnspentOutputInfo;
import com.tangem.wallet.btc.BtcData;
import com.tangem.wallet.btc.BtcEngine;
import com.tangem.wallet.btc.Unspents;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.reactivex.SingleObserver;
import io.reactivex.observers.DisposableSingleObserver;

public class DucatusEngine extends BtcEngine {
    private static final String TAG = DucatusEngine.class.getSimpleName();
    public DucatusData coinData = null;

    public DucatusEngine(TangemContext context) throws Exception {
        super(context);
        if (context.getCoinData() == null) {
            coinData = new DucatusData();
            context.setCoinData(coinData);
        } else if (context.getCoinData() instanceof BtcData) {
            coinData = (DucatusData) context.getCoinData();
        } else {
            throw new Exception("Invalid type of Blockchain data for DucatusEngine");
        }
    }

    public DucatusEngine() {
        super();
    }

    private static int getDecimals() {
        return 8;
    }


    private void checkBlockchainDataExists() throws Exception {
        if (coinData == null) throw new Exception("No blockchain data");
    }

    @Override
    public boolean awaitingConfirmation() {
        if (coinData == null) return false;
        return coinData.getBalanceUnconfirmed() != 0 || App.pendingTransactionsStorage.hasTransactions(ctx.getCard());
    }

    @Override
    public String getBalanceHTML() {
        Amount balance = getBalance();
        if (balance != null) {
            return balance.toDescriptionString(getDecimals());
        } else {
            return "";
        }
    }

    @Override
    public String getBalanceCurrency() {
        return "DUC";
    }

    @Override
    public boolean isBalanceNotZero() {
        if (coinData == null) return false;
        if (coinData.getBalanceInInternalUnits() == null) return false;
        return coinData.getBalanceInInternalUnits().notZero();
    }

    @Override
    public boolean hasBalanceInfo() {
        if (coinData == null) return false;
        return coinData.hasBalanceInfo();
    }


    @Override
    public boolean isExtractPossible() {
        if (!hasBalanceInfo()) {
            ctx.setMessage(R.string.loaded_wallet_error_obtaining_blockchain_data);
        } else if (!isBalanceNotZero()) {
            ctx.setMessage(R.string.general_wallet_empty);
        } else if (awaitingConfirmation()) {
            ctx.setMessage(R.string.loaded_wallet_message_wait);
        } else if (coinData.getUnspentTransactions().size() == 0) {
            ctx.setMessage(R.string.loaded_wallet_message_refresh);
        } else {
            return true;
        }
        return false;
    }

    @Override
    public String getFeeCurrency() {
        return "DUC";
    }

    @Override
    public boolean validateAddress(String address) {
        if (address == null || address.isEmpty()) {
            return false;
        }

        if (address.length() < 25) {
            return false;
        }

        if (address.length() > 35) {
            return false;
        }

        if (!address.startsWith("L") && !address.startsWith("M")) {
            return false;
        }

        byte[] decAddress = Base58.decodeBase58(address);

        if (decAddress == null || decAddress.length == 0) {
            return false;
        }

        byte[] rip = new byte[21];
        for (int i = 0; i < 21; ++i) {
            rip[i] = decAddress[i];
        }

        byte[] kcv = CryptoUtil.doubleSha256(rip);

        for (int i = 0; i < 4; ++i) {
            if (kcv[i] != decAddress[21 + i])
                return false;
        }

        return true;
    }


    @Override
    public boolean isNeedCheckNode() {
        return true;
    }

    @Override
    public Uri getWalletExplorerUri() {
        return Uri.parse("https://insight.ducatus.io/#/DUC/mainnet/address/" + ctx.getCoinData().getWallet());
    }

    @Override
    public Uri getShareWalletUri() {
        return Uri.parse(ctx.getCoinData().getWallet());
    }

    @Override
    public InputFilter[] getAmountInputFilters() {
        return new InputFilter[]{new DecimalDigitsInputFilter(getDecimals())};
    }

    @Override
    public boolean checkNewTransactionAmount(Amount amount) {
        if (coinData == null) return false;
        if (amount.compareTo(convertToAmount(coinData.getBalanceInInternalUnits())) > 0) {
            return false;
        }
        return true;
    }

    @Override
    public boolean checkNewTransactionAmountAndFee(Amount amountValue, Amount feeValue, Boolean isIncludeFee) {
        InternalAmount fee;
        InternalAmount amount;

        try {
            checkBlockchainDataExists();
            amount = convertToInternalAmount(amountValue);
            fee = convertToInternalAmount(feeValue);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        if (fee == null || amount == null)
            return false;

        if (fee.isZero() || amount.isZero())
            return false;

        if (isIncludeFee && (amount.compareTo(coinData.getBalanceInInternalUnits()) > 0 || amount.compareTo(fee) < 0))
            return false;

        if (!isIncludeFee && amount.add(fee).compareTo(coinData.getBalanceInInternalUnits()) > 0)
            return false;

        return true;
    }

    @Override
    public boolean validateBalance(BalanceValidator balanceValidator) {
        if (((ctx.getCard().getOfflineBalance() == null) && !ctx.getCoinData().isBalanceReceived()) || (!ctx.getCoinData().isBalanceReceived() && (ctx.getCard().getRemainingSignatures() != ctx.getCard().getMaxSignatures()))) {
            balanceValidator.setScore(0);
            balanceValidator.setFirstLine(R.string.balance_validator_first_line_unknown_balance);
            balanceValidator.setSecondLine(R.string.balance_validator_second_line_unverified_balance);
            return false;
        }

        // Workaround before new back-end
//        if (card.getRemainingSignatures() == card.getMaxSignatures()) {
//            firstLine = "Verified balance";
//            secondLine = "Balance confirmed in blockchain. ";
//            secondLine += "Verified note identity. ";
//            return;
//        }

        if (coinData.getBalanceUnconfirmed() != 0) {
            balanceValidator.setScore(0);
            balanceValidator.setFirstLine(R.string.balance_validator_first_line_transaction_in_progress);
            balanceValidator.setSecondLine(R.string.balance_validator_second_line_wait_for_confirmation);
            return false;
        }

        if (coinData.isBalanceReceived()) {// && coinData.isBalanceEqual()) { TODO:check
            balanceValidator.setScore(100);
            balanceValidator.setFirstLine(R.string.balance_validator_first_line_verified_balance);
            balanceValidator.setSecondLine(R.string.balance_validator_second_line_confirmed_in_blockchain);
            if (coinData.getBalanceInInternalUnits().isZero()) {
                balanceValidator.setFirstLine(R.string.balance_validator_first_line_empty_wallet);
                balanceValidator.setSecondLine(R.string.empty_string);
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

//        if ((ctx.getCard().getOfflineBalance() != null) && !coinData.isBalanceReceived() && ctx.getCard().getRemainingSignatures() == ctx.getCard().getMaxSignatures()) {
//            balanceValidator.setScore(80);
//            balanceValidator.setFirstLine(R.string.balance_validator_first_line_verified_offline);
//            balanceValidator.setSecondLine(R.string.balance_validator_second_line_internet_to_get_balance);
//        }

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

        return true;
    }

    @Override
    public Amount getBalance() {
        if (!hasBalanceInfo()) return null;
        return convertToAmount(coinData.getBalanceInInternalUnits());
    }

    @Override
    public String evaluateFeeEquivalent(String fee) {
        if (!coinData.getAmountEquivalentDescriptionAvailable()) return "";
        try {
            Amount feeAmount = new Amount(fee, getFeeCurrency());
            return feeAmount.toEquivalentString(coinData.getRate());
        } catch (Exception e) {
            return "";
        }
    }

    @Override
    public String getBalanceEquivalent() {
        if (coinData == null || !coinData.getAmountEquivalentDescriptionAvailable()) return "";
        Amount balance = getBalance();
        if (balance == null) return "";
        return balance.toEquivalentString(coinData.getRate());
    }

    @Override
    public String calculateAddress(byte[] pkUncompressed) throws NoSuchProviderException, NoSuchAlgorithmException {
        byte netSelectionByte = (byte) 0x31;

        byte hash1[] = Util.calculateSHA256(pkUncompressed);
        byte hash2[] = Util.calculateRIPEMD160(hash1);

        ByteBuffer BB = ByteBuffer.allocate(hash2.length + 1);

        BB.put(netSelectionByte);
        BB.put(hash2);

        byte hash3[] = Util.calculateSHA256(BB.array());
        byte hash4[] = Util.calculateSHA256(hash3);

        BB = ByteBuffer.allocate(hash2.length + 5);
        BB.put(netSelectionByte); //BB.put((byte) 0x6f);
        BB.put(hash2);
        BB.put(hash4[0]);
        BB.put(hash4[1]);
        BB.put(hash4[2]);
        BB.put(hash4[3]);

        return org.bitcoinj.core.Base58.encode(BB.array());
    }

    @Override
    public Amount convertToAmount(InternalAmount internalAmount) {
        BigDecimal d = internalAmount.divide(new BigDecimal("100000000"));
        return new Amount(d, getBalanceCurrency());
    }

    @Override
    public Amount convertToAmount(String strAmount, String currency) {
        return new Amount(strAmount, currency);
    }

    @Override
    public InternalAmount convertToInternalAmount(Amount amount) {
        BigDecimal d = amount.multiply(new BigDecimal("100000000"));
        return new InternalAmount(d, "Satoshi");
    }

    @Override
    public InternalAmount convertToInternalAmount(byte[] bytes) {
        if (bytes == null) return null;
        byte[] reversed = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++) reversed[i] = bytes[bytes.length - i - 1];
        return new InternalAmount(Util.byteArrayToLong(reversed), "Satoshi");
    }

    @Override
    public byte[] convertToByteArray(InternalAmount internalAmount) {
        byte[] bytes = Util.longToByteArray(internalAmount.longValueExact());
        byte[] reversed = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++) reversed[i] = bytes[bytes.length - i - 1];
        return reversed;
    }

    @Override
    public CoinData createCoinData() {
        return new DucatusData();
    }

    @Override
    public String getUnspentInputsDescription() {
        Unspents unspents = coinData.getUnspentInputsDescription();
        if (unspents == null) {
            return "";
        } else {
            return String.format(
                    ctx.getContext().getString(R.string.details_unspents_number),
                    unspents.getUnspetns(),
                    unspents.getGatheredUnspents());
        }
    }

    @Override
    public SignTask.TransactionToSign constructTransaction(Amount amountValue, Amount feeValue, boolean IncFee, String targetAddress) throws Exception {
        ArrayList<UnspentOutputInfo> unspentOutputs = new ArrayList<>();
        checkBlockchainDataExists();

        String myAddress = ctx.getCoinData().getWallet();
        byte[] pbKey = ctx.getCard().getWalletPublicKey();

        for (BtcData.UnspentTransaction utxo : coinData.getUnspentTransactions()) {
            unspentOutputs.add(new UnspentOutputInfo(BTCUtils.fromHex(utxo.txID), new Transaction.Script(BTCUtils.fromHex(utxo.script)), utxo.amount, utxo.outputN, -1, utxo.txID, null));
        }

        long fullAmount = 0;
        for (int i = 0; i < unspentOutputs.size(); ++i) {
            fullAmount += unspentOutputs.get(i).value;
        }

        long fees = convertToInternalAmount(feeValue).longValueExact();
        long amount = convertToInternalAmount(amountValue).longValueExact();
        long change = fullAmount - amount;
        if (IncFee) {
            amount = amount - fees;
        } else {
            change = change - fees;
        }

        final long amountFinal = amount;
        final long changeFinal = change;

        if (amount + fees > fullAmount) {
            throw new CardProtocol.TangemException_WrongAmount(String.format("Balance (%d) < change (%d) + amount (%d)", fullAmount, change, amount));
        }

        final byte[][] txForSign = new byte[unspentOutputs.size()][];
        final byte[][] bodyDoubleHash = new byte[unspentOutputs.size()][];
        final byte[][] bodyHash = new byte[unspentOutputs.size()][];

        for (int i = 0; i < unspentOutputs.size(); ++i) {
            txForSign[i] = BTCUtils.buildTXForSign(myAddress, targetAddress, myAddress, unspentOutputs, i, amount, change);
            bodyHash[i] = Util.calculateSHA256(txForSign[i]);
            bodyDoubleHash[i] = Util.calculateSHA256(bodyHash[i]);
        }

        return new SignTask.TransactionToSign() {

            @Override
            public boolean isSigningMethodSupported(TangemCard.SigningMethod signingMethod) {
                return signingMethod == TangemCard.SigningMethod.Sign_Hash || signingMethod == TangemCard.SigningMethod.Sign_Raw;
            }

            @Override
            public byte[][] getHashesToSign() throws Exception {
                byte[][] dataForSign = new byte[unspentOutputs.size()][];
                if (txForSign.length > 10)
                    throw new Exception("To much hashes in one transaction!");
                for (int i = 0; i < unspentOutputs.size(); ++i) {
                    dataForSign[i] = bodyDoubleHash[i];
                }
                return dataForSign;
            }

            @Override
            public byte[] getRawDataToSign() throws Exception {
                ByteArrayOutputStream bs = new ByteArrayOutputStream();
                for (int i = 0; i < txForSign.length; i++) {
                    if (i != 0 && txForSign[0].length != txForSign[i].length)
                        throw new Exception("Hashes length must be identical!");
                    bs.write(txForSign[i]);
                }

                return bs.toByteArray();
            }

            @Override
            public String getHashAlgToSign() {
                return "sha-256x2";
            }

            @Override
            public byte[] getIssuerTransactionSignature(byte[] dataToSignByIssuer) throws Exception {
                throw new Exception("Issuer validation not supported!");
            }

            @Override
            public byte[] onSignCompleted(byte[] signFromCard) throws Exception {
                for (int i = 0; i < unspentOutputs.size(); ++i) {
                    BigInteger r = new BigInteger(1, Arrays.copyOfRange(signFromCard, i * 64, 32 + i * 64));
                    BigInteger s = new BigInteger(1, Arrays.copyOfRange(signFromCard, 32 + i * 64, 64 + i * 64));
                    s = CryptoUtil.toCanonicalised(s);

                    unspentOutputs.get(i).scriptForBuild = DerEncodingUtil.packSignDer(r, s, pbKey);
                }

                byte[] txForSend = BTCUtils.buildTXForSend(targetAddress, myAddress, unspentOutputs, amountFinal, changeFinal);
                notifyOnNeedSendTransaction(txForSend);
                return txForSend;
            }
        };
    }

    @Override
    public void requestBalanceAndUnspentTransactions(BlockchainRequestsCallbacks blockchainRequestsCallbacks) {
        SingleObserver<BitcoreBalanceAndUnspents> balanceAndUnspentsObserver = new DisposableSingleObserver<BitcoreBalanceAndUnspents>() {
            @Override
            public void onSuccess(BitcoreBalanceAndUnspents balanceAndUnspents) {
                try {
                    coinData.setBalanceConfirmed(balanceAndUnspents.getBalance().getConfirmed());
                    coinData.setBalanceUnconfirmed(balanceAndUnspents.getBalance().getUnconfirmed());
                    coinData.setBalanceReceived(true);

                    for (BitcoreUtxo utxo : balanceAndUnspents.getUnspents()) {
                        BtcData.UnspentTransaction trUnspent = new BtcData.UnspentTransaction();
                        trUnspent.txID = utxo.getMintTxid();
                        trUnspent.amount = utxo.getValue();
                        trUnspent.outputN = utxo.getMintIndex();
                        trUnspent.script = utxo.getScript();
                        coinData.getUnspentTransactions().add(trUnspent);
                    }
                    blockchainRequestsCallbacks.onComplete(true);

                } catch (Exception e) {
                    Log.e(TAG, "FAIL BITCORE_BALANCE_AND_UNSPENTS Exception");
                    e.printStackTrace();
                    ctx.setError(e.getMessage());
                    blockchainRequestsCallbacks.onComplete(false);
                }
            }

            @Override
            public void onError(Throwable e) {
                Log.e(TAG, "FAIL BITCORE_BALANCE_AND_UNSPENTS Exception");
                e.printStackTrace();
                ctx.setError(e.getMessage());
                blockchainRequestsCallbacks.onComplete(false);
            }
        };

        ServerApiBitcore serverApiBitcore = new ServerApiBitcore();
        serverApiBitcore.getBalanceAndUnspents(coinData.getWallet(), balanceAndUnspentsObserver);
    }

//    private final static BigDecimal relayFee = new BigDecimal(0.00001);

    @Override
    public void requestFee(BlockchainRequestsCallbacks blockchainRequestsCallbacks, String targetAddress, Amount amount) throws Exception {
        final int calcSize = calculateEstimatedTransactionSize(targetAddress, amount.toValueString());
        Log.e(TAG, String.format("Estimated tx size %d", calcSize));

        coinData.minFee = new Amount(BigDecimal.valueOf(calcSize).multiply(BigDecimal.valueOf(0.00000089)), ctx.getBlockchain().getCurrency()); //fee for byte from Ducatus wallet for android
        coinData.normalFee = new Amount(BigDecimal.valueOf(calcSize).multiply(BigDecimal.valueOf(0.00000144)), ctx.getBlockchain().getCurrency());
        coinData.maxFee = new Amount(BigDecimal.valueOf(calcSize).multiply(BigDecimal.valueOf(0.00000350)), ctx.getBlockchain().getCurrency());

        blockchainRequestsCallbacks.onComplete(true);
    }

    @Override
    public void requestSendTransaction(BlockchainRequestsCallbacks blockchainRequestsCallbacks, byte[] txForSend) {
        final String txStr = BTCUtils.toHex(txForSend);

        SingleObserver<BitcoreSendResponse> sendResponseObserver = new DisposableSingleObserver<BitcoreSendResponse>() {
            @Override
            public void onSuccess(BitcoreSendResponse sendResponse) {
                if (sendResponse.getTxid() != null) {
                    blockchainRequestsCallbacks.onComplete(true);
                } else {
                    ctx.setError("Unknown send error");
                    blockchainRequestsCallbacks.onComplete(false);
                }
            }

            @Override
            public void onError(Throwable e) {
                Log.e(TAG, "onError: Bitcore sendTransaction" + e.getMessage());
                ctx.setError(e.getMessage());
                blockchainRequestsCallbacks.onComplete(false);
            }
        };

        ServerApiBitcore serverApiBitcore = new ServerApiBitcore();
        serverApiBitcore.sendTransaction(txStr, sendResponseObserver);
    }
}