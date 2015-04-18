package ozelentok.zbackup;

import android.view.View;
import android.widget.AdapterView;

public class BackupItemClickListener implements AdapterView.OnItemClickListener {

    private MainActivity activity;

    public BackupItemClickListener(MainActivity activity) {
        this.activity = activity;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        activity.openItemEditDialog(position);
    }
}
