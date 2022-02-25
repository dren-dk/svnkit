package org.tmatesoft.svn.core.internal.io.svn.ssh.trilead;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.trilead.ssh2.ChannelCondition;
import com.trilead.ssh2.Session;
import org.tmatesoft.svn.core.internal.io.svn.ssh.SshSession;

public class TrileadSshSession implements SshSession {
    
    private SshConnection myOwner;
    private Session mySession;

    public TrileadSshSession(SshConnection owner, Session session) {
        mySession = session;
        myOwner = owner;
    }
    
    @Override
    public void close() {
        mySession.close();
        waitForCondition(ChannelCondition.CLOSED, 0);        
        myOwner.sessionClosed(this);
    }    
    
    @Override
    public InputStream getOut() {
        return mySession.getStdout();
    }

    @Override
    public InputStream getErr() {
        return mySession.getStderr();        
    }
    
    @Override
    public OutputStream getIn() {
        return mySession.getStdin();
    }
    
    public Integer getExitCode() {
        return mySession.getExitStatus();
    }
    
    public String getExitSignal() {
        return mySession.getExitSignal();
    }
    
    @Override
    public void waitForCondition(int code, long timeout) {
        mySession.waitForCondition(code, timeout);
    }
    
    @Override
    public void execCommand(String command) throws IOException {
        mySession.execCommand(command);
    }
    
    @Override
    public void ping() throws IOException {
        mySession.ping();
    }
}
