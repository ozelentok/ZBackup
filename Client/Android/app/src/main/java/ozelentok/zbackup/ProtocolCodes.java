package ozelentok.zbackup;

public class ProtocolCodes {
	public enum RequestType {
		exit,
		download,
		upload
	}
	public enum ErrorCode {
		ok,
		fail,
		wrongtype,
		invalidpath,
		fileexists,
	}
	public enum FileType {
		file,
		directory
	}

}
