/*
 * ====================================================================
 * Copyright (c) 2004-2012 TMate Software Ltd.  All rights reserved.
 *
 * This software is licensed as described in the file COPYING, which
 * you should have received as part of this distribution.  The terms
 * are also available at http://svnkit.com/license.html.
 * If newer versions of this license are posted there, you may use a
 * newer version instead, at your option.
 * ====================================================================
 */
package org.tmatesoft.svn.cli;


import java.io.File;
import java.io.IOException;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.util.SVNDoubleLock;
import org.tmatesoft.svn.core.internal.util.jna.SVNJNAUtil;

/**
 * @version 1.3
 * @author  TMate Software Ltd.
 */
public class SVN {

    public static void main(String[] args) {
        try {
            run();
        } catch (SVNException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void run() throws SVNException, IOException {
        SVNJNAUtil.setJNAEnabled(false);

        final File file = new File("/tmp/file");

        System.out.println("Obtaining lock");
        final SVNDoubleLock lock = SVNDoubleLock.obtain(file, true);
        System.out.println("lock = " + lock);
        lock.release();
        System.out.println("lock = " + lock);


        while (true) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
