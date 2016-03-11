package org.tmatesoft.svn.test;

import org.junit.Assert;
import org.junit.Test;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNPropertyValue;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.core.internal.wc.admin.SVNAdminArea16Factory;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb;
import org.tmatesoft.svn.core.io.ISVNFileCheckoutTarget;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.wc.SVNEvent;
import org.tmatesoft.svn.core.wc.SVNEventAction;
import org.tmatesoft.svn.core.wc.SVNStatusType;
import org.tmatesoft.svn.core.wc2.*;

import java.io.File;
import java.io.OutputStream;
import java.util.HashSet;

public class CheckoutTest {
    @Test
    public void testCheckoutWC17() throws Exception {
        final TestOptions options = TestOptions.getInstance();

        final SvnOperationFactory svnOperationFactory = new SvnOperationFactory();
        final Sandbox sandbox = Sandbox.createWithCleanup(getTestName() + ".testCheckoutWC17", options);
        try {
            final SVNURL url = sandbox.createSvnRepository();

            final CommitBuilder commitBuilder = new CommitBuilder(url);
            commitBuilder.addFile("file");
            commitBuilder.commit();

            final File workingCopyDirectory = sandbox.createDirectory("wc");

            final int expectedWorkingCopyFormat = ISVNWCDb.WC_FORMAT_17;

            final SvnCheckout checkout = svnOperationFactory.createCheckout();
            checkout.setSource(SvnTarget.fromURL(url));
            checkout.setSingleTarget(SvnTarget.fromFile(workingCopyDirectory));
            checkout.setTargetWorkingCopyFormat(expectedWorkingCopyFormat);
            checkout.run();

            final SvnGetStatus getStatus = svnOperationFactory.createGetStatus();
            getStatus.setDepth(SVNDepth.EMPTY);
            getStatus.setReportAll(true);
            getStatus.setSingleTarget(SvnTarget.fromFile(workingCopyDirectory));
            final SvnStatus status = getStatus.run();

            final int actualWorkingCopyFormat = status.getWorkingCopyFormat();
            Assert.assertEquals(expectedWorkingCopyFormat, actualWorkingCopyFormat);

        } finally {
            svnOperationFactory.dispose();
            sandbox.dispose();
        }
    }

    @Test
    public void testCheckoutWC16() throws Exception {
        final TestOptions options = TestOptions.getInstance();

        final SvnOperationFactory svnOperationFactory = new SvnOperationFactory();
        final Sandbox sandbox = Sandbox.createWithCleanup(getTestName() + ".testCheckoutWC16", options);
        try {
            final SVNURL url = sandbox.createSvnRepository();

            final CommitBuilder commitBuilder = new CommitBuilder(url);
            commitBuilder.addFile("file");
            commitBuilder.commit();

            final File workingCopyDirectory = sandbox.createDirectory("wc");

            final int expectedWorkingCopyFormat = SVNAdminArea16Factory.WC_FORMAT;

            final SvnCheckout checkout = svnOperationFactory.createCheckout();
            checkout.setSource(SvnTarget.fromURL(url));
            checkout.setSingleTarget(SvnTarget.fromFile(workingCopyDirectory));
            checkout.setTargetWorkingCopyFormat(expectedWorkingCopyFormat);
            checkout.run();

            final SvnGetStatus getStatus = svnOperationFactory.createGetStatus();
            getStatus.setDepth(SVNDepth.EMPTY);
            getStatus.setReportAll(true);
            getStatus.setSingleTarget(SvnTarget.fromFile(workingCopyDirectory));
            final SvnStatus status = getStatus.run();

            final int actualWorkingCopyFormat = status.getWorkingCopyFormat();
            Assert.assertEquals(expectedWorkingCopyFormat, actualWorkingCopyFormat);

        } finally {
            svnOperationFactory.dispose();
            sandbox.dispose();
        }
    }

    @Test
    public void testCheckoutFiles() throws Exception {
        //SVNKIT-652: checkoutFiles should return all files if paths list is null or empty
        final TestOptions options = TestOptions.getInstance();

        final SvnOperationFactory svnOperationFactory = new SvnOperationFactory();
        final Sandbox sandbox = Sandbox.createWithCleanup(getTestName() + ".testCheckoutFiles", options);
        try {
            final SVNURL url = sandbox.createSvnRepository();

            final CommitBuilder commitBuilder = new CommitBuilder(url);
            commitBuilder.addFile("file");
            commitBuilder.addFile("directory/file");
            commitBuilder.commit();

            final HashSet<String> paths = new HashSet<String>(2);

            SVNRepository repository = SVNRepositoryFactory.create(url);
            repository.checkoutFiles(1, new String[0], new ISVNFileCheckoutTarget() {
                public OutputStream getOutputStream(String path) throws SVNException {
                    return SVNFileUtil.DUMMY_OUT;
                }

                public void filePropertyChanged(String path, String name, SVNPropertyValue value) throws SVNException {
                    Assert.assertTrue(path.endsWith("file"));
                    paths.add(path);
                }
            });
            Assert.assertEquals(2, paths.size());
        } finally {
            svnOperationFactory.dispose();
            sandbox.dispose();
        }
    }

    private String getTestName() {
        return getClass().getSimpleName();
    }
}
