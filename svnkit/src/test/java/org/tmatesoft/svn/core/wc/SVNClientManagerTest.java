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
import org.tmatesoft.svn.core.internal.io.svn.ssh.SshSessionPool;
import org.tmatesoft.svn.core.internal.io.svn.ssh.apache.ApacheSshSession;
import org.tmatesoft.svn.core.internal.io.svn.ssh.apache.ApacheSshSessionPool;
import org.tmatesoft.svn.core.internal.io.svn.ssh.trilead.TrileadSshSessionPool;
import org.tmatesoft.svn.core.wc2.SvnList;
import org.tmatesoft.svn.core.wc2.SvnOperationFactory;
import org.tmatesoft.svn.core.wc2.SvnTarget;

import java.io.File;
import java.util.Collection;

public class SVNClientManagerTest {
    // This server has sha1 based KEX algos enabled:
    public static final String ROOT_URL_OF_LAX_SERVER = "svn+ssh://subversion.stibo.dk/c.acmecorp";

    // This one only has the secure KEX algos:
    public static final String ROOT_URL_OF_STRICT_SERVER = "svn+ssh://staging.subversion.stibo.dk/c.acmecorp";

    @Test
    public void testApacheSshStrict() throws SVNException {
        final int execBefore = ApacheSshSession.getExecCount();
        testSsh(SessionPoolFactory.APACHE, ROOT_URL_OF_STRICT_SERVER);
        final int execAfter = ApacheSshSession.getExecCount();
        Assert.assertEquals(1, execAfter-execBefore);
    }

    @Test // This test always fails because trilead-ssh2 can't handle the strict server
    public void testTrileadSshStrict() throws SVNException {
        testSsh(SessionPoolFactory.TRILEAD, ROOT_URL_OF_STRICT_SERVER);
    }

    @Test
    public void testApacheSsh() throws SVNException {
        final int execBefore = ApacheSshSession.getExecCount();
        testSsh(SessionPoolFactory.APACHE, ROOT_URL_OF_LAX_SERVER);
        final int execAfter = ApacheSshSession.getExecCount();
        Assert.assertEquals(1, execAfter-execBefore);
    }

    @Test
    public void testTrileadSsh() throws SVNException {
        testSsh(SessionPoolFactory.TRILEAD, ROOT_URL_OF_LAX_SERVER);
    }

    private void testSsh(String implementationName, String rootUrl) throws SVNException {
        System.err.println("Testing ssh with " + implementationName + " against " + rootUrl);
        System.setProperty(SessionPoolFactory.SVNKIT_SSH_CLIENT, implementationName);
        SVNSSHConnector.recreateSessionPoolForTest();

        final SshSessionPool sshSessionPool = SessionPoolFactory.create();
        if (implementationName.equals(SessionPoolFactory.APACHE)) {
            Assert.assertTrue(sshSessionPool instanceof ApacheSshSessionPool);
        } else {
            Assert.assertTrue(sshSessionPool instanceof TrileadSshSessionPool);
        }
        sshSessionPool.shutdown();

        SVNURL baseUrl = SVNURL.parseURIDecoded(rootUrl);
        SvnOperationFactory operationFactory = new SvnOperationFactory();

        File keyFile = new File(System.getProperty("user.home") + "/.ssh/id_ed25519");
        SVNSSHAuthentication sshCredentials = SVNSSHAuthentication.newInstance("nobody", keyFile, null, 22, true, baseUrl, false);
        ISVNAuthenticationManager authManager = new BasicAuthenticationManager(new SVNAuthentication[]{sshCredentials});
        operationFactory.setAuthenticationManager(authManager);

        final SVNClientManager svnClientManager = SVNClientManager.newInstance(operationFactory);

        try {
            for (int i = 0; i < 10; i++) {

                final SvnList list = svnClientManager.getOperationFactory().createList();

                list.addTarget(SvnTarget.fromURL(baseUrl, SVNRevision.HEAD));
                list.setRevision(SVNRevision.HEAD);
                list.setIgnoreExternals(true);
                final Collection<SVNDirEntry> entries = list.run(null);

                Assert.assertTrue(entries.size() > 5);

                for (SVNDirEntry entry : entries) {
                    System.out.println(entry.getName());
                }
            }
        } finally {
            System.clearProperty(SessionPoolFactory.SVNKIT_SSH_CLIENT);
        }

        svnClientManager.dispose();
    }
}