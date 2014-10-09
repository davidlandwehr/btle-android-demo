package gn.blebug;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Toast;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.UUID;

public class DiscoveryResult extends Fragment {
    private static final String ARGUMENT_DEVICE = "device";
    private static final UUID SERVICE_UUID = UUID.fromString("8436054B-047E-494B-9EC0-E6B9E2ADF8EE");
    private static final UUID NAME_UUID = UUID.fromString("2C1CF75D-751C-4ED8-A478-884BDCDCBD75");
    private static final UUID VOLUME_UUID = UUID.fromString("D5BB9776-B58F-44D4-AF28-13DE24BE71ED");
    private static final UUID CC_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    private BluetoothDevice device;

    private ProgressBar progress;
    private EditText name;
    private SeekBar volume;
    private ArrayList<Runnable> operations = new ArrayList<Runnable>();
    private BluetoothGatt gatt;
    private boolean hackUpdatingText = false;
    public static DiscoveryResult newInstance(BluetoothDevice device) {
        DiscoveryResult fragment = new DiscoveryResult();
        Bundle args = new Bundle();
        args.putParcelable(ARGUMENT_DEVICE, device);
        fragment.setArguments(args);
        return fragment;
    }

    public DiscoveryResult() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            device = getArguments().getParcelable(ARGUMENT_DEVICE);
        }
    }

    @Override
    public void onStop() {
        if (gatt!=null)
            gatt.disconnect();
        operations.clear();
        super.onStop();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_discovery_result, container, false);
        progress = (ProgressBar)v.findViewById(R.id.progress_bar);
        name = (EditText)v.findViewById(R.id.name);
        volume = (SeekBar)v.findViewById(R.id.volume);

        name.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!hackUpdatingText)
                    pushQueue(new WriteName());
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        volume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser)
                    pushQueue(new WriteVolume());
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        return v;
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }


    @Override
    public void onStart() {
        super.onStart();
        gatt = device.connectGatt(getActivity(), false, gattCallback);

    }

    private boolean refreshDeviceCache(BluetoothGatt gatt){
        try {
            BluetoothGatt localBluetoothGatt = gatt;
            Method localMethod = localBluetoothGatt.getClass().getMethod("refresh", new Class[0]);
            if (localMethod != null) {
                boolean bool = ((Boolean) localMethod.invoke(localBluetoothGatt, new Object[0])).booleanValue();
                return bool;
            }
        }
        catch (Exception localException) {
            Log.e("GATT", "An exception occurred while refreshing device");
        }
        return false;
    }

    BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(final BluetoothGatt gatt, int status, int newState) {
            if (newState==BluetoothGatt.STATE_CONNECTED) {
                progress.post(new Runnable() {
                    @Override
                    public void run() {
                        refreshDeviceCache(gatt);
                        progress.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                gatt.discoverServices();
                            }
                        }, 2000);

                    }
                });
            } else if (newState==BluetoothGatt.STATE_DISCONNECTED) {
                progress.post(new Runnable() {
                    @Override
                    public void run() {
                        gatt.close();
                        Log.d("GATT", "Closed");
                    }
                });
            }
        }

        @Override
        public void onServicesDiscovered(final BluetoothGatt gatt, final int status) {
            progress.post(new Runnable() {
                @Override
                public void run() {
                    if (status==BluetoothGatt.GATT_SUCCESS) {

                        pushQueue(new WriteCCDescriptor(NAME_UUID));
                        pushQueue(new WriteCCDescriptor(VOLUME_UUID));
                        pushQueue(new ReadName());
                        pushQueue(new ReadVolume());
                        pushQueue(new RemoveSpinner());
                    } else {
                        Toast.makeText(getActivity(), String.format("Failed discovering service. Error %d", status), Toast.LENGTH_LONG).show();
                    }
                }
            });
        }

        @Override
        public void onCharacteristicRead(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, final int status) {
            pushQueue(new HandleCharacteristics(characteristic));
            popQueue();
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            popQueue();
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            popQueue();
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            pushQueue(new HandleCharacteristics(characteristic));
        }
    };

    public synchronized void pushQueue(Runnable run) {
        operations.add(run);
        if (operations.size()==1)
            name.post(operations.get(0));
    }

    public synchronized void popQueue() {
        operations.remove(0);
        if (!operations.isEmpty())
            name.post(operations.get(0));
    }

    class HandleCharacteristics implements Runnable {
        BluetoothGattCharacteristic characteristic;

        public HandleCharacteristics(BluetoothGattCharacteristic characteristic){
            this.characteristic = characteristic;
        }

        public void run() {
            if (characteristic.getUuid().equals(NAME_UUID)) {
                hackUpdatingText = true;
                name.setText(new String(characteristic.getValue()));
                hackUpdatingText = false;
            } else if (characteristic.getUuid().equals(VOLUME_UUID)) {
                int volumeValue = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT32, 0);
                volume.setProgress(volumeValue);
            }
            popQueue();
        }

    }

    class ReadName implements Runnable {
        public void run() {
            gatt.readCharacteristic(gatt.getService(SERVICE_UUID).getCharacteristic(NAME_UUID));
        }
    }

    class ReadVolume implements Runnable {
        public void run() {
            gatt.readCharacteristic(gatt.getService(SERVICE_UUID).getCharacteristic(VOLUME_UUID));
        }
    }

    class WriteVolume implements Runnable {
        public void run() {
            BluetoothGattCharacteristic characteristic = gatt.getService(SERVICE_UUID).getCharacteristic(VOLUME_UUID);
            characteristic.setValue(volume.getProgress(), BluetoothGattCharacteristic.FORMAT_SINT32, 0);
            gatt.writeCharacteristic(characteristic);
        }
    }

    class WriteName implements Runnable {
        public void run() {
            BluetoothGattCharacteristic characteristic = gatt.getService(SERVICE_UUID).getCharacteristic(NAME_UUID);
            characteristic.setValue(name.getText().toString().getBytes());
            gatt.writeCharacteristic(characteristic);
        }
    }

    class WriteCCDescriptor implements Runnable {
        UUID characteristicsUUID;
        public WriteCCDescriptor(UUID characteristicsUUID) {
            this.characteristicsUUID = characteristicsUUID;
        }

        public void run() {
            BluetoothGattCharacteristic name = gatt.getService(SERVICE_UUID).getCharacteristic(this.characteristicsUUID);
            gatt.setCharacteristicNotification(name, true);
            BluetoothGattDescriptor desc = name.getDescriptor(CC_UUID);
            desc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            gatt.writeDescriptor(desc);
        }
    }

    class RemoveSpinner implements Runnable {
        public void run() {
            progress.setVisibility(View.INVISIBLE);
            popQueue();
        }
    }

}
