package com.example.pruebanfc;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.StringTokenizer;






import java.util.HashMap;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.paypal.android.MEP.PayPal;

import android.R.string;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.DocumentsContract.Document;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

 
/**
 * Activity for reading data from an NDEF Tag.
 * 
 * @author Ralf Wondratschek
 *
 */
public class MainActivity extends Activity {
 
    public static final String MIME_TEXT_PLAIN = "text/plain";
    public static final String TAG = "NfcDemo";
    public ArrayList productos;
    public ArrayAdapter adapter;
    TextView saldo;
    
    int pagar=0;
    //private String[] productos=new String[50];
    ListView lista;
    
   
    private NfcAdapter mNfcAdapter;
 
    @SuppressWarnings("unchecked")
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        lista=(ListView) findViewById(R.id.productos);
        saldo = (TextView)findViewById(R.id.texto_cuenta);
        saldo.setText("$ "+Integer.toString(pagar));
        productos=new ArrayList();
        productos.add("Vacio");
        		 
        		 
        adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, productos);
        		 
        		 lista.setAdapter(adapter);
 
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
 
        if (mNfcAdapter == null) {
            // Stop here, we definitely need NFC
            //Toast.makeText(this, "This device doesn't support NFC.", Toast.LENGTH_LONG).show();
            finish();
            return;
 
        }
     
        if (!mNfcAdapter.isEnabled()) {
            //mTextView.setText("NFC esta Apagado.");
        } else {
            //mTextView.setText("NFC esta Encendido");
        }
         
        handleIntent(getIntent());
    }
     
    @Override
    protected void onResume() {
        super.onResume();
         
        /**
         * It's important, that the activity is in the foreground (resumed). Otherwise
         * an IllegalStateException is thrown. 
         */
        setupForegroundDispatch(this, mNfcAdapter);
    }
     
    @Override
    protected void onPause() {
        /**
         * Call this before onPause, otherwise an IllegalArgumentException is thrown as well.
         */
        stopForegroundDispatch(this, mNfcAdapter);
         
        super.onPause();
    }
     
    @Override
    protected void onNewIntent(Intent intent) { 
        /**
         * This method gets called, when a new Intent gets associated with the current activity instance.
         * Instead of creating a new activity, onNewIntent will be called. For more information have a look
         * at the documentation.
         * 
         * In our case this method gets called, when the user attaches a Tag to the device.
         */
        handleIntent(intent);
    }
     
    private void handleIntent(Intent intent) {
        String action = intent.getAction();
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
             
            String type = intent.getType();
            if (MIME_TEXT_PLAIN.equals(type)) {
     
                Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
                new NdefReaderTask().execute(tag);
                 
            } else {
                Log.d(TAG, "Wrong mime type: " + type);
            }
        } else if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)) {
             
            // In case we would still use the Tech Discovered Intent
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            String[] techList = tag.getTechList();
            String searchedTech = Ndef.class.getName();
             
            for (String tech : techList) {
                if (searchedTech.equals(tech)) {
                    new NdefReaderTask().execute(tag);
                    break;
                }
            }
        }
    }
     
    /**
     * @param activity The corresponding {@link Activity} requesting the foreground dispatch.
     * @param adapter The {@link NfcAdapter} used for the foreground dispatch.
     */
    public static void setupForegroundDispatch(final Activity activity, NfcAdapter adapter) {
        final Intent intent = new Intent(activity.getApplicationContext(), activity.getClass());
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
 
        final PendingIntent pendingIntent = PendingIntent.getActivity(activity.getApplicationContext(), 0, intent, 0);
 
        IntentFilter[] filters = new IntentFilter[1];
        String[][] techList = new String[][]{};
 
        // Notice that this is the same filter as in our manifest.
        filters[0] = new IntentFilter();
        filters[0].addAction(NfcAdapter.ACTION_NDEF_DISCOVERED);
        filters[0].addCategory(Intent.CATEGORY_DEFAULT);
        try {
            filters[0].addDataType(MIME_TEXT_PLAIN);
        } catch (MalformedMimeTypeException e) {
            throw new RuntimeException("Check your mime type.");
        }
         
        adapter.enableForegroundDispatch(activity, pendingIntent, filters, techList);
    }
 
    /**
     * @param activity The corresponding {@link BaseActivity} requesting to stop the foreground dispatch.
     * @param adapter The {@link NfcAdapter} used for the foreground dispatch.
     */
    public static void stopForegroundDispatch(final Activity activity, NfcAdapter adapter) {
        adapter.disableForegroundDispatch(activity);
    }
    
    public String readText(NdefRecord record) throws UnsupportedEncodingException {
        /*
         * See NFC forum specification for "Text Record Type Definition" at 3.2.1 
         * 
         * http://www.nfc-forum.org/specs/
         * 
         * bit_7 defines encoding
         * bit_6 reserved for future use, must be 0
         * bit_5..0 length of IANA language code
         */
 
        byte[] payload = record.getPayload();
 
        // Get the Text Encoding
        String textEncoding = ((payload[0] & 128) == 0) ? "UTF-8" : "UTF-16";
 
        // Get the Language Code
        int languageCodeLength = payload[0] & 0063;
         
        // String languageCode = new String(payload, 1, languageCodeLength, "US-ASCII");
        // e.g. "en"
         
        // Get the Text
        return new String(payload, languageCodeLength + 1, payload.length - languageCodeLength - 1, textEncoding);
    }
    
    private class NdefReaderTask extends AsyncTask<Tag, Void, String>{
    	
    	//public static final String TAG = "NfcDemo";
    	//private TextView mTextView;
    	String articulo = "";
		int precio = 0;

    	@Override
    	protected String doInBackground(Tag... params) {
    		// TODO Auto-generated method stub
    		Tag tag = params[0];
            
            Ndef ndef = Ndef.get(tag);
            if (ndef == null) {
                // NDEF is not supported by this Tag. 
                return null;
            }
     
            NdefMessage ndefMessage = ndef.getCachedNdefMessage();
     
            NdefRecord[] records = ndefMessage.getRecords();
            for (NdefRecord ndefRecord : records) {
                if (ndefRecord.getTnf() == NdefRecord.TNF_WELL_KNOWN && Arrays.equals(ndefRecord.getType(), NdefRecord.RTD_TEXT)) {
                    try {
                        return readText(ndefRecord);
                    } catch (UnsupportedEncodingException e) {
                        Log.e(TAG, "Unsupported Encoding", e);
                    }
                }
            }
     
            return null;
    	}

    	
    	@Override
    	protected void onPostExecute(String result) {
    		
    		
    		System.out.print("1");
            if (result != null) {
            	System.out.print(result);
            	Toast.makeText(getApplicationContext(), 
                        result, Toast.LENGTH_LONG).show();
            	//productos.add(result);
            	//adapter.notifyDataSetChanged();
            	
            	separar(result);
//            	
//            	Toast.makeText(getApplicationContext(), 
//                        articulo, Toast.LENGTH_LONG).show();
//            	Toast.makeText(getApplicationContext(), 
//                        precio, Toast.LENGTH_LONG).show();
            	
//            	DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
//            	DocumentBuilder db;
//				try {
//					db = dbf.newDocumentBuilder();
//				} catch (ParserConfigurationException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//            	ByteArrayInputStream bis = new ByteArrayInputStream(result.getBytes());
//            	org.w3c.dom.Document doc;
//				try {
//					doc = db.parse(bis);
//				} catch (SAXException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				} catch (IOException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//            	Node n = doc.getFirstChild();
//            	System.out.print(innerXml(n));
//            }
//            
//  
//            
//    	}
//    	
//    	public String innerXml(Node node) {
//    	    DOMImplementationLS lsImpl = (DOMImplementationLS)node.getOwnerDocument().getImplementation().getFeature("LS", "3.0");
//    	    LSSerializer lsSerializer = lsImpl.createLSSerializer();
//    	    NodeList childNodes = node.getChildNodes();
//    	    StringBuilder sb = new StringBuilder();
//    	    for (int i = 0; i < childNodes.getLength(); i++) {
//    	       sb.append(lsSerializer.writeToString(childNodes.item(i)));
//    	    }
//    	    return sb.toString(); 
}
    	
    	}


		public void separar(String result) {
			StringTokenizer tokens = new StringTokenizer(result,".");

			articulo=tokens.nextToken();
			precio=Integer.parseInt(tokens.nextToken());
			pagar+=precio;
			productos.add(articulo);
        	adapter.notifyDataSetChanged();
			cambiarSaldo();
		}
         
    	}
    public void cambiarSaldo(){
    	saldo.setText("$ "+Integer.toString(pagar));
    	
    }
    
    public void initLibrary(){
    	
    	PayPal pp=PayPal.getInstance();
    	
    	if(pp==null){
    		
    		pp=PayPal.initWithAppID(this,"APP-80W284485P519543T",PayPal.ENV_NONE);
    		pp.setLanguage("en_US");
    		pp.setFeesPayer(PayPal.FEEPAYER_EACHRECEIVER);
    		pp.setShippingEnabled(false);
    		//_paypalLibraryInit = true;
    	
    	}
    	
    	
    }
    
    
    
    

    }
        



