package org.tmatesoft.svn.core.wc;

import junit.framework.TestCase;
import org.junit.Assert;
import org.tmatesoft.svn.core.SVNDirEntry;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.BasicAuthenticationManager;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.auth.SVNAuthentication;
import org.tmatesoft.svn.core.auth.SVNSSHAuthentication;
import org.tmatesoft.svn.core.internal.wc.DefaultSVNAuthenticationManager;
import org.tmatesoft.svn.core.wc2.SvnList;
import org.tmatesoft.svn.core.wc2.SvnOperationFactory;
import org.tmatesoft.svn.core.wc2.SvnTarget;

import java.io.File;
import java.util.Collection;

/**
 *
 */
public class SVNClientManagerTest extends TestCase {
    public void testSsh() throws SVNException {
        String rootUrl = "svn+ssh://staging.subversion.stibo.dk/c.acmecorp";
        SVNURL baseUrl = SVNURL.parseURIDecoded(rootUrl);
        SvnOperationFactory operationFactory = new SvnOperationFactory();

        File keyFile = new File("/home/ff/.ssh/id_rsa");
        SVNSSHAuthentication sshCredentials = SVNSSHAuthentication.newInstance("nobody", keyFile, null, 22, true, baseUrl, false);
        ISVNAuthenticationManager authManager = new BasicAuthenticationManager(new SVNAuthentication[]{sshCredentials});
        operationFactory.setAuthenticationManager(authManager);

        final SVNClientManager svnClientManager = SVNClientManager.newInstance(operationFactory);

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
}