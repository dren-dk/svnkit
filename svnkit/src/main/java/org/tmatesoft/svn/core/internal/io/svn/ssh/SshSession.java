package org.tmatesoft.svn.core.internal.io.svn.ssh;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * A Session, aka. an ssh channel
 */
public interface SshSession {
    void close();

    InputStream getOut();

    InputStream getErr();

    OutputStream getIn();

    void waitForCondition(int code, long timeout);

    void execCommand(String command) throws IOException;

    void ping() throws IOException;
}
