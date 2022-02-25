package org.tmatesoft.svn.core.internal.io.svn.ssh;

import org.tmatesoft.svn.core.internal.io.svn.ssh.apache.ApacheSshSessionPool;
import org.tmatesoft.svn.core.internal.io.svn.ssh.trilead.TrileadSshSessionPool;

import java.util.logging.Logger;

/**
 * This is where SshSessionPool instances are born
 */
public class SessionPoolFactory {
    private static final Logger log = Logger.getLogger(SessionPoolFactory.class.getName());
    public static final String TRILEAD = "trilead";
    public static final String SVNKIT_SSH_CLIENT = "svnkit.ssh.client";
    public static final String APACHE = "apache";

    public static SshSessionPool create() {
        final String implementationName = System.getProperty(SVNKIT_SSH_CLIENT, TRILEAD);

        if (implementationName.equals(TRILEAD)) {
            log.warning("Using the obsolete " + TRILEAD + " ssh client implementation, consider switching to " + APACHE +
                    " by using -D" + SVNKIT_SSH_CLIENT + "=" + APACHE);
            return new TrileadSshSessionPool();
        } else {
            return new ApacheSshSessionPool();
        }
    }
}
