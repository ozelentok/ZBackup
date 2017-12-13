package ozelentok.zbackup;

import android.app.NotificationManager;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimerTask;

import ozelentok.zbackup.ProtocolCodes.ErrorCode;


public class NetworkBackuper extends Backuper {

    private String server;
    private short port;
    private String password;
    private NetworkBackupTask backupTask;

    public NetworkBackuper(ArrayList<BackupItem> backupList, boolean onlySelected, String server, short port, String password) {
        super(backupList, onlySelected);
        this.server = server;
        this.port = port;
        this.password = password;
    }

    @Override
    public void backup(MainActivity activity) {
        backupTask = new NetworkBackupTask(server, port, user, password, activity);
        backupTask.execute(backupItems);
        super.backup(activity);
    }

    public void cancel() {
        if (backupTask != null) {
            backupTask.cancel(true);
        }
    }

    private class NetworkBackupTask extends AsyncTask<BackupItem, Integer, String> {
        private String server;
        private short port;
        private String user;
        private String password;

        private MainActivity activity;
        private NotificationManager nManager;
        private NotificationCompat.Builder nBuilder;
        private NotificationCompat.BigTextStyle style;
        private TransferStatus status;
        private static final int NOTIFICATION_ID = 2;


        private NetworkBackupTask(String server, short port, String user, String password, MainActivity activity) {
            super();
            this.activity = activity;
            this.activity.lockBackup();
            this.server = server;
            this.port = port;
            this.user = user;
            this.password = password;
            this.nManager = (NotificationManager) activity.getSystemService(Context.NOTIFICATION_SERVICE);
            this.nBuilder = new NotificationCompat.Builder(activity);
            this.style = new NotificationCompat.BigTextStyle().setBigContentTitle("Network Backup in Progress");
            this.status = new TransferStatus();
            nBuilder.setContentTitle("Network Backup in Progress");
            nBuilder.setSmallIcon(R.drawable.ic_notification);
            nBuilder.setStyle(style);
            nBuilder.setOngoing(true);
        }

        protected String doInBackground(BackupItem... backupItems) {
            for (BackupItem item : backupItems) {
                String path = item.getLocalPath();
                for (FileIterator iter = new FileIterator(path); iter.hasNext(); ) {
                    if (isCancelled()) {
                        return "Network Backup Cancelled";
                    }
                    File f = iter.next();
                    if (!f.isDirectory()) {
                        status.totalBytesCount += f.length();
                    }
                    status.totalFilesCount += 1;
                }
            }
            try {
                publishProgress(0);
                BackupStatus backupStatus = new BackupStatus();
                initiateUpload(backupStatus, server, port, user, password);
                while(!backupStatus.finished)
                {
                    int progress = (int) ((status.transferredBytesCount.get() / (float) status.totalBytesCount) * 100);
                    publishProgress(progress);
                    Thread.sleep(progressRefreshTime);
                }
                return backupStatus.resultMessage;
            }

            catch (Exception e) {
                return e.getMessage();
            }
        }

        private void uploadRootFileToServer(File f, SyncClient client, String remoteRoot) throws IOException, NoSuchAlgorithmException {
            int code = client.upload(
                    f.getAbsolutePath(),
                    remoteRoot,
                    status);
            if (code != ErrorCode.ok.ordinal()) {
                throw new IOException("Error Uploading " + f.getAbsolutePath() + "\nError Code:" + code);
            }
        }

        private void uploadFileToServer(File f, SyncClient client, String localRootPath, String remoteRootPath) throws IOException, NoSuchAlgorithmException {
            String filePath = f.getAbsolutePath();
            String remoteFilePath = f.getAbsolutePath().replaceFirst(localRootPath, remoteRootPath);
            int code = client.upload(
                    filePath,
                    remoteFilePath,
                    status);
            if (code != ErrorCode.ok.ordinal()) {
                throw new IOException("Error Uploading " + f.getAbsolutePath() + "\nError Code:" + code);
            }
        }

        private void initiateUpload(final BackupStatus backupStatus, final String server, final short port, final String user, final String password) {
            Runnable upload_task = new Runnable() {
                @Override
                public void run() {
                    try {
                        SyncClient client = new SyncClient(server, port);
                        boolean loggedIn = client.login(user, password);
                        if (!loggedIn) {
                            throw new IOException("Failed Login");
                        }
                        for (BackupItem item : backupItems) {
                            if (NetworkBackupTask.this.isCancelled()) {
                                backupStatus.resultMessage = "Network Backup Cancelled";
                                client.close();
                                return;
                            }
                            long itemLastBackupTime = item.getLastFullBackupTime().getTime();
                            if (NetworkBackuper.this.onlySelected) {
                                itemLastBackupTime = item.getLastSelectedBackupTime().getTime();
                            }
                            Date newBackupTime = new Date();

                            String path = item.getLocalPath();
                            FileIterator iter = new FileIterator(path);
                            File rootFile = iter.next();
                            String remoteRoot = rootFile.getAbsolutePath().substring(1).replaceAll("/", "_");
                            uploadRootFileToServer(rootFile, client, remoteRoot);
                            while (iter.hasNext()) {
                                if (NetworkBackupTask.this.isCancelled()) {
                                    backupStatus.resultMessage = "Network Backup Cancelled";
                                    client.close();
                                    return;
                                }
                                File f = iter.next();
                                if (f.isFile() && f.lastModified() < itemLastBackupTime) {
                                	continue;
                                }
                                uploadFileToServer(f, client, path, remoteRoot);
                            }

							if (NetworkBackuper.this.onlySelected) {
								item.setLastSelectedBackupTime(newBackupTime);
							} else {
                                item.setLastFullBackupTime(newBackupTime);
                            }
                        }
                        client.close();
                        backupStatus.resultMessage = "Network Backup Successful";
                    } catch (final Exception e) {
                        backupStatus.resultMessage = e.getMessage();
                    } finally {
                        backupStatus.finished = true;
                    }
                }
            };
            (new Thread(upload_task)).start();
        }

        protected void onProgressUpdate(Integer... progress) {
            nBuilder.setProgress(100, progress[0], false);
            String formattedProgress = progress[0] + "%";
            nBuilder.setContentText(formattedProgress);
            style.bigText(formattedProgress);
            nManager.notify(NOTIFICATION_ID, nBuilder.build());
        }

        protected void onCancelled(String result) {
            backupTaskDone("Network Backup Cancelled", result);
        }

        protected void onPostExecute(String result) {
            backupTaskDone("Network Backup Finished", result);
        }

        private void backupTaskDone(String title, String result) {
            nBuilder.setContentTitle(title);
            nBuilder.setContentText(result);
            style.setBigContentTitle(title);
            style.bigText(result);
            nBuilder.setProgress(0, 0, false);
            nBuilder.setOngoing(false);
            nManager.notify(NOTIFICATION_ID, nBuilder.build());
            activity.unlockBackup();
            activity.refreshAdapter(MainActivity.NETWORK_ARRAY);
        }
    }

}
