package org.tmatesoft.svn.core.internal.io.svn.ssh;

import com.trilead.ssh2.auth.AgentProxy;
import org.tmatesoft.svn.core.auth.ISVNSSHHostVerifier;

import java.io.IOException;

/**
 * A pool of ssh clients that can open sessions
 */
public interface SshSessionPool {
    SshSession openSession(String host, int port, String userName,
                           char[] privateKey, char[] passphrase, char[] password, AgentProxy agentProxy,
                           ISVNSSHHostVerifier verifier, int connectTimeout, int readTimeout) throws IOException;
    void shutdown();
}
