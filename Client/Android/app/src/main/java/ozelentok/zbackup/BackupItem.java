package ozelentok.zbackup;

import java.util.Date;

public class BackupItem implements java.io.Serializable {
    private String localPath;
    private Date lastBackupTime;
    private boolean isSelected;

    public BackupItem(String localPath, boolean isSelected) {
        this.localPath = localPath;
        this.isSelected = isSelected;
        this.lastBackupTime = new Date(0);
    }

    public String getLocalPath() {
        return localPath;
    }

    public void setLocalPath(String localPath) {
        this.localPath = localPath;
    }

    public Date getLastBackupTime() { return lastBackupTime; }

    public void setLastBackupTime(Date lastBackupTime) { this.lastBackupTime = lastBackupTime; }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean isSelected) {
        this.isSelected = isSelected;
    }
}
