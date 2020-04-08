package ozelentok.zbackup;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class BackupListAdapter extends BaseAdapter {
    private final ArrayList<BackupItem> backupItems;
    private final MainActivity activity;
    private final AdapterView.OnItemClickListener itemClickListener;
    private final AdapterView.OnItemLongClickListener itemLongClickListener;

    private static class ItemViewHolder {
        TextView localPathView;
        TextView lastBackupView;
        CheckBox isSelectedCheckBox;

        public ItemViewHolder(View view, BackupItem item) {
            localPathView = view.findViewById(R.id.local_path_view);
            lastBackupView = view.findViewById(R.id.last_backup_view);
            isSelectedCheckBox = view.findViewById(R.id.backup_check_box);
            initializeViews(item);
        }

        public void initializeViews(final BackupItem item)  {
            localPathView.setText(item.getLocalPath());
            long lastBackupTime = Math.max(
                    item.getLastFullBackupTime().getTime(),
                    item.getLastSelectedBackupTime().getTime());
            if (lastBackupTime > 0) {
                lastBackupView.setText(dateFormat.format(new Date(lastBackupTime)));
            } else {
                lastBackupView.setText("");
            }
            isSelectedCheckBox.setOnCheckedChangeListener(null);
            isSelectedCheckBox.setChecked(item.isSelected());
            isSelectedCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> item.setSelected(isChecked));
        }
    }

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd EEE HH:mm:ss", Locale.US);

    public BackupListAdapter(ArrayList<BackupItem> backupItems, MainActivity activity) {
        this.backupItems = backupItems;
        this.activity = activity;
        this.itemClickListener = new BackupItemClickListener(activity);
        this.itemLongClickListener = new BackupItemLongClickListener(activity);
    }

    public AdapterView.OnItemClickListener getItemClickListener() {
        return itemClickListener;
    }

    public AdapterView.OnItemLongClickListener getItemLongClickListener() {
        return itemLongClickListener;
    }

    @Override
    public int getCount() {
        return this.backupItems.size();
    }
    @Override
    public Object getItem(int position) {
        return backupItems.get(position);
    }
    @Override
    public long getItemId(int position) {
        return 0;
    }
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        BackupItem item = backupItems.get(position);
        ItemViewHolder viewHolder;
        if (convertView != null) {
            viewHolder = (ItemViewHolder) convertView.getTag();
            viewHolder.initializeViews(item);
            return convertView;
        }
        LayoutInflater inflater = (LayoutInflater)activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        convertView = inflater.inflate(R.layout.backup_item_layout, null);
        viewHolder = new ItemViewHolder(convertView, item);
        convertView.setTag(viewHolder);
        return convertView;
    }
}
