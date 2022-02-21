package org.tmatesoft.svn.core.internal.io.svn;

import org.apache.sshd.common.channel.exception.SshChannelClosedException;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Copies an input stream to a logger
 */
public class StreamLogger implements Closeable {
    private static final Logger log = Logger.getLogger(StreamLogger.class.getName());
    private int copied;
    private Thread thread;

    public StreamLogger(String name, InputStream in, Logger logger, Level level) {
        thread = new Thread(()-> {
            try {
                ByteArrayOutputStream buffy = null;
                if (logger.isLoggable(level)) {
                    buffy = new ByteArrayOutputStream();
                }
                byte[] buf = new byte[2048];
                int length;
                int emptyRead = 0;
                while (!thread.isInterrupted() || emptyRead < 10) {
                    if (buffy != null) {
                        buffy.reset();
                    }
                    while (in.available() > 0 && (length = in.read(buf)) > 0) {
                        if (buffy != null) {
                            buffy.write(buf, 0, length);
                        }
                        copied += length;
                        emptyRead = 0;
                    }
                    emptyRead++;
                    if (buffy != null) {
                        logger.log(level,"Discarded input from "+name+": "+ buffy);
                    }
                    Thread.sleep(100);
                }
            } catch (SshChannelClosedException e) {
                // Nothing to do but quit
                logger.log(level, name+ ": Channel closed "+e);
            } catch (IOException e) {
                logger.log(level, name+ ": Failed while streaming "+e);
            } catch (InterruptedException e) {
                logger.log(level, name+ ": Got interrupted");
            }
        });
        thread.setName("Piping "+name);
        thread.start();
    }

    public static void consume(InputStream errorStream) {
        new StreamLogger("consumer",errorStream, log, Level.FINE);
    }

    @Override
    public void close() {
        if (thread != null) {
            if (thread.isAlive()) {
                thread.interrupt();
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    // Meh.
                }
            }
            thread = null;
        }
    }

    public int getCopied() {
        return copied;
    }
}
