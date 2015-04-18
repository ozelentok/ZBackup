package ozelentok.zbackup;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import ozelentok.zbackup.ProtocolCodes.ErrorCode;
import ozelentok.zbackup.ProtocolCodes.FileType;
import ozelentok.zbackup.ProtocolCodes.RequestType;

public class SyncClient {

    private static final int bufferSize = 8192*16;
	private ClientSocket conn;
	public SyncClient(String host, short port) throws KeyManagementException, NoSuchAlgorithmException, IOException {
		conn = new ClientSocket();
		conn.connect(host, port);
	}
	
	public void close() throws IOException {
		conn.sendCode(RequestType.exit.ordinal());
		conn.close();
	}

	public boolean login(String user, String password) throws IOException {
		conn.send(user);
		conn.send(password);
		int statusCode = conn.recvCode();
		return statusCode == ErrorCode.ok.ordinal();
	}

	public int upload(String localPath, String remotePath, TransferStatus transferStatus) throws IOException, NoSuchAlgorithmException {
		conn.sendCode(RequestType.upload.ordinal());

		File f = new File(localPath);
		if (f.isDirectory()) {
			return sendDirectory(remotePath, transferStatus);
		}
		return sendFile(f, remotePath, transferStatus);
	}
	
	private int sendDirectory(String remotePath, TransferStatus transferStatus) throws IOException {
		conn.sendCode(FileType.directory.ordinal());
		conn.send(remotePath);
		transferStatus.transferredFilesCount.incrementAndGet();
		return conn.recvCode();
	}

	private int sendFile(File localFile, String remotePath, TransferStatus transferStatus) throws IOException, NoSuchAlgorithmException {
		
		// Send file type, size and path
		conn.sendCode(FileType.file.ordinal());
		conn.send(remotePath);
        conn.sendLongCode(localFile.lastModified());
		int statusCode = conn.recvCode();
		if (statusCode != ErrorCode.ok.ordinal() &&
				statusCode != ErrorCode.fileexists.ordinal()) {
			return statusCode;
		}
		
		// Check if file exists
		if (statusCode == ErrorCode.fileexists.ordinal()) {
            transferStatus.transferredBytesCount.addAndGet(localFile.length());
            transferStatus.transferredFilesCount.incrementAndGet();
			return ErrorCode.ok.ordinal();
		}

		return sendFileContents(localFile, transferStatus);
	}
	
	private int sendFileContents(File localFile, TransferStatus transferStatus) throws IOException {
		FileInputStream is = new FileInputStream(localFile);
        FileChannel fileChannel = is.getChannel();
        final long fileLength = localFile.length();
        long currentPosition = 0;
        conn.sendLongCode(fileLength);
		while (currentPosition < fileLength - bufferSize) {
			conn.send(fileChannel, currentPosition, bufferSize);
			transferStatus.transferredBytesCount.addAndGet(bufferSize);
            currentPosition += bufferSize;
		}
        int leftOverSize = (int)(fileLength - currentPosition);
        conn.send(fileChannel, currentPosition, leftOverSize);
        transferStatus.transferredBytesCount.addAndGet(leftOverSize);
        fileChannel.close();
		is.close();
		transferStatus.transferredFilesCount.incrementAndGet();
		return conn.recvCode();
	}
}
