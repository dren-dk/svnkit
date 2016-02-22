package org.tmatesoft.svn.core.javahl17;

import org.apache.subversion.javahl.ClientException;
import org.apache.subversion.javahl.ISVNReporter;
import org.apache.subversion.javahl.types.Depth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.io.ISVNReporterBaton;

import java.util.ArrayList;
import java.util.List;

public abstract class JavaHLReporter implements ISVNReporter {

    private final List<Entry> entries;

    public JavaHLReporter() {
        this.entries = new ArrayList<Entry>();
    }

    protected ISVNReporterBaton getReporter() {
        return new ISVNReporterBaton() {
            public void report(org.tmatesoft.svn.core.io.ISVNReporter reporter) throws SVNException {
                for (Entry entry : entries) {
                    entry.applyTo(reporter);
                }
            }
        };
    }

    public void setPath(String path, long revision, Depth depth, boolean startEmpty, String lockToken) throws ClientException {
        final Entry entry = new Entry();
        entry.kind = Kind.SET_PATH;
        entry.path = path;
        entry.revision = revision;
        entry.depth = depth;
        entry.startEmpty = startEmpty;
        entry.lockToken = lockToken;
        entries.add(entry);
    }

    public void deletePath(String path) throws ClientException {
        final Entry entry = new Entry();
        entry.kind = Kind.DELETE_PATH;
        entry.path = path;
        entries.add(entry);
    }

    public void linkPath(String url, String path, long revision, Depth depth, boolean startEmpty, String lockToken) throws ClientException {
        final Entry entry = new Entry();
        entry.kind = Kind.LINK_PATH;
        entry.url = url;
        entry.path = path;
        entry.revision = revision;
        entry.depth = depth;
        entry.startEmpty = startEmpty;
        entry.lockToken = lockToken;
        entries.add(entry);
    }

    public abstract long finishReport() throws ClientException;

    public void abortReport() throws ClientException {
        entries.clear();
    }

    public void dispose() {
        entries.clear();
    }

    private static enum Kind {
        SET_PATH, DELETE_PATH, LINK_PATH
    }

    private static class Entry {
        Kind kind;
        String path;
        String url;
        long revision;
        Depth depth;
        boolean startEmpty;
        String lockToken;

        public void applyTo(org.tmatesoft.svn.core.io.ISVNReporter reporter) throws SVNException {
            switch (kind) {
                case SET_PATH:
                    reporter.setPath(path, lockToken, revision, SVNClientImpl.getDepth(depth), startEmpty);
                    break;
                case LINK_PATH:
                    reporter.linkPath(SVNURL.parseURIEncoded(url), path, lockToken, revision, SVNClientImpl.getDepth(depth), startEmpty);
                    break;
                case DELETE_PATH:
                    reporter.deletePath(path);
                    break;
                default:
                    throw new IllegalStateException("Uknown kind " + kind);
            }
        }
    }
}
