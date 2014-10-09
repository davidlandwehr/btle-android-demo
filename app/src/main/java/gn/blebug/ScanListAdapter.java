package gn.blebug;

import android.bluetooth.BluetoothDevice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

/** An adapter for showing scan results
 */
public class ScanListAdapter extends BaseAdapter {

    /** Inflater to inflate the list views  */
    private LayoutInflater inflater;

    /** List of items */
    private ArrayList<ScanListItem> items = new ArrayList<ScanListItem>();


    public ScanListAdapter(LayoutInflater inflater) {
        this.inflater = inflater;
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public Object getItem(int i) {
        return items.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    public BluetoothDevice getDevice(int i) {
        return ((ScanListItem)getItem(i)).device;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {

        if (view==null) {
            view = inflater.inflate(R.layout.listitem_scanning, null);
            view.setTag(new ScanListItemAccessor(view));
        }

        ScanListItemAccessor accessor = (ScanListItemAccessor)view.getTag();

        accessor.populate((ScanListItem)getItem(i));

        return view;
    }

    /** Updates or insert an bluetooth device
     *
     * @param device the bluetooth device
     * @param rssi the rssi
     */
    public void updateItem(BluetoothDevice device, String name, int rssi) {
        for (ScanListItem item: items) {
            if (item.device.getAddress().equals(device.getAddress())) {
                item.name = name;
                item.rssi = rssi;
                notifyDataSetChanged();
                return;
            }
        }
        ScanListItem item = new ScanListItem();
        item.device = device;
        item.name = name;
        item.rssi = rssi;
        items.add(item);
        notifyDataSetInvalidated();
    }

    /** Clears all the items
     */
    public void clearAllItems() {
        items.clear();
        notifyDataSetInvalidated();
    }

    /** Wraps the population of data for items
     */
    static class ScanListItemAccessor {

        private TextView title;
        private TextView rssi;
        private TextView address;


        ScanListItemAccessor(View root) {
            title = (TextView)root.findViewById(R.id.scan_item_title);
            address = (TextView)root.findViewById(R.id.scan_item_address);
            rssi = (TextView)root.findViewById(R.id.scan_item_rssi);
        }

        void populate(ScanListItem scanListItem) {
            title.setText(scanListItem.name);
            address.setText(scanListItem.device.getAddress());
            rssi.setText(String.format("%ddB", scanListItem.rssi));
        }

    }

    /** Holder of the bluetooth information
     */
    static class ScanListItem {
        private BluetoothDevice device;
        private String name;
        private int rssi;


        @Override
        public int hashCode() {
            return device.getAddress().hashCode();
        }

        @Override
        public boolean equals(Object o) {
            return o!=null && ((ScanListItem)o).device.equals(device);
        }
    }
}
