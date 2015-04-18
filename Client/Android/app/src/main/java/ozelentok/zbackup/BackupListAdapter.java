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

public class BackupListAdapter extends BaseAdapter {
    private final ArrayList<BackupItem> backupItems;
    private final MainActivity activity;
    private final AdapterView.OnItemClickListener itemClickListener;
    private final AdapterView.OnItemLongClickListener itemLongClickListener;

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat(
            "yyyy-MM-dd EEE HH:mm:ss"
    );

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
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater)activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.backup_item_layout, null);
        }
        BackupItem item = backupItems.get(position);
        setViewDetails(convertView, item);
        return convertView;
    }

    private void setViewDetails(View view, final BackupItem item) {
        TextView localPathView = (TextView) view.findViewById(R.id.local_path_view);
        TextView lastBackupView = (TextView) view.findViewById(R.id.last_backup_view);
        CheckBox isSelectedCheckBox = (CheckBox) view.findViewById(R.id.backup_check_box);
        localPathView.setText(item.getLocalPath());
        if (item.getLastBackupTime().getTime() > 0) {
            lastBackupView.setText(dateFormat.format(item.getLastBackupTime()));
        } else {
            lastBackupView.setText("");
        }
        isSelectedCheckBox.setChecked(item.isSelected());
        isSelectedCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                item.setSelected(isChecked);
            }
        });
    }
}
