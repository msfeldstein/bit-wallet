package com.mijoro.bitcoin.btcwallet;

import java.io.File;
import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.AddressFormatException;
import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.InsufficientMoneyException;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.Utils;
import com.google.bitcoin.core.Wallet;
import com.google.bitcoin.core.Wallet.SendResult;
import com.google.bitcoin.core.WalletEventListener;
import com.google.bitcoin.core.Wallet.BalanceType;
import com.google.bitcoin.kits.WalletAppKit;
import com.google.bitcoin.params.TestNet3Params;
import com.google.bitcoin.script.Script;
import com.google.bitcoin.utils.Threading;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends Activity implements WalletEventListener {
	private Handler mHandler;
	private EditText mLogger;
	private TextView mBalanceField, mVerifiedBalanceField;
	private NetworkParameters mNetworkParams;
	private WalletAppKit mKit;
	

    /**
     * Called when the activity is first created.
     * @param savedInstanceState If the activity is being re-initialized after 
     * previously being shut down then this Bundle contains the data it most 
     * recently supplied in onSaveInstanceState(Bundle). <b>Note: Otherwise it is null.</b>
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mLogger = (EditText)findViewById(R.id.logger);
        mBalanceField = (TextView)findViewById(R.id.balance_field);
        mVerifiedBalanceField = (TextView)findViewById(R.id.verified_balance_field);
        mNetworkParams = TestNet3Params.get();
        String filePrefix = "forwarding-service-testnet";
        final File f = new File(getDir("blockstore", Context.MODE_PRIVATE), "blockchain-" + filePrefix);
        initializeThreads();
        
        mKit = new WalletAppKit(mNetworkParams, f, filePrefix) {
        	@Override
        	protected void onSetupCompleted() {
        		// File walletFile = new File(f, filePrefix + ".wallet");
        		ECKey key = new ECKey();
        		List<ECKey> keys = wallet().getKeys();
        		if (keys.size() < 1) {
        			System.out.println("KEY CREATED - " + key.toAddress(params));
        			wallet().addKey(key);
        		}
        		logBG("First key is " + keys.get(0).toAddress(params));
        		wallet().addEventListener(MainActivity.this);
        	}
        };
        mKit.start();
        findViewById(R.id.return_to_faucet).setOnClickListener(new OnClickListener() {
			public void onClick(View arg0) {
				EditText amountField = (EditText)findViewById(R.id.faucet_amount);
				sendBG("mn6A139WczczyDmwT3MVFQwcLzXdQK6EfM", amountField.getText().toString());
			}
		});
    }
    
    private void sendBG(String address, String amount) {
    	new SendCoinTask(address, amount).execute();
    }
    
    private class SendCoinTask extends AsyncTask<Void, Integer, Long> {
    	private String mAddress;
    	private String mAmount;
    	
    	public SendCoinTask(String address, String amount) {
    		mAddress = address;
    		mAmount = amount;
    	}

		@Override
		protected Long doInBackground(Void... params) {
			send(mAddress, mAmount);
			return null;
		}
		
		protected void onPostExecute(Long result) {
			log("Executed Spend");
		}
    	
    }
    
    private void send(String address, String amount) {
    	logBG("Sending " + amount + " to " + address + " Nano: "  + Utils.toNanoCoins(amount));
    	try {
			Address target = new Address(mNetworkParams, address);
			SendResult result = mKit.wallet().sendCoins(mKit.peerGroup(), target, Utils.toNanoCoins(amount));
			result.broadcastComplete.get();
		} catch (AddressFormatException e) {
			e.printStackTrace();
			logBG("Address Format Exception: " + address);
		} catch (InsufficientMoneyException e) {
			e.printStackTrace();
			logBG("Insufficient Money");
		} catch (InterruptedException e) {
			e.printStackTrace();
			logBG("Interrupted Exception" + e);
		} catch (ExecutionException e) {
			e.printStackTrace();
			logBG("ExecutionException " + e);
		}
    	
    }
    
    private void initializeThreads() {
    	mHandler = new Handler();
    	Threading.USER_THREAD = new Executor() {
			public void execute(Runnable command) {
				mHandler.post(command);
			}
		};
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
	// Inflate the menu; this adds items to the action bar if it is present.
	getMenuInflater().inflate(com.mijoro.bitcoin.btcwallet.R.menu.main, menu);
	return true;
    }
    
    private void logBG(final String msg) {
    	mHandler.post(new Runnable() {
			public void run() {
				log(msg);
			}
		});
    }
    
    private void log(String msg) {
    	System.out.println("BTCLog: " + msg);
    	mLogger.append(msg + "\n");
    }

	public void onCoinsReceived(Wallet wallet, Transaction tx,
			BigInteger prevBalance, BigInteger newBalance) {
		BigInteger amt = newBalance.subtract(prevBalance);
		log("onCoinsReceived: Amt: "+amt +" Prev:"+prevBalance + " new:" + newBalance);
	}

	public void onCoinsSent(Wallet wallet, Transaction tx,
			BigInteger prevBalance, BigInteger newBalance) {
		BigInteger amt = newBalance.subtract(prevBalance);
		log("onCoinsSent: Amt: " + amt + " Prev:"+prevBalance + " new:" + newBalance);
	}

	public void onReorganize(Wallet wallet) {
		log("Reorganize" + wallet);
	}

	public void onTransactionConfidenceChanged(Wallet wallet, Transaction tx) {
	}
	
	private String formatBTC(BigInteger amount) {
		return BTCUtils.formatValue(amount, 4, 0);
	}

	public void onWalletChanged(Wallet wallet) {
		mBalanceField.setText(formatBTC(wallet.getBalance(BalanceType.ESTIMATED)));
		mVerifiedBalanceField.setText(formatBTC(wallet.getBalance(BalanceType.ESTIMATED)));
	}

	public void onKeysAdded(Wallet wallet, List<ECKey> keys) {
		for (ECKey key : keys) {
			log("keyAdded" + key.toAddress(mNetworkParams));
		}
		
	}

	public void onScriptsAdded(Wallet wallet, List<Script> scripts) {
		// TODO Auto-generated method stub
		
	}

}

