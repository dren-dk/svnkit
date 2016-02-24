package org.tmatesoft.svn.core.internal.wc17;

import org.tmatesoft.svn.core.SVNProperties;

import java.io.File;

public interface ISVNEditorProxyCallbacks {

    void unlock(String path);

    SVNProperties fetchProperties(String path, long baseRevision);

    File fetchBase(String path, long baseRevision);

    ISVNEditorExtraCallbacks getExtraCallbacks();
}
