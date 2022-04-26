package org.tmatesoft.svn.core.internal.io.svn.ssh.apache;

import org.apache.sshd.client.channel.ChannelExec;
import org.apache.sshd.client.channel.ClientChannelEvent;
import org.apache.sshd.client.session.ClientSession;
import org.tmatesoft.svn.core.internal.io.svn.ssh.SshSession;

import java.io.*;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ApacheSshSession implements SshSession {

    private static final Logger log = Logger.getLogger(ApacheSshSession.class.getName());
    private SshConnection connection;
    private ChannelExec channel;
    private PipedInputStream out;
    private PipedInputStream err;
    private PipedOutputStream in;
    private static int execCount;

    public ApacheSshSession(SshConnection connection) {
        this.connection = connection;
    }

    public static int getExecCount() {
        return execCount;
    }

    public void close() {
        if (channel != null) {
            try {
                channel.close();
                channel.waitFor(Collections.singleton(ClientChannelEvent.CLOSED), 10000);
            } catch (Exception e) {
                log.log(Level.WARNING, "Failed to close channel", e);
            }
        }
//        waitForCondition(ChannelCondition.CLOSED, 0);
        connection.sessionClosed(this);
    }    
    
    public InputStream getOut() {
        if (in == null) {
            throw new IllegalStateException("execCommand must be called first");
        }
        return out;
    }

    public InputStream getErr() {
        if (in == null) {
            throw new IllegalStateException("execCommand must be called first");
        }
        return err;
    }
    
    public OutputStream getIn() {
        if (in == null) {
            throw new IllegalStateException("execCommand must be called first");
        }
        return in;
    }

    @Override
    public void waitForCondition(int code, long timeout) {
        // TODO
    }

    public void execCommand(String command) throws IOException {
        execCount++;
        try {
            tryExecCommand(command);
        } catch (Exception e) {
            close();
            connection = connection.reOpen();
            tryExecCommand(command);
        }
    }

    private void tryExecCommand(String command) throws IOException {
        channel = connection.getSession().createExecChannel(command);
        out = new PipedInputStream();
        channel.setOut(new PipedOutputStream(out));
        err = new PipedInputStream();
        channel.setErr(new PipedOutputStream(err));
        in = new PipedOutputStream();
        channel.setIn(new PipedInputStream(in));
        channel.open().await();
        log.finest(() -> "Opened connection with " + command);
    }

    /**
     * This doesn't really ping the server, because that's slow and useless anyway as we can silently re-connect to the server
     * in case we end up in a race-condition where the server has closed the connection between the call to ping() and
     * the call to execCommand()
     */
    public void ping() throws IOException {
        final ClientSession session = connection.getSession();
        if (!session.isOpen()) {
            throw new IOException("Session is not open");
        }

        if (session.isClosing()) {
            throw new IOException("Session is closing");
        }
    }
}
