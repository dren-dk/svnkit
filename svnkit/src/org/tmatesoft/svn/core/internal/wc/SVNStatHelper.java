/*
 * ====================================================================
 * Copyright (c) 2004-2007 TMate Software Ltd.  All rights reserved.
 *
 * This software is licensed as described in the file COPYING, which
 * you should have received as part of this distribution.  The terms
 * are also available at http://svnkit.com/license.html.
 * If newer versions of this license are posted there, you may use a
 * newer version instead, at your option.
 * ====================================================================
 */
package org.tmatesoft.svn.core.internal.wc;

import java.io.File;

import org.tmatesoft.svn.util.SVNDebugLog;


/**
 * @version 1.1.2
 * @author  TMate Software Ltd.
 */
public class SVNStatHelper {

    private static boolean ourIsLibraryLoaded = false;

    static {
        try {
            System.loadLibrary("jsvnstat32");
            ourIsLibraryLoaded = true;
        } catch (Throwable th) {
            try {
                System.loadLibrary("jsvnstat64");
                ourIsLibraryLoaded = true;
            } catch (Throwable th2) {
            }
        }
    }

    public static SVNFileType getType(File path, boolean discoverLinks) {
        if (ourIsLibraryLoaded) {
            int type = getType(path.getAbsolutePath(), discoverLinks);
            switch (type) {
                case 1:
                    return SVNFileType.FILE; 
                case 2:
                    return SVNFileType.DIRECTORY; 
                case 3:
                    return SVNFileType.SYMLINK; 
                case 4:
                    return SVNFileType.UNKNOWN; 
                default:
                    return SVNFileType.NONE;
            }
        } 
        String line = null;
        try {
            line = SVNFileUtil.execCommand(new String[] {SVNFileUtil.LS_COMMAND, "-ld", path.getAbsolutePath()});
        } catch (Throwable th) {
            SVNDebugLog.getDefaultLog().info(th);
        }
        if (line != null) {
            if (line.startsWith("l")) {
                return SVNFileType.SYMLINK;
            } else if (line.startsWith("d")) {
                return SVNFileType.DIRECTORY;
            } else if (line.startsWith("-")) {
                return SVNFileType.FILE;
            }
        }
        return SVNFileType.NONE;
    }
    
    /**
     * return values:
     *   1 - ordinar file
     *   2 - directory
     *   3 - symlink
     *   4 - unknown
     *  -1 - no such path or error occurres during system call 
     * 
     */
    private static native int getType(String path, boolean findLinks);
}
