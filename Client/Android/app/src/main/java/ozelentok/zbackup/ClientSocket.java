package ozelentok.zbackup;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class ClientSocket {
	private static final int timeout = 300000;
	private SSLSocket socket;
	private DataOutputStream outputStream;
	private DataInputStream inputStream;
	private WritableByteChannel outputChannel;

	// Currently accepts all certificates for ease of usage, may change in the future
	static final TrustManager[] trustAllCerts = new TrustManager[] {
		new X509TrustManager() {
			public java.security.cert.X509Certificate[] getAcceptedIssuers() {
				return new X509Certificate[0];
			}
			public void checkClientTrusted(
					java.security.cert.X509Certificate[] certs, String authType) {}
			public void checkServerTrusted(
					java.security.cert.X509Certificate[] certs, String authType) {}
		}
	};
	public ClientSocket() {}

	public void connect(String host, short port) throws NoSuchAlgorithmException, KeyManagementException, IOException {
		final SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
		sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
		this.socket=(SSLSocket)sslContext.getSocketFactory().createSocket(host, port);
		this.socket.setSoTimeout(timeout);
		outputStream = new DataOutputStream(this.socket.getOutputStream());
		inputStream = new DataInputStream(this.socket.getInputStream());
		outputChannel = Channels.newChannel(outputStream);
	}

	public void send(final byte[] bytesToSend) throws IOException {
		outputStream.writeInt(bytesToSend.length);
		outputStream.write(bytesToSend, 0, bytesToSend.length);
	}

	public void send(final byte[] bytesToSend, int length) throws IOException {
		outputStream.writeInt(length);
		outputStream.write(bytesToSend, 0, length);
	}

	public void send(final String stringToSend) throws IOException {
		send(stringToSend.getBytes("UTF-8"));
	}

	public void send(final FileChannel sourceChannel, long position, int length) throws IOException
	{
		outputStream.writeInt(length);
		long bytesSent = sourceChannel.transferTo(position, length, outputChannel);
		if (bytesSent < length)
		{
			throw new IOException("Not all bytes were sent");
		}
	}

	public byte[] recv() throws IOException {
		int length = inputStream.readInt();
		byte[] bytesBuffer = new byte[length];
		inputStream.read(bytesBuffer, 0, length);
		return bytesBuffer;
	}

	public void sendCode(int code) throws IOException {
		outputStream.writeInt(code);
	}

	public int recvCode() throws IOException {
		return inputStream.readInt();
	}

	public void sendLongCode(long code) throws IOException {
		outputStream.writeLong(code);
	}

	public long recvLongCode() throws IOException {
		return inputStream.readLong();
	}

	public void close() throws IOException {
		outputChannel.close();
		outputStream.close();
		inputStream.close();
		socket.close();
	}
}
