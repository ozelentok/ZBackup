package ozelentok.zbackup;

import java.io.File;
import java.util.Iterator;
import java.util.Stack;

public class FileIterator implements Iterator<File>{

    private Stack<File> dirStack;
    private File[] fileList;
    private int fileIndex;
    private File currentFile;
    public FileIterator(String filePath) {
        File f = new File(filePath);
        currentFile = f;
        fileIndex = 0;
        dirStack = new Stack<File>();
        fileList = new File[0];
    }

    @Override
    public boolean hasNext() {
        return (currentFile != null);
    }

    @Override
    public File next() {
        File returnedFile = currentFile;
        currentFile = null;
        if (returnedFile.isDirectory()) {
            dirStack.push(returnedFile);
        }
        fileIndex += 1;
        if (fileList != null && fileIndex < fileList.length) {
            currentFile = fileList[fileIndex];
            return returnedFile;
        }
        while (!dirStack.isEmpty() && currentFile == null) {
            File currentDir = dirStack.pop();
            fileList = currentDir.listFiles();
            if (fileList != null && fileList.length > 0) {
                fileIndex = 0;
                currentFile = fileList[fileIndex];
            }
        }
        return returnedFile;
    }

    @Override
    public void remove() {}
}