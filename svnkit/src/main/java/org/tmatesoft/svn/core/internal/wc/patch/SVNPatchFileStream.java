/*
 * ====================================================================
 * Copyright (c) 2004-2010 TMate Software Ltd.  All rights reserved.
 *
 * This software is licensed as described in the file COPYING, which
 * you should have received as part of this distribution.  The terms
 * are also available at http://svnkit.com/license.html.
 * If newer versions of this license are posted there, you may use a
 * newer version instead, at your option.
 * ====================================================================
 */
package org.tmatesoft.svn.core.internal.wc.patch;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.logging.Level;

import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.util.BufferedRandomAccessFile;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.util.SVNLogType;

/**
 * @version 1.3
 * @author TMate Software Ltd.
 */
public class SVNPatchFileStream {

    public interface SVNPatchFileLineFilter {

        boolean lineFilter(String line);
    }

    public interface SVNPatchFileLineTransformer {

        String lineTransformer(String line);
    }

    private File path;
    private boolean write;
    private long start = 0;
    private long end = -1;

    private RandomAccessFile file;
    private BufferedRandomAccessFile bufferedFile;

    private SVNPatchFileLineFilter lineFilter;
    private SVNPatchFileLineTransformer lineTransformer;

    private SVNPatchFileStream(File path, boolean write, long start, long end) {
        this(path, write);
        if (start >= 0 && end >= 0 && start <= end) {
            this.start = start;
            this.end = end;
        } else {
            throw new IllegalArgumentException("Bad start or end");
        }
    }

    private SVNPatchFileStream(File path, boolean write) {
        if (path != null) {
            this.path = path;
        } else {
            throw new IllegalArgumentException("Bad path");
        }
        this.write = write;
    }

    public static SVNPatchFileStream openReadOnly(File path) throws IOException, SVNException {
        final SVNPatchFileStream stream = new SVNPatchFileStream(path, false);
        stream.reset();
        return stream;
    }

    public static SVNPatchFileStream openRangeReadOnly(File path, long start, long end) throws IOException, SVNException {
        final SVNPatchFileStream stream = new SVNPatchFileStream(path, false, start, end);
        stream.reset();
        return stream;
    }

    public static SVNPatchFileStream openForWrite(File path) throws IOException, SVNException {
        final SVNPatchFileStream stream = new SVNPatchFileStream(path, true);
        stream.reset();
        return stream;
    }

    public File getPath() {
        return path;
    }

    public void setLineFilter(SVNPatchFileLineFilter lineFilter) {
        this.lineFilter = lineFilter;
    }

    public void setLineTransformer(SVNPatchFileLineTransformer lineTransfomer) {
        this.lineTransformer = lineTransfomer;
    }

    private RandomAccessFile getFile() throws SVNException {
        if (file == null) {
            synchronized (this) {
                if (file == null) {
                    if (write) {
                        file = SVNFileUtil.openRAFileForWriting(path, false);
                        bufferedFile = null;
                    } else {
                        file = SVNFileUtil.openRAFileForReading(path);
                        bufferedFile = new BufferedRandomAccessFile(file);
                    }
                }
            }
        }
        return file;
    }

    /**
     * Reset a generic stream back to its origin. E.g. On a file this would be
     * implemented as a seek to position 0). This function returns a
     * #SVN_ERR_STREAM_RESET_NOT_SUPPORTED error when the stream doesn't
     * implement resetting.
     * 
     * @throws IOException
     * @throws SVNException
     */
    public void reset() throws IOException, SVNException {
        final RandomAccessFile file = getFile();
        if (start != file.getFilePointer()) {
            file.seek(start);
        }
    }

    public void close() throws IOException {
        if (file != null) {
            file.close();
        }
    }

    public boolean isEOF() throws IOException, SVNException {
        final RandomAccessFile file = getFile();
        return file.getFilePointer() >= (end > 0 ? end : file.length());
    }

    public long getSeekPosition() throws SVNException, IOException {
        final RandomAccessFile file = getFile();
        return file.getFilePointer();
    }

    public void setSeekPosition(long pos) throws SVNException, IOException {
        checkPos(pos);
        final RandomAccessFile file = getFile();
        file.seek(pos);
    }

    private void checkPos(long pos) throws SVNException {
        if (!isPosValid(pos)) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, "Bad position for file ''{0}'': {1}. Range is {2}..{3}.", new Object[] {
                    path, pos, start, end
            });
            SVNErrorManager.error(err, Level.FINE, SVNLogType.DEFAULT);
        }
    }

    private boolean isPosValid(long pos) {
        return start <= pos && (end > 0 ? pos <= end : true);
    }

    public void write(String str) throws SVNException, IOException {
        final RandomAccessFile file = getFile();
        final long pos = file.getFilePointer();
        checkPos(pos + str.length());
        file.write(str.getBytes());
    }

    public void write(StringBuffer str) throws SVNException, IOException {
        write(str.toString());
    }

    public void tryWrite(StringBuffer lineBuf) throws SVNException, IOException {
        write(lineBuf);
    }

    public boolean readLineWithEol(StringBuffer lineBuf, StringBuffer eolStr) throws IOException, SVNException {
        return readLine(lineBuf, eolStr, true);
    }

    public boolean readLine(StringBuffer lineBuf) throws IOException, SVNException {
        return readLine(lineBuf, null);
    }

    public boolean readLine(StringBuffer lineBuf, String eolStr) throws IOException, SVNException {
        final StringBuffer eol = eolStr != null ? new StringBuffer(eolStr) : null;
        return readLine(lineBuf, eol, false);
    }

    private boolean readLine(StringBuffer input, StringBuffer eolStr, boolean detectEol) throws IOException, SVNException {
        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        int c;
        boolean eol;
        boolean filtered;

        bufferedFile.loadPosition();
        try {
            do {

                c = -1;
                eol = false;
                filtered = false;

                byteArrayOutputStream.reset();
                if (eolStr != null) {
                    eolStr.setLength(0);
                }

                while (!eol) {
                    switch (c = bufferedFile.read()) {
                        case -1:
                            eol = true;
                            break;
                        case '\n':
                            if (detectEol && eolStr != null) {
                                eolStr.append((char) c);
                            }
                            eol = true;
                            break;
                        case '\r':
                            if (detectEol && eolStr != null) {
                                eolStr.append((char) c);
                            }
                            long cur = bufferedFile.getFilePointer();
                            final int c2 = bufferedFile.read();
                            if (c2 != '\n') {
                                bufferedFile.seek(cur);
                            } else {
                                if (detectEol && eolStr != null) {
                                    eolStr.append((char) c2);
                                }
                            }
                            eol = true;
                            break;
                        default:
                            byteArrayOutputStream.write(c);
                            break;
                    }
                }

                if (lineFilter != null) {
                    filtered = lineFilter.lineFilter(byteArrayOutputStream.toString("UTF-8"));
                    if (filtered) {
                        byteArrayOutputStream.reset();
                    }
                }

            } while (filtered && !isEOF());

            if (lineTransformer != null) {
                final String line = lineTransformer.lineTransformer(byteArrayOutputStream.toString("UTF-8"));
                byteArrayOutputStream.reset();
                byteArrayOutputStream.write(line.getBytes("UTF-8"));
            }
            input.append(byteArrayOutputStream.toString("UTF-8"));

            return byteArrayOutputStream.size() == 0 && c == -1 && isEOF();
        } finally {
            try {
                bufferedFile.savePosition();
            } catch (Throwable th) {
                //
            }
        }
    }

}
