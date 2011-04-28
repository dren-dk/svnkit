package org.tmatesoft.svn.core.test;

import org.tmatesoft.svn.cli.svn.SVN;
import org.tmatesoft.svn.cli.svnadmin.SVNAdmin;
import org.tmatesoft.svn.cli.svndumpfilter.SVNDumpFilter;
import org.tmatesoft.svn.cli.svnlook.SVNLook;
import org.tmatesoft.svn.cli.svnsync.SVNSync;
import org.tmatesoft.svn.cli.svnversion.SVNVersion;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;

import com.martiansoftware.nailgun.NGContext;

public class NailgunProcessor {

    public static void nailMain(NGContext context) {
        String programName = context.getArgs()[0];
        String[] programArgs = new String[context.getArgs().length - 1];
        System.arraycopy(context.getArgs(), 1, programArgs, 0,
                programArgs.length);

        configureEnvironment(context);

        if ("svn".equals(programName)) {
            SVN.main(programArgs);
        } else if ("svnadmin".equals(programName)) {
            SVNAdmin.main(programArgs);
        } else if ("svnlook".equals(programName)) {
            SVNLook.main(programArgs);
        } else if ("svnversion".equals(programName)) {
            SVNVersion.main(programArgs);
        } else if ("svnsync".equals(programName)) {
            SVNSync.main(programArgs);
        } else if ("svndumpfilter".equals(programName)) {
            SVNDumpFilter.main(programArgs);
        }
    }

    private static void configureEnvironment(NGContext context) {
        String editor = context.getEnv().getProperty("SVN_EDITOR");
        String mergeTool = context.getEnv().getProperty("SVN_MERGE");
        String editorFunction = context.getEnv().getProperty("SVNTEST_EDITOR_FUNC");

        SVNFileUtil.setTestEnvironment(editor, mergeTool, editorFunction);
        SVNFileUtil.setSleepForTimestamp(false);
        System.setProperty("user.dir", context.getWorkingDirectory());
    }
}
