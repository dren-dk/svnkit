package org.tmatesoft.svn.test;

import java.io.File;
import java.util.Map;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.tmatesoft.svn.core.SVNCommitInfo;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.SVNLogEntryPath;
import org.tmatesoft.svn.core.SVNProperty;
import org.tmatesoft.svn.core.SVNPropertyValue;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.wc.SVNExternal;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.core.internal.wc17.SVNWCContext;
import org.tmatesoft.svn.core.internal.wc17.db.Structure;
import org.tmatesoft.svn.core.internal.wc17.db.StructureFields;
import org.tmatesoft.svn.core.internal.wc2.SvnWcGeneration;
import org.tmatesoft.svn.core.io.ISVNEditor;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.wc.DefaultSVNCommitHandler;
import org.tmatesoft.svn.core.wc.DefaultSVNRepositoryPool;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNCommitClient;
import org.tmatesoft.svn.core.wc.SVNCommitItem;
import org.tmatesoft.svn.core.wc.SVNCommitPacket;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNStatusType;
import org.tmatesoft.svn.core.wc2.SvnCheckout;
import org.tmatesoft.svn.core.wc2.SvnCommit;
import org.tmatesoft.svn.core.wc2.SvnLog;
import org.tmatesoft.svn.core.wc2.SvnOperationFactory;
import org.tmatesoft.svn.core.wc2.SvnRevisionRange;
import org.tmatesoft.svn.core.wc2.SvnScheduleForAddition;
import org.tmatesoft.svn.core.wc2.SvnStatus;
import org.tmatesoft.svn.core.wc2.SvnTarget;
import org.tmatesoft.svn.core.wc2.admin.SvnRepositoryCreate;

public class CommitTest {
    @Test
    public void testFileIsChangedToEmpty() throws SVNException {
        final TestOptions options = TestOptions.getInstance();

        final SvnOperationFactory svnOperationFactory = new SvnOperationFactory();
        final Sandbox sandbox = Sandbox.createWithCleanup(getTestName() + ".testFileIsChangedToEmpty", options);
        try {
            final SVNURL url = sandbox.createSvnRepository();

            final CommitBuilder commitBuilder = new CommitBuilder(url);
            commitBuilder.addFile("file", "originalContents".getBytes());
            commitBuilder.commit();

            final WorkingCopy workingCopy = sandbox.checkoutNewWorkingCopy(url, SVNRevision.HEAD.getNumber());
            final File workingCopyDirectory = workingCopy.getWorkingCopyDirectory();

            final File file = new File(workingCopyDirectory, "file");

            final String emptyString = "";
            TestUtil.writeFileContentsString(file, emptyString);

            final SvnCommit commit = svnOperationFactory.createCommit();
            commit.setSingleTarget(SvnTarget.fromFile(workingCopyDirectory));
            commit.setCommitMessage("File contents is changed to empty");
            final SVNCommitInfo commitInfo = commit.run();

            Assert.assertEquals(2, commitInfo.getNewRevision());
        } finally {
            svnOperationFactory.dispose();
            sandbox.dispose();
        }
    }
    
    @Test
    public void testCommitFromExternals16() throws SVNException {
        final SvnOperationFactory svnOperationFactory = new SvnOperationFactory();
        final Sandbox sandbox = Sandbox.createWithCleanup(getTestName() + ".testCommitFromExternals", TestOptions.getInstance());
        try {
            final SVNURL url1 = sandbox.createSvnRepository();
            final SVNURL url2 = sandbox.createSvnRepository();

            final CommitBuilder commitBuilder1 = new CommitBuilder(url1);
            commitBuilder1.addFile("trunk/file", "originalContents".getBytes());
            commitBuilder1.setDirectoryProperty("trunk", "svn:externals", SVNPropertyValue.create("ext " + url2.appendPath("trunk/dir", false)));
            commitBuilder1.commit();

            final CommitBuilder commitBuilder2 = new CommitBuilder(url2);
            commitBuilder2.addFile("trunk/dir/file", "originalContents".getBytes());
            
            commitBuilder2.commit();

            final WorkingCopy workingCopy = sandbox.checkoutNewWorkingCopy(url1, SVNRevision.HEAD.getNumber(), false, SvnWcGeneration.V16);
            workingCopy.changeFileContents("trunk/ext/file", "modified");
            
            final File path = workingCopy.getFile("trunk/ext/file");
            SVNCommitClient ci = SVNClientManager.newInstance().getCommitClient();
            final SVNCommitInfo info = ci.doCommit(new File[]{path},
                    false,
                    "message", null, null, false, true, SVNDepth.INFINITY);
            Assert.assertNotNull(info);
            Assert.assertEquals(2, info.getNewRevision());

            final SVNCommitInfo info2 = ci.doCommit(new File[]{path},
                    false,
                    "message", null, null, false, true, SVNDepth.INFINITY);
            Assert.assertNotNull(info2);
            Assert.assertEquals(-1, info2.getNewRevision());
        } finally {
            svnOperationFactory.dispose();
            sandbox.dispose();
        }
    }

    @Test
    public void testCommitIncompleteDirectory() throws Exception {
        final TestOptions options = TestOptions.getInstance();

        Assume.assumeTrue(TestUtil.isNewWorkingCopyTest());

        final SvnOperationFactory svnOperationFactory = new SvnOperationFactory();
        final Sandbox sandbox = Sandbox.createWithCleanup(getTestName() + ".testCommitIncompleteDirectory", options);
        try {
            final SVNURL url = sandbox.createSvnRepository();

            final CommitBuilder commitBuilder = new CommitBuilder(url);
            commitBuilder.addFile("directory/file");
            commitBuilder.commit();

            final WorkingCopy workingCopy = sandbox.checkoutNewWorkingCopy(url);
            final File workingCopyDirectory = workingCopy.getWorkingCopyDirectory();

            final File directory = new File(workingCopyDirectory, "directory");

            workingCopy.setProperty(directory, "propertyName", SVNPropertyValue.create("propertyValue"));

            setIncomplete(svnOperationFactory, directory, 1, null);

            final SvnCommit commit = svnOperationFactory.createCommit();
            commit.setSingleTarget(SvnTarget.fromFile(directory));
            final SVNCommitInfo commitInfo = commit.run();

            Assert.assertEquals(2, commitInfo.getNewRevision());

            final Map<File,SvnStatus> statuses = TestUtil.getStatuses(svnOperationFactory, workingCopyDirectory);
            final SvnStatus status = statuses.get(directory);
            Assert.assertEquals(SVNStatusType.STATUS_INCOMPLETE, status.getNodeStatus());
            Assert.assertEquals(2, status.getRevision());

        } finally {
            svnOperationFactory.dispose();
            sandbox.dispose();
        }
   }

    @Test
    public void testCommitNoChanges() throws Exception {
        final TestOptions options = TestOptions.getInstance();

        final SvnOperationFactory svnOperationFactory = new SvnOperationFactory();
        final Sandbox sandbox = Sandbox.createWithCleanup(getTestName() + ".testCommitNoChanges", options);
        try {
            final SVNURL url = sandbox.createSvnRepository();

            final CommitBuilder commitBuilder = new CommitBuilder(url);
            commitBuilder.addFile("directory/file");
            commitBuilder.commit();

            final WorkingCopy workingCopy = sandbox.checkoutNewWorkingCopy(url);
            final File workingCopyDirectory = workingCopy.getWorkingCopyDirectory();

            final SvnCommit commit = svnOperationFactory.createCommit();
            commit.setSingleTarget(SvnTarget.fromFile(workingCopyDirectory));
            final SVNCommitInfo commitInfo = commit.run();

            Assert.assertEquals(SVNCommitInfo.NULL, commitInfo);

        } finally {
            svnOperationFactory.dispose();
            sandbox.dispose();
        }
    }

    @Test
    public void testCommitMessageContainsCRLF() throws Exception {
        final TestOptions options = TestOptions.getInstance();

        final SvnOperationFactory svnOperationFactory = new SvnOperationFactory();
        final Sandbox sandbox = Sandbox.createWithCleanup(getTestName() + ".testCommitMessageContainsCRLF", options);
        try {
            final SVNURL url = sandbox.createSvnRepository();

            final WorkingCopy workingCopy = sandbox.checkoutNewWorkingCopy(url);
            final File workingCopyDirectory = workingCopy.getWorkingCopyDirectory();

            final File file = workingCopy.getFile("file");
            TestUtil.writeFileContentsString(file, "contents");
            workingCopy.add(file);

            final String commitMessage = "Commit message with " + "\r\n" + "CRLF";

            final SvnCommit commit = svnOperationFactory.createCommit();
            commit.setCommitMessage(commitMessage);
            commit.setSingleTarget(SvnTarget.fromFile(workingCopyDirectory));
            final SVNCommitInfo commitInfo = commit.run();

            workingCopy.updateToRevision(commitInfo.getNewRevision());

            final SvnLog log = svnOperationFactory.createLog();
            log.addRange(SvnRevisionRange.create(SVNRevision.create(commitInfo.getNewRevision()), SVNRevision.create(commitInfo.getNewRevision())));
            log.setSingleTarget(SvnTarget.fromFile(workingCopyDirectory));
            final SVNLogEntry logEntry = log.run();

            Assert.assertEquals(commitMessage.replace("\r\n", "\n"), logEntry.getMessage());

        } finally {
            svnOperationFactory.dispose();
            sandbox.dispose();
        }
    }

    @Test
    public void testURLsAreNotEncodedTwiceForAddedFiles() throws Exception {
        //SVNKIT-282
        final TestOptions options = TestOptions.getInstance();

        final SvnOperationFactory svnOperationFactory = new SvnOperationFactory();
        final Sandbox sandbox = Sandbox.createWithCleanup(getTestName() + ".testURLsAreNotEncodedTwiceForAddedFiles", options);
        try {
            //prepare a repository with a space in the URL
            final File repositoryDirectory = sandbox.createDirectory("svn.repo with space");

            final SvnRepositoryCreate repositoryCreate = svnOperationFactory.createRepositoryCreate();
            repositoryCreate.setRepositoryRoot(repositoryDirectory);
            final SVNURL url = repositoryCreate.run();

            final WorkingCopy workingCopy = sandbox.checkoutNewWorkingCopy(url);

            //add a file locally
            final File file = workingCopy.getFile("file");
            TestUtil.writeFileContentsString(file, "contents");
            workingCopy.add(file);

            //commit that file
            final SvnCommit commit = svnOperationFactory.createCommit();
            commit.setSingleTarget(SvnTarget.fromFile(file));
            commit.run();

        } finally {
            svnOperationFactory.dispose();
            sandbox.dispose();
        }
    }

    @Test
    public void testSkipCommitItem() throws Exception {
        //SVNKIT-334
        final TestOptions options = TestOptions.getInstance();

        final SvnOperationFactory svnOperationFactory = new SvnOperationFactory();
        final Sandbox sandbox = Sandbox.createWithCleanup(getTestName() + ".testSkipCommitItem", options);
        try {
            final SVNURL url = sandbox.createSvnRepository();

            final WorkingCopy workingCopy = sandbox.checkoutNewWorkingCopy(url);

            final File directory = workingCopy.getFile("directory");
            final File file1 = new File(directory, "file1");
            final File file2 = new File(directory, "file2");

            SVNFileUtil.ensureDirectoryExists(directory);
            TestUtil.writeFileContentsString(file1, "contents");
            TestUtil.writeFileContentsString(file2, "contents");

            final SvnScheduleForAddition scheduleForAddition = svnOperationFactory.createScheduleForAddition();
            scheduleForAddition.addTarget(SvnTarget.fromFile(directory));
            scheduleForAddition.addTarget(SvnTarget.fromFile(file1));
            scheduleForAddition.addTarget(SvnTarget.fromFile(file2));
            scheduleForAddition.run();

            final SVNClientManager clientManager = SVNClientManager.newInstance();
            try {
                final SVNCommitClient commitClient = clientManager.getCommitClient();
                commitClient.setCommitHandler(new DefaultSVNCommitHandler());
                final SVNCommitPacket commitPacket = commitClient.doCollectCommitItems(new File[]{workingCopy.getWorkingCopyDirectory()}, false, true, SVNDepth.INFINITY, null);
                for (SVNCommitItem commitItem : commitPacket.getCommitItems()) {
                    if (commitItem.getFile().equals(file2)) {
                        commitPacket.setCommitItemSkipped(commitItem, true);
                    }
                }
                commitClient.doCommit(commitPacket, true, "");
            } finally {
                clientManager.dispose();
            }

            final SvnLog log = svnOperationFactory.createLog();
            log.addRange(SvnRevisionRange.create(SVNRevision.create(1), SVNRevision.HEAD));
            log.setSingleTarget(SvnTarget.fromURL(url));
            log.setDiscoverChangedPaths(true);
            final SVNLogEntry logEntry = log.run();

            final Map<String, SVNLogEntryPath> changedPaths = logEntry.getChangedPaths();
            final SVNLogEntryPath logEntryPath = changedPaths.get("/directory/file2");
            Assert.assertNull(logEntryPath);

        } finally {
            svnOperationFactory.dispose();
            sandbox.dispose();
        }
    }

    @Test
    public void testCollectCommitItemsNotCombinedWithExternal() throws Exception {
        //SVNKIT-336
        final TestOptions options = TestOptions.getInstance();

        final SvnOperationFactory svnOperationFactory = new SvnOperationFactory();
        final Sandbox sandbox = Sandbox.createWithCleanup(getTestName() + ".testCollectCommitItemsNotCombinedWithExternal", options);
        try {
            final SVNURL url = sandbox.createSvnRepository();

            final CommitBuilder commitBuilder1 = new CommitBuilder(url);
            commitBuilder1.addFile("directory/externalFile");
            commitBuilder1.commit();

            final SVNExternal external = new SVNExternal("external", url.appendPath("directory", false).toString(), SVNRevision.HEAD, SVNRevision.HEAD, false, false, true);

            final CommitBuilder commitBuilder2 = new CommitBuilder(url);
            commitBuilder2.setDirectoryProperty("", SVNProperty.EXTERNALS, SVNPropertyValue.create(external.toString()));
            commitBuilder2.addFile("file");
            commitBuilder2.commit();

            final File workingCopyDirectory = sandbox.createDirectory("wc");

            final SvnCheckout checkout = svnOperationFactory.createCheckout();
            checkout.setSource(SvnTarget.fromURL(url));
            checkout.setSingleTarget(SvnTarget.fromFile(workingCopyDirectory));
            checkout.setIgnoreExternals(false);
            checkout.run();

            final File file = new File(workingCopyDirectory, "file");
            final File externalFile = new File(workingCopyDirectory, "external/externalFile");

            TestUtil.writeFileContentsString(file, "contents");
            TestUtil.writeFileContentsString(externalFile, "contents");

            final SVNClientManager clientManager = SVNClientManager.newInstance();
            try {
                final SVNCommitClient commitClient = clientManager.getCommitClient();
                commitClient.setCommitHandler(new DefaultSVNCommitHandler());
                final SVNCommitPacket[] commitPackets = commitClient.doCollectCommitItems(new File[]{workingCopyDirectory, externalFile}, false, false, SVNDepth.INFINITY, false, null);

                Assert.assertEquals(2, commitPackets.length);
            } finally {
                clientManager.dispose();
            }

        } finally {
            svnOperationFactory.dispose();
            sandbox.dispose();
        }
    }

    @Test
    public void testCollectCommitItemsNotCombinedDifferentRepositories() throws Exception {
        //SVNKIT-336
        final TestOptions options = TestOptions.getInstance();

        final SvnOperationFactory svnOperationFactory = new SvnOperationFactory();
        final Sandbox sandbox = Sandbox.createWithCleanup(getTestName() + ".testCollectCommitItemsNotCombinedDifferentRepositories", options);
        try {
            final SVNURL url1 = sandbox.createSvnRepository();
            final SVNURL url2 = sandbox.createSvnRepository();

            final CommitBuilder commitBuilder1 = new CommitBuilder(url1);
            commitBuilder1.addFile("directory/externalFile");
            commitBuilder1.commit();

            final SVNExternal external = new SVNExternal("external", url1.appendPath("directory", false).toString(), SVNRevision.HEAD, SVNRevision.HEAD, false, false, true);

            final CommitBuilder commitBuilder2 = new CommitBuilder(url2);
            commitBuilder2.setDirectoryProperty("", SVNProperty.EXTERNALS, SVNPropertyValue.create(external.toString()));
            commitBuilder2.addFile("file");
            commitBuilder2.commit();

            final File workingCopyDirectory = sandbox.createDirectory("wc");

            final SvnCheckout checkout = svnOperationFactory.createCheckout();
            checkout.setSource(SvnTarget.fromURL(url2));
            checkout.setSingleTarget(SvnTarget.fromFile(workingCopyDirectory));
            checkout.setIgnoreExternals(false);
            checkout.run();

            final File file = new File(workingCopyDirectory, "file");
            final File externalFile = new File(workingCopyDirectory, "external/externalFile");

            TestUtil.writeFileContentsString(file, "contents");
            TestUtil.writeFileContentsString(externalFile, "contents");

            final SVNClientManager clientManager = SVNClientManager.newInstance();
            try {
                final SVNCommitClient commitClient = clientManager.getCommitClient();
                commitClient.setCommitHandler(new DefaultSVNCommitHandler());
                final SVNCommitPacket[] commitPackets = commitClient.doCollectCommitItems(new File[]{workingCopyDirectory, externalFile}, false, false, SVNDepth.INFINITY, false, null);

                Assert.assertEquals(2, commitPackets.length);
            } finally {
                clientManager.dispose();
            }

        } finally {
            svnOperationFactory.dispose();
            sandbox.dispose();
        }
    }

    @Test
    public void testCommitAddedFilesLeavesNoModifications() throws Exception {
        final TestOptions options = TestOptions.getInstance();

        final SvnOperationFactory svnOperationFactory = new SvnOperationFactory();
        final Sandbox sandbox = Sandbox.createWithCleanup(getTestName() + ".testCommitAddedFilesLeavesNoModifications", options);
        try {
            final SVNURL url = sandbox.createSvnRepository();

            final WorkingCopy workingCopy = sandbox.checkoutNewWorkingCopy(url);
            final File workingCopyDirectory = workingCopy.getWorkingCopyDirectory();
            final File file = workingCopy.getFile("file");
            TestUtil.writeFileContentsString(file, "contents");
            workingCopy.add(file);

            final SvnCommit commit = svnOperationFactory.createCommit();
            commit.setSingleTarget(SvnTarget.fromFile(workingCopyDirectory));
            commit.run();

            final Map<File, SvnStatus> statuses = TestUtil.getStatuses(svnOperationFactory, workingCopyDirectory);
            Assert.assertEquals(SVNStatusType.STATUS_NORMAL, statuses.get(file).getNodeStatus());

        } finally {
            svnOperationFactory.dispose();
            sandbox.dispose();
        }
    }

    @Test
    public void testConflictOnRemovingNonExistentProperty() throws Exception {
        //SVNKIT-694
        final TestOptions testOptions = TestOptions.getInstance();

        final Sandbox sandbox = Sandbox.createWithCleanup(getTestName() + ".testConflictOnRemovingNonExistentProperty", testOptions);

        final SvnOperationFactory operationFactory = new SvnOperationFactory();
        try {
            final SVNURL url = sandbox.createSvnRepository();

            final CommitBuilder commitBuilder = new CommitBuilder(url);
            commitBuilder.addFile("trunk/conf/conf/file");
            commitBuilder.commit();

            final File workingCopy = sandbox.createDirectory("wc");

            final SvnCheckout checkout = operationFactory.createCheckout();
            checkout.setSingleTarget(SvnTarget.fromFile(workingCopy));
            checkout.setSource(SvnTarget.fromURL(url.appendPath("trunk/conf", false)));
            checkout.run();

            final File newFile = new File(workingCopy, "conf/newFile");
            Assert.assertTrue(newFile.createNewFile());

            final SvnScheduleForAddition scheduleForAddition = operationFactory.createScheduleForAddition();
            scheduleForAddition.setSingleTarget(SvnTarget.fromFile(newFile));
            scheduleForAddition.run();

            final DefaultSVNRepositoryPool svnRepositoryPool = new DefaultSVNRepositoryPool(null, null);
            try {
                final SVNRepository svnRepository = svnRepositoryPool.createRepository(url, true);

                final ISVNEditor commitEditor = svnRepository.getCommitEditor("", null);
                commitEditor.openRoot(1);
                commitEditor.openDir("trunk", 1);
                commitEditor.changeDirProperty("custom:lock", null);

                final SvnCommit commit = operationFactory.createCommit();
                commit.setSingleTarget(SvnTarget.fromFile(workingCopy.getAbsoluteFile()));
                commit.run();

                commitEditor.openDir("trunk/conf", 1);
                commitEditor.openDir("trunk/conf/conf", 1);
                commitEditor.addDir("trunk/conf/conf/dir", null, -1);
                commitEditor.closeDir();
                commitEditor.closeDir();
                commitEditor.closeDir();
                commitEditor.closeDir();
                commitEditor.closeDir();
                try {
                    commitEditor.closeEdit();
                    Assert.fail("An exception should be thrown");
                } catch (SVNException e) {
                    //expected
                    e.printStackTrace();
                    Assert.assertEquals(SVNErrorCode.FS_CONFLICT, e.getErrorMessage().getErrorCode());
                }
            } finally {
                svnRepositoryPool.dispose();
            }
        } finally {
            operationFactory.dispose();
            sandbox.dispose();
        }
    }
    @Test
    public void testEmptyCommitPacketWithOtherPackets() throws Exception {
        //SVNKIT-358
        final TestOptions testOptions = TestOptions.getInstance();

        final Sandbox sandbox = Sandbox.createWithCleanup(getTestName() + ".testEmptyCommitPacketWithOtherPackets", testOptions);

        final SvnOperationFactory operationFactory = new SvnOperationFactory();
        try {
            final SVNURL url = sandbox.createSvnRepository();

            final CommitBuilder commitBuilder = new CommitBuilder(url);
            commitBuilder.addFile("file1");
            commitBuilder.addFile("file2");
            commitBuilder.commit();

            final WorkingCopy workingCopy = sandbox.checkoutNewWorkingCopy(url);
            final File workingCopyDirectory = workingCopy.getWorkingCopyDirectory();

            final File file1 = workingCopy.getFile("file1");
            final File file2 = workingCopy.getFile("file2");
            TestUtil.writeFileContentsString(file1, "changed");
            TestUtil.writeFileContentsString(file2, "changed");

            final SVNClientManager clientManager = SVNClientManager.newInstance();
            try {
                final SVNCommitClient commitClient = clientManager.getCommitClient();
                final SVNCommitPacket[] commitPackets = commitClient.doCollectCommitItems(new File[]{workingCopyDirectory}, false, false, SVNDepth.INFINITY, false, new String[0]);
                Assert.assertEquals(1, commitPackets.length);

                commitClient.doCommit(new SVNCommitPacket[]{SVNCommitPacket.EMPTY, commitPackets[0]}, false, "Commit message");
            } finally {
                clientManager.dispose();
            }
            final SVNRepository svnRepository = SVNRepositoryFactory.create(url);
            try {
                final long latestRevision = svnRepository.getLatestRevision();
                Assert.assertEquals(2, latestRevision);
            } finally {
                svnRepository.closeSession();
            }
        } finally {
            operationFactory.dispose();
            sandbox.dispose();
        }
    }

    @Test
    public void testCommitEmptyPacket() throws Exception {
        //SVNKIT-358
        final SVNClientManager clientManager = SVNClientManager.newInstance();
        try {
            final SVNCommitClient commitClient = clientManager.getCommitClient();
            commitClient.doCommit(SVNCommitPacket.EMPTY, false, "Commit message");
            commitClient.doCommit(new SVNCommitPacket[]{}, false, "Commit message");
            commitClient.doCommit(new SVNCommitPacket[]{SVNCommitPacket.EMPTY}, false, "Commit message");
            commitClient.doCommit(new SVNCommitPacket[]{SVNCommitPacket.EMPTY, SVNCommitPacket.EMPTY}, false, "Commit message");
            //no exception should be thrown
        } finally {
            clientManager.dispose();
        }
    }

    private void setIncomplete(SvnOperationFactory svnOperationFactory, File path, long revision, File reposRelpath) throws SVNException {
        SVNWCContext context = new SVNWCContext(svnOperationFactory.getOptions(), svnOperationFactory.getEventHandler());
        try {
            if (reposRelpath == null) {
                final Structure<StructureFields.NodeInfo> nodeInfoStructure = context.getDb().readInfo(path, StructureFields.NodeInfo.reposRelPath);
                reposRelpath = nodeInfoStructure.get(StructureFields.NodeInfo.reposRelPath);
            }

            context.getDb().opStartDirectoryUpdateTemp(path, reposRelpath, revision);
        } finally {
            context.close();
        }
    }

    private String getTestName() {
        return "CommitTest";
    }
}