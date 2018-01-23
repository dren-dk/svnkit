package org.tmatesoft.svn.core.internal.util;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.util.logging.Level;

import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.util.SVNDebugLog;
import org.tmatesoft.svn.util.SVNLogType;

public class SVNDoubleLock {

    public static SVNDoubleLock obtain(File file, boolean exclusive) throws SVNException {
        //FLOCK
        final SVNFLock flock = SVNFLock.obtain(file, exclusive);

        //POSIX lock
        RandomAccessFile randomAccessFile = null;
        FileLock posixLock = null;
        try {
            randomAccessFile = new RandomAccessFile(file, "rw");
            posixLock = randomAccessFile.getChannel().lock(0, Long.MAX_VALUE, !exclusive);
        } catch (IOException e) {
            if (flock != null) {
                flock.release();
            }
            final SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, e);
            SVNErrorManager.error(errorMessage, SVNLogType.DEFAULT);
        }
        return new SVNDoubleLock(flock, randomAccessFile, posixLock, file, exclusive);
    }

    private SVNFLock flock; //null if flocks are not supported on the platform
    private RandomAccessFile randomAccessFile;
    private FileLock posixLock;

    //fields for toString() method:
    private final File file;
    private final boolean exclusive;
    private boolean valid;

    private SVNDoubleLock(SVNFLock flock, RandomAccessFile randomAccessFile, FileLock posixLock, File file, boolean exclusive) {
        this.flock = flock;
        this.randomAccessFile = randomAccessFile;
        this.posixLock = posixLock;
        this.file = file;
        this.exclusive = exclusive;
        this.valid = true;
    }

    public void release() {
        //release locks in the reverse order: POSIX lock first, FLOCK second
        try {
            if (posixLock != null) {
                posixLock.release();
                posixLock = null;
            }
            if (randomAccessFile != null) {
                randomAccessFile.close();
                randomAccessFile = null;
            }
            if (flock != null) {
                flock.release();
                flock = null;
            }
            valid = false;
        } catch (IOException e) {
            SVNDebugLog.getDefaultLog().log(SVNLogType.DEFAULT, e, Level.INFO);
        }
    }

    public String toString() {
        if (!valid) {
            return "SVNDoubleLock{file=" + file + ", valid = false, exclusive=" + exclusive + "}";
        } else {
            return "SVNDoubleLock{" +
                    ((flock != null) ? ("flock=" + flock) : "flock is not supported") +
                    ", posixLock=" + posixLock +
                    '}';
        }
    }
}
