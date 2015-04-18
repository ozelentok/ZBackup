package ozelentok.zbackup;

public class BackupStatus {

    public Boolean finished;
    public String resultMessage;

    public BackupStatus() {
        this.finished = false;
        this.resultMessage = "Not Finished";
    }
}
