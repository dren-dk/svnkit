package org.tmatesoft.svn.core.wc;

import org.junit.Assert;
import org.junit.Test;
import org.tmatesoft.svn.core.SVNDirEntry;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.BasicAuthenticationManager;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.auth.SVNAuthentication;
import org.tmatesoft.svn.core.auth.SVNSSHAuthentication;
import org.tmatesoft.svn.core.internal.io.svn.SVNSSHConnector;
import org.tmatesoft.svn.core.internal.io.svn.ssh.SessionPoolFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Tests that the connection pool works with connections that have timed out while being idle.
 */
public class PoolTimeoutTest {
    public static final String ROOT_URL_OF_LOCAL_SERVER = "svn+ssh://localhost/c.acmecorp";
    public static final int PORT_NUMBER = 2200;
//    public static final String ROOT_URL_OF_LOCAL_SERVER = "svn+ssh://10.0.50.11/c.acmecorp";
//    public static final int PORT_NUMBER = 22;

    @Test
    public void testTrileadReuseWithPing() throws SVNException, ExecutionException, InterruptedException {
        connectionReuse(SessionPoolFactory.TRILEAD, "true");
    }

    @Test
    public void testApacheReuseWithPing() throws SVNException, ExecutionException, InterruptedException {
        connectionReuse(SessionPoolFactory.APACHE, "true");
    }

    @Test
    public void testTrileadReuseWithoutPing() throws SVNException, ExecutionException, InterruptedException {
        connectionReuse(SessionPoolFactory.TRILEAD, "false");
    }

    @Test
    public void testApacheReuseWithoutPing() throws SVNException, ExecutionException, InterruptedException {
        connectionReuse(SessionPoolFactory.APACHE, "false");
    }

    private void connectionReuse(String implementationName, String enablePing) throws SVNException, ExecutionException, InterruptedException {
        final String rootUrl = ROOT_URL_OF_LOCAL_SERVER;
        final String keyName = "id_rsa";

        System.err.println("Testing ssh connection reuse with " + implementationName + " against " + rootUrl);
        System.setProperty(SessionPoolFactory.SVNKIT_SSH_CLIENT, implementationName);
        System.setProperty("svnkit.ssh2.ping", enablePing);
        SVNSSHConnector.recreateSessionPoolForTest();

        SVNURL baseUrl = SVNURL.parseURIDecoded(rootUrl);
        File keyFile = new File(System.getProperty("user.home") + "/.ssh/" + keyName);
        SVNSSHAuthentication sshCredentials = SVNSSHAuthentication.newInstance(System.getProperty("user.name"), keyFile, null, PORT_NUMBER, true, baseUrl, false);
        ISVNAuthenticationManager authManager = new BasicAuthenticationManager(new SVNAuthentication[]{sshCredentials});

        final SVNClientManager svnClientManager = SVNClientManager.newInstance(SVNWCUtil.createDefaultOptions(true), authManager);
        try {
            List<SVNDirEntry> entries = new ArrayList<>();
            svnClientManager.getLogClient().doList(baseUrl, SVNRevision.HEAD, SVNRevision.HEAD, false, false, entries::add);

            Assert.assertTrue("The number of entries is too low: " + entries.size(), entries.size() > 5);

            System.out.println("Done with first request, sleeping");
            Thread.sleep(2000); // Allow the server to time out the ssh connection

            System.out.println("Started second request");
            List<SVNDirEntry> entries2 = new ArrayList<>();
            svnClientManager.getLogClient().doList(baseUrl, SVNRevision.HEAD, SVNRevision.HEAD, false, false, entries2::add);

            Assert.assertTrue("The number of entries is too low: " + entries2.size(), entries2.size() > 5);
        } finally {
            svnClientManager.dispose();
        }
    }
}

