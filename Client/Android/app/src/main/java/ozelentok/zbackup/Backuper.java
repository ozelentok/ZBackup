package ozelentok.zbackup;

import android.widget.Toast;

import java.util.ArrayList;

public abstract class Backuper {
    protected BackupItem[] backupItems;
    protected String user;
    protected boolean onlySelected;
    public final int progressRefreshTime = 200;

    public Backuper(ArrayList<BackupItem> backupList, boolean onlySelected) {
        this.onlySelected = onlySelected;
        ArrayList<BackupItem> tempBackupList = new ArrayList<>(backupList.size());
        for (BackupItem item : backupList) {
            if (item.isSelected() || !onlySelected) {
                tempBackupList.add(item);
            }
        }
        this.backupItems = tempBackupList.toArray(new BackupItem[tempBackupList.size()]);

        if (onlySelected) {
            user = "Selected";
        } else {
            user = "All";
        }
    }
    public void backup(MainActivity activity) {
        Toast.makeText(activity, "Starting Backup...", Toast.LENGTH_SHORT).show();
    }

    public void cancel() {}
}
