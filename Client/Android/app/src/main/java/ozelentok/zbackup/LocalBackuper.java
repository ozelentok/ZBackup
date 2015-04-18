package ozelentok.zbackup;

import android.app.NotificationManager;
import android.content.Context;
import android.os.AsyncTask;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class LocalBackuper extends Backuper{

    private String storageDir;
    public LocalBackuper(ArrayList<BackupItem> backupList, boolean onlySelected, String storageDir) {
        super(backupList, onlySelected);
        this.storageDir = storageDir;
    }

    public void backup(MainActivity activity) {
        LocalBackupTask backupTask = new LocalBackupTask(storageDir + '/' + user, activity);
        backupTask.execute(backupItems);
        super.backup(activity);
    }

    private class LocalBackupTask extends AsyncTask<BackupItem, Integer, String> {
        private String storageDir;
        private MainActivity activity;
        private NotificationManager nManager;
        private NotificationCompat.Builder nBuilder;
        private NotificationCompat.BigTextStyle style;
        private TransferStatus status;
        private static final int NOTIFICATION_ID = 1;

        public LocalBackupTask(String storageDir, MainActivity activity) {
            super();
            this.activity = activity;
            this.activity.lockBackup();
            this.storageDir = storageDir;
            new File(storageDir).mkdir();
            this.nManager = (NotificationManager) activity.getSystemService(Context.NOTIFICATION_SERVICE);
            this.nBuilder = new NotificationCompat.Builder(activity);
            this.style = new NotificationCompat.BigTextStyle().setBigContentTitle("Local Backup in Progress");
            this.status = new TransferStatus();
            nBuilder.setContentTitle("Local Backup in Progress");
            nBuilder.setSmallIcon(R.drawable.ic_notification);
            nBuilder.setStyle(style);
            nBuilder.setOngoing(true);
        }

        protected String doInBackground(BackupItem... backupItems) {
            for (BackupItem item : backupItems) {
                String path = item.getLocalPath();
                for (FileIterator iter = new FileIterator(path); iter.hasNext(); ) {
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
                initiateZipping(backupStatus);
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

        private void addRootFileToZipStream(File f, ZipOutputStream zipStream, byte[] buffer) throws IOException {
            if (!f.isDirectory()) {
                FileInputStream inStream = new FileInputStream(f);
                String filename = f.getName();
                zipStream.putNextEntry(new ZipEntry(filename));
                int bytesRead = inStream.read(buffer);
                while (bytesRead > -1) {
                    zipStream.write(buffer, 0, bytesRead);
                    status.transferredBytesCount.addAndGet(bytesRead);
                    bytesRead = inStream.read(buffer);
                }
                inStream.close();
            }
            status.transferredFilesCount.incrementAndGet();
        }

        private void addFileToZipStream(File f, ZipOutputStream zipStream, String localRootPath, byte[] buffer) throws IOException {
            String filepath = f.getAbsolutePath().replaceFirst(localRootPath, "");
            if (f.isDirectory()) {
                zipStream.putNextEntry(new ZipEntry(filepath + '/'));
            } else {
                FileInputStream inStream = new FileInputStream(f);
                zipStream.putNextEntry(new ZipEntry(filepath));
                int bytesRead = inStream.read(buffer);
                while (bytesRead > -1) {
                    zipStream.write(buffer, 0, bytesRead);
                    status.transferredBytesCount.addAndGet(bytesRead);
                    bytesRead = inStream.read(buffer);
                }
                inStream.close();
            }
            status.transferredFilesCount.incrementAndGet();
        }

        private void initiateZipping(final BackupStatus backupStatus) {
            Runnable zipping_task = new Runnable() {
                @Override
                public void run() {
                    try {
                        byte buffer[] = new byte[8192];
                        for (BackupItem item : backupItems) {
                            String path = item.getLocalPath();
                            FileIterator iter = new FileIterator(path);
                            File rootFile = iter.next();
                            String filename = rootFile.getAbsolutePath().substring(1).replaceAll("/", "_") + ".zip";
                            FileOutputStream fileStream = new FileOutputStream(new File(storageDir, filename));
                            ZipOutputStream zipStream = new ZipOutputStream(new BufferedOutputStream(fileStream));
                            addRootFileToZipStream(rootFile, zipStream, buffer);
                            while (iter.hasNext()) {
                                File f = iter.next();
                                addFileToZipStream(f, zipStream, path, buffer);
                            }
                            zipStream.close();
                            item.setLastBackupTime(new Date());
                        }
                        backupStatus.resultMessage = "Local Backup Successful";
                    } catch (final Exception e) {
                        backupStatus.resultMessage = e.getMessage();
                    } finally {
                        backupStatus.finished = true;
                    }
                }
            };
            (new Thread(zipping_task)).start();
        }

        protected void onProgressUpdate(Integer... progress) {
            nBuilder.setProgress(100, progress[0], false);
            String formatedProgress = progress[0] + "%";
            nBuilder.setContentText(formatedProgress);
            style.bigText(formatedProgress);
            nManager.notify(NOTIFICATION_ID, nBuilder.build());
        }

        protected void onPostExecute(String result) {
            nBuilder.setContentTitle("Local Backup Finished");
            nBuilder.setContentText(result);
            style.setBigContentTitle("Local Backup Finished");
            style.bigText(result);
            nBuilder.setProgress(0, 0, false);
            nBuilder.setOngoing(false);
            nManager.notify(NOTIFICATION_ID, nBuilder.build());
            activity.unlockBackup();
            activity.refreshAdapter(MainActivity.LOCAL_ARRAY);
        }
    }

}
