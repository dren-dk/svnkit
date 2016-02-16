package org.tmatesoft.svn.core.javahl17;

import org.apache.subversion.javahl.types.RuntimeVersion;

public class SVNClientImplRuntimeVersion extends RuntimeVersion {

    private static SVNClientImplRuntimeVersion instance;

    public int getMajor() {
        return SVNClientImpl.versionMajor();
    }

    public int getMinor() {
        return SVNClientImpl.versionMinor();
    }

    public int getPatch() {
        return SVNClientImpl.versionMicro();
    }

    public long getRevisionNumber() {
        return SVNClientImpl.versionRevisionNumber();
    }

    private String getNumberTag() {
        return "r" + getRevisionNumber();
    }

    public String toString() {
        String revision = getRevisionNumber() < 0 ? org.tmatesoft.svn.util.Version.getRevisionString() : Long.toString(getRevisionNumber());
        return "SVNKit v" + getMajor() + "." + getMinor() + "." + getPatch() + "." + revision;
    }

    public static RuntimeVersion getInstance() {
        if (instance == null) {
            instance = new SVNClientImplRuntimeVersion();
        }
        return instance;
    }

}
