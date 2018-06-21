package org.tmatesoft.svn.test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.core.internal.wc2.ng.SvnDiffGenerator;
import org.tmatesoft.svn.core.wc.SVNEvent;
import org.tmatesoft.svn.core.wc.SVNEventAction;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc2.SvnDiff;
import org.tmatesoft.svn.core.wc2.SvnOperationFactory;
import org.tmatesoft.svn.core.wc2.SvnPatch;
import org.tmatesoft.svn.core.wc2.SvnTarget;

public class PatchTest {

    @Test
    public void testModification() throws Exception {
        final TestOptions options = TestOptions.getInstance();

        final SvnOperationFactory svnOperationFactory = new SvnOperationFactory();
        final Sandbox sandbox = Sandbox.createWithCleanup(getTestName() + ".testModification", options);
        try {
            final SVNURL url = sandbox.createSvnRepository();

            final CommitBuilder commitBuilder1 = new CommitBuilder(url);
            commitBuilder1.addFile("file", "old".getBytes());
            commitBuilder1.commit();

            final CommitBuilder commitBuilder2 = new CommitBuilder(url);
            commitBuilder2.changeFile("file", "new".getBytes());
            commitBuilder2.commit();

            final SvnDiffGenerator diffGenerator = new SvnDiffGenerator();
            diffGenerator.setBasePath(new File("").getAbsoluteFile());
            final ByteArrayOutputStream output = new ByteArrayOutputStream();

            final SvnDiff diff = svnOperationFactory.createDiff();
            diff.setSource(SvnTarget.fromURL(url), SVNRevision.create(1), SVNRevision.create(2));
            diff.setDiffGenerator(diffGenerator);
            diff.setOutput(output);
            diff.run();

            final String patchString = output.toString();

            final WorkingCopy workingCopy = sandbox.checkoutNewWorkingCopy(url, 1);
            final File workingCopyDirectory = workingCopy.getWorkingCopyDirectory();
            final File file = workingCopy.getFile("file");
            final File patchFile = new File(sandbox.createDirectory("directory"), "patchFile");

            TestUtil.writeFileContentsString(patchFile, patchString);

            final UpdateTest.EventsHandler eventHandler = new UpdateTest.EventsHandler();
            svnOperationFactory.setEventHandler(eventHandler);

            final SvnPatch patch = svnOperationFactory.createPatch();
            patch.setSingleTarget(SvnTarget.fromFile(workingCopyDirectory));
            patch.setPatchFile(patchFile);
            patch.run();

            Assert.assertEquals("new", TestUtil.readFileContentsString(file));

            final List<SVNEvent> events = eventHandler.getEvents();
            for (SVNEvent event : events) {
                System.out.println("event = " + event);
            }
            Assert.assertEquals(2, events.size());
            Assert.assertEquals(SVNEventAction.PATCH, events.get(0).getAction());
            Assert.assertEquals(file, events.get(0).getFile());
            Assert.assertEquals(SVNEventAction.PATCH_APPLIED_HUNK, events.get(1).getAction());
            Assert.assertEquals(file, events.get(1).getFile());
        } finally {
            svnOperationFactory.dispose();
            sandbox.dispose();
        }
    }

    @Test
    public void testAddition() throws Exception {
        final TestOptions options = TestOptions.getInstance();

        final SvnOperationFactory svnOperationFactory = new SvnOperationFactory();
        final Sandbox sandbox = Sandbox.createWithCleanup(getTestName() + ".testAddition", options);
        try {
            final SVNURL url = sandbox.createSvnRepository();

            final CommitBuilder commitBuilder1 = new CommitBuilder(url);
            commitBuilder1.addFile("file", "new".getBytes());
            commitBuilder1.commit();

            final SvnDiffGenerator diffGenerator = new SvnDiffGenerator();
            diffGenerator.setBasePath(new File("").getAbsoluteFile());
            final ByteArrayOutputStream output = new ByteArrayOutputStream();

            final SvnDiff diff = svnOperationFactory.createDiff();
            diff.setSource(SvnTarget.fromURL(url), SVNRevision.create(0), SVNRevision.create(1));
            diff.setDiffGenerator(diffGenerator);
            diff.setOutput(output);
            diff.run();

            final String patchString = output.toString();

            final WorkingCopy workingCopy = sandbox.checkoutNewWorkingCopy(url, 0);
            final File workingCopyDirectory = workingCopy.getWorkingCopyDirectory();
            final File file = workingCopy.getFile("file");
            final File patchFile = new File(sandbox.createDirectory("directory"), "patchFile");

            TestUtil.writeFileContentsString(patchFile, patchString);

            final UpdateTest.EventsHandler eventHandler = new UpdateTest.EventsHandler();
            svnOperationFactory.setEventHandler(eventHandler);

            final SvnPatch patch = svnOperationFactory.createPatch();
            patch.setSingleTarget(SvnTarget.fromFile(workingCopyDirectory));
            patch.setPatchFile(patchFile);
            patch.run();

            Assert.assertEquals("new", TestUtil.readFileContentsString(file));

            final List<SVNEvent> events = eventHandler.getEvents();
            Assert.assertEquals(1, events.size());
            Assert.assertEquals(SVNEventAction.ADD, events.get(0).getAction());
            Assert.assertEquals(file, events.get(0).getFile());
        } finally {
            svnOperationFactory.dispose();
            sandbox.dispose();
        }
    }

    @Test
    public void testDeletion() throws Exception {
        final TestOptions options = TestOptions.getInstance();

        final SvnOperationFactory svnOperationFactory = new SvnOperationFactory();
        final Sandbox sandbox = Sandbox.createWithCleanup(getTestName() + ".testDeletion", options);
        try {
            final SVNURL url = sandbox.createSvnRepository();

            final CommitBuilder commitBuilder1 = new CommitBuilder(url);
            commitBuilder1.addFile("file", "new".getBytes());
            commitBuilder1.commit();

            final CommitBuilder commitBuilder2 = new CommitBuilder(url);
            commitBuilder2.delete("file");
            commitBuilder2.commit();

            final SvnDiffGenerator diffGenerator = new SvnDiffGenerator();
            diffGenerator.setBasePath(new File("").getAbsoluteFile());
            final ByteArrayOutputStream output = new ByteArrayOutputStream();

            final SvnDiff diff = svnOperationFactory.createDiff();
            diff.setSource(SvnTarget.fromURL(url), SVNRevision.create(1), SVNRevision.create(2));
            diff.setDiffGenerator(diffGenerator);
            diff.setOutput(output);
            diff.run();

            final String patchString = output.toString();

            final WorkingCopy workingCopy = sandbox.checkoutNewWorkingCopy(url, 1);
            final File workingCopyDirectory = workingCopy.getWorkingCopyDirectory();
            final File file = workingCopy.getFile("file");
            final File patchFile = new File(sandbox.createDirectory("directory"), "patchFile");

            TestUtil.writeFileContentsString(patchFile, patchString);

            final UpdateTest.EventsHandler eventHandler = new UpdateTest.EventsHandler();
            svnOperationFactory.setEventHandler(eventHandler);

            final SvnPatch patch = svnOperationFactory.createPatch();
            patch.setSingleTarget(SvnTarget.fromFile(workingCopyDirectory));
            patch.setPatchFile(patchFile);
            patch.run();

            Assert.assertFalse(file.exists());

            final List<SVNEvent> events = eventHandler.getEvents();
            Assert.assertEquals(1, events.size());
            Assert.assertEquals(SVNEventAction.DELETE, events.get(0).getAction());
            Assert.assertEquals(file, events.get(0).getFile());
        } finally {
            svnOperationFactory.dispose();
            sandbox.dispose();
        }
    }

    @Test
    public void testMovement() throws Exception {
        final TestOptions options = TestOptions.getInstance();

        final SvnOperationFactory svnOperationFactory = new SvnOperationFactory();
        final Sandbox sandbox = Sandbox.createWithCleanup(getTestName() + ".testMovement", options);
        try {
            final SVNURL url = sandbox.createSvnRepository();

            final CommitBuilder commitBuilder1 = new CommitBuilder(url);
            commitBuilder1.addFile("file", "new".getBytes());
            commitBuilder1.commit();

            final CommitBuilder commitBuilder2 = new CommitBuilder(url);
            commitBuilder2.delete("file");
            commitBuilder2.addFileByCopying("movedFile", "file");
            commitBuilder2.commit();

            final SvnDiffGenerator diffGenerator = new SvnDiffGenerator();
            diffGenerator.setBasePath(new File("").getAbsoluteFile());
            final ByteArrayOutputStream output = new ByteArrayOutputStream();

            final SvnDiff diff = svnOperationFactory.createDiff();
            diff.setSource(SvnTarget.fromURL(url), SVNRevision.create(1), SVNRevision.create(2));
            diff.setDiffGenerator(diffGenerator);
            diff.setOutput(output);
            diff.run();

            final String patchString = output.toString();

            final WorkingCopy workingCopy = sandbox.checkoutNewWorkingCopy(url, 1);
            final File workingCopyDirectory = workingCopy.getWorkingCopyDirectory();
            final File file = workingCopy.getFile("file");
            final File movedFile = workingCopy.getFile("movedFile");
            final File patchFile = new File(sandbox.createDirectory("directory"), "patchFile");

            TestUtil.writeFileContentsString(patchFile, patchString);

            final UpdateTest.EventsHandler eventHandler = new UpdateTest.EventsHandler();
            svnOperationFactory.setEventHandler(eventHandler);

            final SvnPatch patch = svnOperationFactory.createPatch();
            patch.setSingleTarget(SvnTarget.fromFile(workingCopyDirectory));
            patch.setPatchFile(patchFile);
            patch.run();

            Assert.assertFalse(file.exists());
            Assert.assertEquals("new", TestUtil.readFileContentsString(movedFile));

            final List<SVNEvent> events = eventHandler.getEvents();
            Assert.assertEquals(2, events.size());
            Assert.assertEquals(SVNEventAction.DELETE, events.get(0).getAction());
            Assert.assertEquals(file, events.get(0).getFile());
            Assert.assertEquals(SVNEventAction.ADD, events.get(1).getAction());
            Assert.assertEquals(movedFile, events.get(1).getFile());
        } finally {
            svnOperationFactory.dispose();
            sandbox.dispose();
        }
    }

    @Test
    public void testPatchGitFormat() throws Exception {
        final TestOptions options = TestOptions.getInstance();
        
        final SvnOperationFactory svnOperationFactory = new SvnOperationFactory();
        final Sandbox sandbox = Sandbox.createWithCleanup(getTestName() + ".testPatchGitFormat", options);
        try {
            final SVNURL url = sandbox.createSvnRepository();

            final String oldContent = "some line";
            final String newContent = "another line";

            final CommitBuilder commitBuilder1 = new CommitBuilder(url);
            commitBuilder1.addFile("file", oldContent.getBytes());
            commitBuilder1.commit();

            final CommitBuilder commitBuilder2 = new CommitBuilder(url);
            commitBuilder2.changeFile("file", newContent.getBytes());
            commitBuilder2.commit();

            final SvnDiffGenerator diffGenerator = new SvnDiffGenerator();
            diffGenerator.setBasePath(new File("").getAbsoluteFile());
            diffGenerator.setUseGitFormat(true);
            final ByteArrayOutputStream output = new ByteArrayOutputStream();

            final SvnDiff diff = svnOperationFactory.createDiff();
            diff.setSource(SvnTarget.fromURL(url), SVNRevision.create(1), SVNRevision.create(2));
            diff.setDiffGenerator(diffGenerator);
            diff.setUseGitDiffFormat(true);
            diff.setOutput(output);
            diff.run();

            final String patchString = output.toString();

            final File directory = sandbox.createDirectory("tmp");
            final File patchFile = new File(directory, "patchFile");
            SVNFileUtil.writeToFile(patchFile, patchString, "UTF-8");

            final WorkingCopy workingCopy = sandbox.checkoutNewWorkingCopy(url, 1);
            final File workingCopyDirectory = workingCopy.getWorkingCopyDirectory();
            final SvnPatch patch = svnOperationFactory.createPatch();
            patch.setSingleTarget(SvnTarget.fromFile(workingCopyDirectory));
            patch.setPatchFile(patchFile);
            patch.run();

            final File file = workingCopy.getFile("file");
            final String fileContent = SVNFileUtil.readFile(file);

            Assert.assertEquals(newContent, fileContent);
        } finally {
            svnOperationFactory.dispose();
            sandbox.dispose();
        }
    }

    private String getTestName() {
        return getClass().getSimpleName();
    }
}
