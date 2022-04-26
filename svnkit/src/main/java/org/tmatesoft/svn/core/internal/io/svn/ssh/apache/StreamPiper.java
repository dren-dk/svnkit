package org.tmatesoft.svn.core.internal.io.svn.ssh.apache;

import org.apache.sshd.common.channel.exception.SshChannelClosedException;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Copies an input stream to an output stream in a thread
 */
public class StreamPiper implements Closeable {
    private int copied;
    private Thread thread;

    public StreamPiper(String name, InputStream in, OutputStream out) {
        thread = new Thread(()-> {
            final Logger log = Logger.getLogger(StreamPiper.class.getName() + "#" + name);
            try {
                byte[] buf = new byte[2048];
                int length;
                int emptyRead = 0;
                while (!thread.isInterrupted() || emptyRead < 10) {
                    while (in.available() > 0 && (length = in.read(buf)) > 0) {
                        out.write(buf, 0, length);
                        copied += length;
                        emptyRead = 0;
                    }
                    emptyRead++;
                    out.flush();
                    Thread.sleep(10);
                }
            } catch (SshChannelClosedException e) {
                log.fine("Channel closed "+e);
            } catch (IOException e) {
                log.log(Level.FINE, "Failed while streaming", e);
            } catch (InterruptedException e) {
                log.log(Level.FINE, "Got interrupted", e);
            }
        });
        thread.setName("Piping "+name);
        thread.start();
    }

    @Override
    public void close() {
        if (thread != null) {
            if (thread.isAlive()) {
                thread.interrupt();
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    throw new RuntimeException("Got interrupted", e);
                }
            }
            thread = null;
        }
    }


    public int getCopied() {
        return copied;
    }
}
