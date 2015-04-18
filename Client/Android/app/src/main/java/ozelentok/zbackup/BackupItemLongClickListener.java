package ozelentok.zbackup;

import android.view.View;
import android.widget.AdapterView;

public class BackupItemLongClickListener implements AdapterView.OnItemLongClickListener {

    private MainActivity activity;

    public BackupItemLongClickListener(MainActivity activity) {
        this.activity = activity;
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        activity.openItemDeletionDialog(position);
        return true;
    }
}
