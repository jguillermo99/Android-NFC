package com.example.pruebanfc;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.java.Producto;
import com.java.XMLClass;
import com.paypal.android.MEP.PayPal;

/**
 * Activity for reading data from an NDEF Tag.
 * 
 * @author Juan Guillermo
 * 
 */
public class MainActivity extends Activity {

	public static final String MIME_TEXT_PLAIN = "text/plain";
	public static final String TAG = "NfcDemo";
	public ItemListAdapter adapter;
	private ArrayList<Producto> products;
	private TextView saldo;
	private int pagar = 0;
	private ListView lista;
	private NfcAdapter mNfcAdapter;
	private Button boton;

	@SuppressWarnings("unchecked")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// Inicializar Vista
		this.saldo = (TextView) findViewById(R.id.texto_cuenta);
		this.saldo.setText("$ " + Integer.toString(pagar));

		products = new ArrayList<Producto>();
		Producto p1 = new Producto("telefono", 50000, "www.123.com");
		p1.setCantidad(20);
		products.add(p1);

		Producto p2 = new Producto("Camara Digital", 350000, "www.123.com");
		p2.setCantidad(5);
		products.add(p2);

		Producto p3 = new Producto("Portatil Toshiba", 1800000, "www.123.com");
		p3.setCantidad(2);
		products.add(p3);

		Producto p4 = new Producto("Audifonos Bluetooth", 120000, "www.123.com");
		p4.setCantidad(3);
		products.add(p4);

		Producto p5 = new Producto("Tablet Lenovo", 450000, "www.123.com");
		p5.setCantidad(4);
		products.add(p5);

		int num = 0;
		for (int i = 0; i < products.size(); i++) {
			num = num
					+ (products.get(i).getPrecio() * products.get(i)
							.getCantidad());
		}
		cambiarSaldo(num);

		adapter = new ItemListAdapter(this, products);

		boton = (Button) findViewById(R.id.boton_pagar);
		boton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {

				Toast.makeText(getApplicationContext(), "A Pagar",
						Toast.LENGTH_LONG).show();
				// TODO Auto-generated method stub

			}
		});

		lista = (ListView) findViewById(R.id.productos);

		lista.setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> arg0, View view,
					int pos, long arg3) {
				products.get(pos).restarCantidad();
				products.get(pos)
						.sumarTotal(products.get(pos).getPrecio() * -1);
				cambiarSaldo(products.get(pos).getPrecio() * -1);

				if (products.get(pos).getCantidad() == 0)
					products.remove(pos);

				adapter.notifyDataSetChanged();
				// TODO Auto-generated method stub
				return true;
			}

		});

		lista.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View view, int pos,
					long arg3) {
				Intent intent = null;
				intent = new Intent(intent.ACTION_VIEW, Uri.parse(products.get(
						pos).getUrl()));
				startActivity(intent);
			}
		});

		lista.setAdapter(adapter);
		adapter.notifyDataSetChanged();

		mNfcAdapter = NfcAdapter.getDefaultAdapter(this);

		if (mNfcAdapter == null) {
			// Stop here, we definitely need NFC
			// Toast.makeText(this, "This device doesn't support NFC.",
			// Toast.LENGTH_LONG).show();
			finish();
			return;

		}

		if (!mNfcAdapter.isEnabled()) {
			// mTextView.setText("NFC esta Apagado.");
		} else {
			// mTextView.setText("NFC esta Encendido");
		}

		handleIntent(getIntent());
	}

	@Override
	protected void onResume() {
		super.onResume();

		/**
		 * It's important, that the activity is in the foreground (resumed).
		 * Otherwise an IllegalStateException is thrown.
		 */
		setupForegroundDispatch(this, mNfcAdapter);
	}

	@Override
	protected void onPause() {
		/**
		 * Call this before onPause, otherwise an IllegalArgumentException is
		 * thrown as well.
		 */
		stopForegroundDispatch(this, mNfcAdapter);

		super.onPause();
	}

	@Override
	protected void onNewIntent(Intent intent) {
		/**
		 * This method gets called, when a new Intent gets associated with the
		 * current activity instance. Instead of creating a new activity,
		 * onNewIntent will be called. For more information have a look at the
		 * documentation.
		 * 
		 * In our case this method gets called, when the user attaches a Tag to
		 * the device.
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
	 * @param activity
	 *            The corresponding {@link Activity} requesting the foreground
	 *            dispatch.
	 * @param adapter
	 *            The {@link NfcAdapter} used for the foreground dispatch.
	 */
	public static void setupForegroundDispatch(final Activity activity,
			NfcAdapter adapter) {
		final Intent intent = new Intent(activity.getApplicationContext(),
				activity.getClass());
		intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

		final PendingIntent pendingIntent = PendingIntent.getActivity(
				activity.getApplicationContext(), 0, intent, 0);

		IntentFilter[] filters = new IntentFilter[1];
		String[][] techList = new String[][] {};

		// Notice that this is the same filter as in our manifest.
		filters[0] = new IntentFilter();
		filters[0].addAction(NfcAdapter.ACTION_NDEF_DISCOVERED);
		filters[0].addCategory(Intent.CATEGORY_DEFAULT);
		try {
			filters[0].addDataType(MIME_TEXT_PLAIN);
		} catch (MalformedMimeTypeException e) {
			throw new RuntimeException("Check your mime type.");
		}

		adapter.enableForegroundDispatch(activity, pendingIntent, filters,
				techList);
	}

	/**
	 * @param activity
	 *            The corresponding {@link BaseActivity} requesting to stop the
	 *            foreground dispatch.
	 * @param adapter
	 *            The {@link NfcAdapter} used for the foreground dispatch.
	 */
	public static void stopForegroundDispatch(final Activity activity,
			NfcAdapter adapter) {
		adapter.disableForegroundDispatch(activity);
	}

	public String readText(NdefRecord record)
			throws UnsupportedEncodingException {
		/*
		 * See NFC forum specification for "Text Record Type Definition" at
		 * 3.2.1
		 * 
		 * http://www.nfc-forum.org/specs/
		 * 
		 * bit_7 defines encoding bit_6 reserved for future use, must be 0
		 * bit_5..0 length of IANA language code
		 */

		byte[] payload = record.getPayload();

		// Get the Text Encoding
		String textEncoding = ((payload[0] & 128) == 0) ? "UTF-8" : "UTF-16";

		// Get the Language Code
		int languageCodeLength = payload[0] & 0063;

		// String languageCode = new String(payload, 1, languageCodeLength,
		// "US-ASCII");
		// e.g. "en"

		// Get the Text
		return new String(payload, languageCodeLength + 1, payload.length
				- languageCodeLength - 1, textEncoding);
	}

	private class NdefReaderTask extends AsyncTask<Tag, Void, String> {

		// public static final String TAG = "NfcDemo";
		// private TextView mTextView;

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
				if (ndefRecord.getTnf() == NdefRecord.TNF_WELL_KNOWN
						&& Arrays.equals(ndefRecord.getType(),
								NdefRecord.RTD_TEXT)) {
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
			System.out.println(result);

			if (result != null) {
				Toast.makeText(getApplicationContext(), result,
						Toast.LENGTH_LONG).show();

				String resul = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>"
						+ "<etiqueta>"
						+ "<nombre>Televisor Led Sony.</nombre>"
						+ "<precio>900000</precio>"
						+ "<url>http://www.sony.com.co/electronics/televisores/w950b-series</url>"
						+ "</etiqueta>";

				XMLClass xml = new XMLClass(resul);

				Producto prod = xml.buildObject();
				prod.printAtributes();

				if (validateProduct(prod))
					prod.sumarCantidad();

				else {
					products.add(prod);
					// productos.add(prod.getNombre());
					adapter.notifyDataSetChanged();
				}
				cambiarSaldo(prod.getPrecio());

			} else
				Toast.makeText(getApplicationContext(), "Etiqueta No Válida",
						Toast.LENGTH_LONG).show();

		}

		public boolean validateProduct(Producto producto) {
			// Si el producto ya existe retorna verdadero
			for (int i = 0; i < products.size(); i++) {
				if (producto.getNombre().equalsIgnoreCase(
						products.get(i).getNombre())) {
					products.get(i).sumarCantidad();
					products.get(i).sumarTotal(products.get(i).getPrecio());
					adapter.notifyDataSetChanged();
					return true;

				}
			}

			return false;
		}

	}

	public void cambiarSaldo(int pag) {
		pagar += pag;
		saldo.setText("$ " + Integer.toString(pagar));

	}

	public void initLibrary() {

		PayPal pp = PayPal.getInstance();

		if (pp == null) {

			pp = PayPal.initWithAppID(this, "APP-80W284485P519543T",
					PayPal.ENV_NONE);
			pp.setLanguage("en_US");
			pp.setFeesPayer(PayPal.FEEPAYER_EACHRECEIVER);
			pp.setShippingEnabled(false);
			// _paypalLibraryInit = true;

		}

	}

}
