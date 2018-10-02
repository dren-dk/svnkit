package org.tmatesoft.svn.test;

import java.io.File;
import java.io.RandomAccessFile;

import org.junit.Assert;
import org.junit.Test;
import org.tmatesoft.svn.core.internal.util.BufferedRandomAccessFile;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;

public class MiscTest {

    @Test
    public void test() throws Exception {
        final TestOptions options = TestOptions.getInstance();

        final Sandbox sandbox = Sandbox.createWithCleanup(getTestName() + ".test", options);
        try {
            final File directory = sandbox.createDirectory("tmp");
            final File file = new File(directory, "file");
            SVNFileUtil.createEmptyFile(file);

            final RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");
            try {
                final int fileSize = 10000;
                for (int i = 0; i < fileSize; i++) {
                    randomAccessFile.write(i);
                }
                final int pos = 15;
                randomAccessFile.seek(pos);
                final BufferedRandomAccessFile bufferedRandomAccessFile = new BufferedRandomAccessFile(randomAccessFile, 7);
                bufferedRandomAccessFile.loadPosition();
                for (int i = pos; i < fileSize; i++) {
                    final int b = bufferedRandomAccessFile.read();
                    Assert.assertEquals(i & 0xff, b);
                }
                bufferedRandomAccessFile.savePosition();
                Assert.assertEquals(fileSize, randomAccessFile.getFilePointer());
                Assert.assertEquals(-1, bufferedRandomAccessFile.read());
                Assert.assertEquals(-1, bufferedRandomAccessFile.read());
            } finally {
                randomAccessFile.close();
            }
        } finally {
            sandbox.dispose();
        }
    }

    private String getTestName() {
        return getClass().getSimpleName();
    }
}
