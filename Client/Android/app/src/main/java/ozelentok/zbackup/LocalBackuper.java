package ozelentok.zbackup;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.AsyncTask;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.AesKeyStrength;
import net.lingala.zip4j.model.enums.CompressionLevel;
import net.lingala.zip4j.model.enums.CompressionMethod;
import net.lingala.zip4j.model.enums.EncryptionMethod;
import net.lingala.zip4j.progress.ProgressMonitor;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;

public class LocalBackuper extends Backuper {

    private String storageDir;
    private LocalBackupTask backupTask;
	private char[] password;

	private static final String CHANNEL_ID = "LocalBackup";
	private static final String CHANNEL_NAME = "Local Backup";

	public LocalBackuper(ArrayList<BackupItem> backupList, boolean onlySelected,
						 String storageDir, char[] password) {
		super(backupList, onlySelected);
		this.storageDir = storageDir;
		this.password = password;
	}

	public void backup(MainActivity activity) {
		backupTask = new LocalBackupTask(storageDir + '/' + user, password, activity);
		backupTask.execute(backupItems);
		super.backup(activity);
	}

	public void cancel() {
		if (backupTask != null) {
			backupTask.cancel(true);
		}
	}

	private class LocalBackupTask extends AsyncTask<BackupItem, Integer, String> {
		private String storageDir;
		private MainActivity activity;
		private NotificationManager nManager;
		private Notification.Builder nBuilder;
		private Notification.BigTextStyle style;
		private char[] zipPassword;
		private static final int NOTIFICATION_ID = 1;

		public LocalBackupTask(String storageDir, char[] zipPassword, MainActivity activity) {
			super();
			this.activity = activity;
			this.activity.lockBackup();
			this.storageDir = storageDir;
			this.zipPassword = zipPassword;
			new File(storageDir).mkdir();

			this.nManager = (NotificationManager) activity.getSystemService(Context.NOTIFICATION_SERVICE);
			this.nBuilder = new Notification.Builder(activity, CHANNEL_ID);
			this.style = new Notification.BigTextStyle().setBigContentTitle("Local Backup in Progress");
			nBuilder.setContentTitle("Local Backup in Progress");
			nBuilder.setSmallIcon(R.drawable.ic_notification);
			nBuilder.setStyle(style);
			nBuilder.setOngoing(true);
			NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME,
					NotificationManager.IMPORTANCE_HIGH);
			channel.setSound(null, null);
			nManager.createNotificationChannel(channel);
		}

		protected String doInBackground(BackupItem... backupItems) {
			try {
				publishProgress(0);
				BackupStatus backupStatus = new BackupStatus();
				zip(backupStatus);
				return backupStatus.resultMessage;
			} catch (Exception e) {
				return e.getMessage();
			}
		}

		private void zip(final BackupStatus backupStatus) {
			try {
				for (BackupItem item : backupItems) {
					if (LocalBackupTask.this.isCancelled()) {
						backupStatus.resultMessage = "Local Backup Cancelled";
						return;
					}
					Date newBackupTime = new Date();

					File rootFile = new File(item.getLocalPath());
					ZipFile zipFile = createZip(rootFile);
					ProgressMonitor progressMonitor = addToZip(zipFile, rootFile);

					while (progressMonitor.getState() == ProgressMonitor.State.BUSY) {
						if (LocalBackupTask.this.isCancelled()) {
							progressMonitor.setCancelAllTasks(true);
							throw new RuntimeException("Local Backup Cancelled");
						}
						int progress = progressMonitor.getPercentDone();
						publishProgress(progress);
						Thread.sleep(progressRefreshTime);
					}

					if (LocalBackuper.this.onlySelected) {
						item.setLastSelectedBackupTime(newBackupTime);
					} else {
						item.setLastFullBackupTime(newBackupTime);
					}
				}
				backupStatus.resultMessage = "Local Backup Successful";
			} catch (final Exception e) {
				backupStatus.resultMessage = e.getMessage();
			} finally {
				backupStatus.finished = true;
			}
		}

		private ZipFile createZip(File rootFile) throws ZipException {
			String filename = rootFile.getAbsolutePath().substring(1).replaceAll("/", "_") + ".zip";
			String filepath = new File(storageDir, filename).getAbsolutePath();
			if (zipPassword != null && zipPassword.length > 0) {
				return new ZipFile(filepath);
			}
			return new ZipFile(filepath, zipPassword);

		}

		private ProgressMonitor addToZip(ZipFile zipFile, File rootFile) throws ZipException {
			zipFile.setRunInThread(true);
			ZipParameters zipParameters = new ZipParameters();
			zipParameters.setCompressionMethod(CompressionMethod.DEFLATE);
			zipParameters.setCompressionLevel(CompressionLevel.FAST);

			if (zipPassword.length > 0) {
				zipParameters.setEncryptFiles(true);
				zipParameters.setEncryptionMethod(EncryptionMethod.AES);
				zipParameters.setAesKeyStrength(AesKeyStrength.KEY_STRENGTH_256);
			}
			if (rootFile.isDirectory()) {
				zipParameters.setIncludeRootFolder(false);
				zipFile.addFolder(rootFile, zipParameters);
			} else {
				zipFile.addFile(rootFile, zipParameters);
			}
			return zipFile.getProgressMonitor();
		}

		protected void onProgressUpdate(Integer... progress) {
			nBuilder.setProgress(100, progress[0], false);
			String formattedProgress = progress[0] + "%";
			nBuilder.setContentText(formattedProgress);
			style.bigText(formattedProgress);
			nManager.notify(NOTIFICATION_ID, nBuilder.build());
		}

		protected void onCancelled(String result) {
			backupTaskDone("Local Backup Cancelled", result);
		}

		protected void onPostExecute(String result) {
			backupTaskDone("Local Backup Finished",result);
		}

		private void clearPassword()
		{
			for (int i = 0; i < password.length; i++) {
				password[i] = (char)(Math.random() * 0x100);
			}
		}

		private void backupTaskDone(String title, String result) {
			this.clearPassword();
			nBuilder.setContentTitle(title);
			nBuilder.setContentText(result);
			style.setBigContentTitle(title);
			style.bigText(result);
			nBuilder.setProgress(0, 0, false);
			nBuilder.setOngoing(false);
			nManager.notify(NOTIFICATION_ID, nBuilder.build());
			activity.unlockBackup();
			activity.refreshAdapter(MainActivity.LOCAL_ARRAY);
		}
	}
}
