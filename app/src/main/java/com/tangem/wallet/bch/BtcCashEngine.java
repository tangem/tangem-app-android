package com.tangem.wallet.bch;

import android.net.Uri;
import android.text.InputFilter;
import android.util.Log;

import com.tangem.App;
import com.tangem.data.network.ServerApiBlockchair;
import com.tangem.data.network.model.BlockchairAddressData;
import com.tangem.data.network.model.BlockchairAddressResponse;
import com.tangem.data.network.model.BlockchairStatsResponse;
import com.tangem.data.network.model.BlockchairTransactionResponse;
import com.tangem.data.network.model.BlockchairUnspentOutput;
import com.tangem.tangem_card.data.TangemCard;
import com.tangem.tangem_card.reader.CardProtocol;
import com.tangem.tangem_card.tasks.SignTask;
import com.tangem.tangem_card.util.Util;
import com.tangem.util.CryptoUtil;
import com.tangem.util.DecimalDigitsInputFilter;
import com.tangem.util.DerEncodingUtil;
import com.tangem.wallet.BCHUtils;
import com.tangem.wallet.BTCUtils;
import com.tangem.wallet.BalanceValidator;
import com.tangem.wallet.CoinData;
import com.tangem.wallet.CoinEngine;
import com.tangem.wallet.R;
import com.tangem.wallet.TangemContext;
import com.tangem.wallet.Transaction;
import com.tangem.wallet.UnspentOutputInfo;
import com.tangem.wallet.btc.BtcData;
import com.tangem.wallet.btc.Unspents;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.reactivex.CompletableObserver;
import io.reactivex.SingleObserver;
import io.reactivex.observers.DisposableCompletableObserver;
import io.reactivex.observers.DisposableSingleObserver;

public class BtcCashEngine extends CoinEngine {

    private static final String TAG = BtcCashEngine.class.getSimpleName();
    public BtcData coinData = null;

    public BtcCashEngine(TangemContext context) throws Exception {
        super(context);
        if (context.getCoinData() == null) {
            coinData = new BtcData();
            context.setCoinData(coinData);
        } else if (context.getCoinData() instanceof BtcData) {
            coinData = (BtcData) context.getCoinData();
        } else {
            throw new Exception("Invalid type of Blockchain data for BtcEngine");
        }
    }

    public BtcCashEngine() {
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
        return coinData.getBalanceUnconfirmed() != 0 || coinData.isHasUnconfirmed() || App.pendingTransactionsStorage.hasTransactions(ctx.getCard());
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
        return "BCH";
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
        return "BCH";
    }

    @Override
    public boolean validateAddress(String address) {
        return CashAddr.isValidCashAddress(address);
    }

    @Override
    public boolean isNeedCheckNode() {
        return true;
    }

    @Override
    public Uri getWalletExplorerUri() {
        return Uri.parse("https://api.blockchair.com/bitcoin-cash/dashboards/address/" + ctx.getCoinData().getWallet());
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

        if (coinData.getBalanceUnconfirmed() != 0 || coinData.isHasUnconfirmed()) {
            balanceValidator.setScore(0);
            balanceValidator.setFirstLine(R.string.balance_validator_first_line_transaction_in_progress);
            balanceValidator.setSecondLine(R.string.balance_validator_second_line_wait_for_confirmation);
            return false;
        }

        if (coinData.isBalanceReceived() && coinData.isBalanceEqual()) {
            balanceValidator.setScore(100);
            balanceValidator.setFirstLine(R.string.balance_validator_first_line_verified_balance);
            balanceValidator.setSecondLine(R.string.balance_validator_second_line_confirmed_in_blockchain);
            if (coinData.getBalanceInInternalUnits().isZero()) {
                balanceValidator.setFirstLine(R.string.balance_validator_first_line_empty_wallet);
                balanceValidator.setSecondLine(R.string.empty_string);
            }
        }
        return true;
    }

    @Override
    public Amount getBalance() {
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
    public String calculateAddress(byte[] pubKey) throws NoSuchProviderException, NoSuchAlgorithmException {

        // CashAddr format
        byte hash1[] = Util.calculateSHA256(pubKey);
        byte hash2[] = Util.calculateRIPEMD160(hash1);
        return CashAddr.toCashAddress(BitcoinCashAddressType.P2PKH, hash2);

    }

    public String calculateLegacyAddress(byte[] pkUncompressed) throws NoSuchProviderException, NoSuchAlgorithmException {

        // Legacy format (BTC)
        byte netSelectionByte = (byte) 0x00;

        byte hash1[] = Util.calculateSHA256(pkUncompressed);
        byte hash2[] = Util.calculateRIPEMD160(hash1);

        ByteBuffer BB = ByteBuffer.allocate(hash2.length + 1);

        BB.put(netSelectionByte);
        BB.put(hash2);

        byte hash3[] = Util.calculateSHA256(BB.array());
        byte hash4[] = Util.calculateSHA256(hash3);

        BB = ByteBuffer.allocate(hash2.length + 5);
        BB.put(netSelectionByte);
        BB.put(hash2);
        BB.put(hash4[0]);
        BB.put(hash4[1]);
        BB.put(hash4[2]);
        BB.put(hash4[3]);

        return org.bitcoinj.core.Base58.encode(BB.array());
    }

    public String convertToLegacyAddress(String cashAddr) throws NoSuchProviderException, NoSuchAlgorithmException {

        BitcoinCashAddressDecodedParts bcadp = CashAddr.decodeCashAddress(cashAddr);
        byte netSelectionByte = (byte) 0x00;
        byte hash2[] = bcadp.hash;

        ByteBuffer BB = ByteBuffer.allocate(hash2.length + 1);

        BB.put(netSelectionByte);
        BB.put(hash2);

        byte hash3[] = Util.calculateSHA256(BB.array());
        byte hash4[] = Util.calculateSHA256(hash3);

        BB = ByteBuffer.allocate(hash2.length + 5);
        BB.put(netSelectionByte);
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
    public InternalAmount convertToInternalAmount(Amount amount) throws Exception {
        BigDecimal d = amount.multiply(new BigDecimal("100000000"));
        return new InternalAmount(d, getBalanceCurrency());
    }

    @Override
    public InternalAmount convertToInternalAmount(byte[] bytes) {
        if (bytes == null) return null;
        byte[] reversed = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++) reversed[i] = bytes[bytes.length - i - 1];
        return new InternalAmount(Util.byteArrayToLong(reversed), "Satoshi");
    }

    @Override
    public byte[] convertToByteArray(InternalAmount internalAmount) throws Exception {
        byte[] bytes = Util.longToByteArray(internalAmount.longValueExact());
        byte[] reversed = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++) reversed[i] = bytes[bytes.length - i - 1];
        return reversed;
    }

    @Override
    public CoinData createCoinData() {
        return new BtcData();
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
    public void defineWallet() throws CardProtocol.TangemException {
        try {
            String wallet = calculateAddress(ctx.getCard().getWalletPublicKeyRar());
            ctx.getCoinData().setWallet(wallet);
        } catch (Exception e) {
            ctx.getCoinData().setWallet("ERROR");
            throw new CardProtocol.TangemException("Can't define wallet address");
        }

    }


    @Override
    public SignTask.TransactionToSign constructTransaction(Amount amountValue, Amount feeValue, boolean IncFee, String targetAddress) throws Exception {
        checkBlockchainDataExists();

        String srcLegacyAddress = convertToLegacyAddress(ctx.getCoinData().getWallet());
        String destLegacyAddress = convertToLegacyAddress(targetAddress);
        byte[] pbKey = ctx.getCard().getWalletPublicKeyRar(); //ALWAYS USING COMPRESS KEY

        final ArrayList<UnspentOutputInfo> unspentOutputs = new ArrayList<>();
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

        if (amount + fees > fullAmount) {
            throw new CardProtocol.TangemException_WrongAmount(String.format("Balance (%d) < change (%d) + amount (%d)", fullAmount, change, amount));
        }

        final long amountFinal = amount;
        final long changeFinal = change;

        byte[][] txForSign = new byte[unspentOutputs.size()][];
        byte[][] bodyHash = new byte[unspentOutputs.size()][];
        byte[][] bodyDoubleHash = new byte[unspentOutputs.size()][];

        for (int i = 0; i < unspentOutputs.size(); ++i) {
            txForSign[i] = BCHUtils.buildTXForSign(srcLegacyAddress, destLegacyAddress, srcLegacyAddress, unspentOutputs, i, amount, change);
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
                throw new Exception("Transaction validation by issuer not supported in this version!");
            }

            @Override
            public byte[] onSignCompleted(byte[] signFromCard) throws Exception {
                for (int i = 0; i < unspentOutputs.size(); ++i) {
                    BigInteger r = new BigInteger(1, Arrays.copyOfRange(signFromCard, i * 64, 32 + i * 64));
                    BigInteger s = new BigInteger(1, Arrays.copyOfRange(signFromCard, 32 + i * 64, 64 + i * 64));
                    s = CryptoUtil.toCanonicalised(s);

                    unspentOutputs.get(i).scriptForBuild = DerEncodingUtil.packSignDerBitcoinCash(r, s, pbKey);
                }

                byte[] txForSend = BCHUtils.buildTXForSend(destLegacyAddress, srcLegacyAddress, unspentOutputs, amountFinal, changeFinal);

                notifyOnNeedSendTransaction(txForSend);
                return txForSend;
            }
        };
    }

    @Override
    public void requestBalanceAndUnspentTransactions(BlockchainRequestsCallbacks blockchainRequestsCallbacks) throws Exception {
        final ServerApiBlockchair serverApiBlockchair = new ServerApiBlockchair(ctx.getBlockchain());

        SingleObserver<BlockchairAddressResponse> addressObserver = new DisposableSingleObserver<BlockchairAddressResponse>() {
            @Override
            public void onSuccess(BlockchairAddressResponse addressResponse) {
                try {
                    BlockchairAddressData addressData = addressResponse.getData().get(coinData.getWallet());

                    coinData.setBalanceConfirmed(addressData.getAddress().getBalance());
                    coinData.setBalanceUnconfirmed(0L);
                    coinData.setBalanceReceived(true);
                    coinData.setValidationNodeDescription(serverApiBlockchair.getUrl());
                    coinData.setSentTransactionsCount(addressData.getAddress().getOutputCount() - addressData.getAddress().getUnspentOutputCount());

                    for (BlockchairUnspentOutput utxo : addressData.getUnspentOutputs()) {
                        if (utxo.getBlock() != -1) {
                            BtcData.UnspentTransaction unspentTx = new BtcData.UnspentTransaction();
                            unspentTx.txID = utxo.getTransactionHash();
                            unspentTx.amount = utxo.getAmount();
                            unspentTx.outputN = utxo.getIndex();
                            coinData.getUnspentTransactions().add(unspentTx);
                        } else {
                            coinData.setHasUnconfirmed(true);
                        }
                    }

                    if (addressData.getAddress().getBalance() != 0) {
                        blockchainRequestsCallbacks.onComplete(true);
                    } else { //check if there is an unconfirmed tx, which spent all funds
                        if (addressData.getTransactions().isEmpty()) {
                            blockchainRequestsCallbacks.onComplete(true);
                        } else {
                            requestIsTransactionConfirmed(addressData.getTransactions().get(0), blockchainRequestsCallbacks);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e(TAG, "FAIL getAddress Exception");
                    ctx.setError(e.getMessage());
                    blockchainRequestsCallbacks.onComplete(false);
                }
            }

            @Override
            public void onError(Throwable e) {
                e.printStackTrace();
                Log.e(TAG, "FAIL getAddress Exception");
                ctx.setError(e.getMessage());
                blockchainRequestsCallbacks.onComplete(false);
            }
        };

        serverApiBlockchair.getAddress(coinData.getWallet(), addressObserver);
    }

    private void requestIsTransactionConfirmed(String transaction, BlockchainRequestsCallbacks blockchainRequestsCallbacks) {
        final ServerApiBlockchair serverApiBlockchair = new ServerApiBlockchair(ctx.getBlockchain());

        SingleObserver<BlockchairTransactionResponse> transactionObserver = new DisposableSingleObserver<BlockchairTransactionResponse>() {
            @Override
            public void onSuccess(BlockchairTransactionResponse transactionResponse) {
                if (transactionResponse.getData().get(transaction).getTransaction().getBlock() == -1) {
                    coinData.setHasUnconfirmed(true);
                }
                blockchainRequestsCallbacks.onComplete(true);
            }

            @Override
            public void onError(Throwable e) {
                e.printStackTrace();
                Log.e(TAG, "FAIL getTransaction Exception");
                ctx.setError(e.getMessage());
                blockchainRequestsCallbacks.onComplete(false);
            }
        };

        serverApiBlockchair.getTransaction(transaction, transactionObserver);
    }

    private Integer calculateEstimatedTransactionSize(String outputAddress, String outAmount) {
        try {
            SignTask.TransactionToSign ps = constructTransaction(new Amount(outAmount, getBalanceCurrency()), new Amount("0.00", getFeeCurrency()), true, outputAddress);
            OnNeedSendTransaction onNeedSendTransactionBackup = onNeedSendTransaction;
            onNeedSendTransaction = (tx) -> {
            }; // empty function to bypass exception

            byte[][] hashesToSign = ps.getHashesToSign();
            byte[] signFromCard = new byte[64 * hashesToSign.length];
            Arrays.fill(signFromCard, (byte) 0x01);
            byte[] txForSend = ps.onSignCompleted(signFromCard);
            onNeedSendTransaction = onNeedSendTransactionBackup;
            Log.e(TAG, "txForSend.length=" + String.valueOf(txForSend.length));
            return txForSend.length + 1;
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Can't calculate transaction size -> use default!");
            return 256;
        }
    }

    private final static BigDecimal relayFee = new BigDecimal(0.00001);

    @Override
    public void requestFee(BlockchainRequestsCallbacks blockchainRequestsCallbacks, String targetAddress, Amount amount) throws Exception {
        final int calcSize = calculateEstimatedTransactionSize(targetAddress, amount.toValueString());
        Log.e(TAG, String.format("Estimated tx size %d", calcSize));
        coinData.minFee = null;
        coinData.maxFee = null;
        coinData.normalFee = null;

        final ServerApiBlockchair serverApiBlockchair = new ServerApiBlockchair(ctx.getBlockchain());

        SingleObserver<BlockchairStatsResponse> statsObserver = new DisposableSingleObserver<BlockchairStatsResponse>() {
            @Override
            public void onSuccess(BlockchairStatsResponse blockchairStatsResponse) {
                try {
                    int feeSatoshi = blockchairStatsResponse.getData().getFeePerByte() * calcSize;
                    BigDecimal fee = BigDecimal.valueOf(feeSatoshi).movePointLeft(getDecimals());
                    if (fee.compareTo(relayFee) < 0) {
                        fee = relayFee;
                    }
                    Amount feeAmount = new Amount(fee.setScale(getDecimals(), RoundingMode.DOWN), getFeeCurrency());
                    coinData.minFee = feeAmount;
                    coinData.normalFee = feeAmount;
                    coinData.maxFee = feeAmount;
                    blockchainRequestsCallbacks.onComplete(true);
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e(TAG, "FAIL getStats Exception");
                    ctx.setError(e.getMessage());
                    blockchainRequestsCallbacks.onComplete(false);
                }
            }

            @Override
            public void onError(Throwable e) {
                e.printStackTrace();
                Log.e(TAG, "FAIL getStats Exception");
                ctx.setError(e.getMessage());
                blockchainRequestsCallbacks.onComplete(false);
            }
        };

        serverApiBlockchair.getStats(statsObserver);
    }

    @Override
    public void requestSendTransaction(BlockchainRequestsCallbacks blockchainRequestsCallbacks, byte[] txForSend) throws Exception {
        final ServerApiBlockchair serverApiBlockchair = new ServerApiBlockchair(ctx.getBlockchain());

        CompletableObserver sendObserver = new DisposableCompletableObserver() {
            @Override
            public void onComplete() {
                blockchainRequestsCallbacks.onComplete(true);
            }

            @Override
            public void onError(Throwable e) {
                ctx.setError(e.getMessage());
                blockchainRequestsCallbacks.onComplete(false);
            }
        };

        serverApiBlockchair.sendTransaction(Util.byteArrayToHexString(txForSend), sendObserver);
    }

    @Override
    public boolean allowSelectFeeLevel() {
        return false;
    }
}
