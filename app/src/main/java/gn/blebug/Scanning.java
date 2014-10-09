package gn.blebug;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ListView;


public class Scanning extends Activity {
    private static final long SCANNING_TIME = 60000;

    private boolean isScanning;
    private BluetoothAdapter adapter;
    private BLEScanning scanCallback;
    private ListView scanItems;
    private ScanListAdapter scanListAdapter;
    private StopScanning stopScanning;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        setContentView(R.layout.activity_scanning);

        scanListAdapter = new ScanListAdapter(getLayoutInflater());

        scanItems = (ListView)findViewById(R.id.scan_items);
        scanItems.setAdapter(scanListAdapter);
        scanItems.setOnItemClickListener(new ScanItemClicked());

        getActionBar().setTitle(R.string.title_scanning);

        scanCallback = new BLEScanning();
        stopScanning = new StopScanning();
        adapter = ((BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();
    }

    @Override
    protected void onStart() {
        super.onStart();
        startScanning();
    }

    public void setScanning(boolean scanning) {
        this.isScanning = scanning;
        setProgressBarIndeterminateVisibility(isScanning);
        invalidateOptionsMenu();
    }

    public void startScanning() {
        if (!isScanning) {
            setScanning(true);
            scanListAdapter.clearAllItems();
            adapter.startLeScan(scanCallback);
            scanItems.postDelayed(stopScanning, SCANNING_TIME);
        }
    }

    public void stopScanning() {
        if (isScanning) {
            setScanning(false);
            adapter.stopLeScan(scanCallback);
            scanItems.removeCallbacks(stopScanning);
        }
    }

    public void toggleScanning() {
        if (isScanning) {
            stopScanning();
        } else {
            startScanning();
        }
    }

    @Override
    protected void onStop() {
        stopScanning();
        super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.scanning, menu);
        menu.getItem(0).setTitle(isScanning ? R.string.stop : R.string.scan);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_refresh) {
            toggleScanning();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    class BLEScanning implements BluetoothAdapter.LeScanCallback {
        @Override
        public void onLeScan(final BluetoothDevice bluetoothDevice, final int rssi, final byte[] bytes) {
            scanItems.post(new Runnable() {
                @Override
                public void run() {
                    scanListAdapter.updateItem(bluetoothDevice, bluetoothDevice.getName(), rssi);
                }
            });
        }
    }

    class ScanItemClicked implements ListView.OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            stopScanning();
            DiscoveryResult discoverResult = DiscoveryResult.newInstance(scanListAdapter.getDevice(i));
            getFragmentManager()
                    .beginTransaction()
                    .add(R.id.scan_container, discoverResult)
                    .addToBackStack(null)
                    .commit();
        }
    }

    class StopScanning implements Runnable {
        public void run() {
            stopScanning();
        }
    }
}
