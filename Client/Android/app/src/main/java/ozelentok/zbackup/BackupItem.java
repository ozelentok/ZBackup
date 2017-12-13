package ozelentok.zbackup;

import java.util.Date;

public class BackupItem implements java.io.Serializable {
    private String localPath;
    private boolean isSelected;
    private Date lastFullBackupTime;
    private Date lastSelectedBackupTime;


    public BackupItem(String localPath, boolean isSelected) {
        this.localPath = localPath;
        this.isSelected = isSelected;
        this.lastFullBackupTime = new Date(0);
        this.lastSelectedBackupTime = new Date(0);
    }

    public String getLocalPath() {
        return localPath;
    }

    public void setLocalPath(String localPath) {
        this.localPath = localPath;
    }

    public Date getLastFullBackupTime() {
        return lastFullBackupTime;
    }

    public void setLastFullBackupTime(Date lastFullBackupTime) {
        this.lastFullBackupTime = lastFullBackupTime;
    }

    public Date getLastSelectedBackupTime() {
        return lastSelectedBackupTime;
    }

    public void setLastSelectedBackupTime(Date lastSelectedBackupTime) {
        this.lastSelectedBackupTime = lastSelectedBackupTime;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean isSelected) {
        this.isSelected = isSelected;
    }
}
