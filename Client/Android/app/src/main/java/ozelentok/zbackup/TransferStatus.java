package ozelentok.zbackup;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class TransferStatus {
	public AtomicLong transferredBytesCount;
	public long totalBytesCount;
	public AtomicInteger transferredFilesCount;
	public int totalFilesCount;
	
	public TransferStatus() {
		this(0L, 0);
	}
	
	public TransferStatus(long totalBytesCount, int totalFilesCount) {
		this.transferredBytesCount = new AtomicLong(0);
		this.transferredFilesCount = new AtomicInteger(0);
		this.totalBytesCount = totalBytesCount;
		this.totalFilesCount = totalFilesCount;
	}
}
