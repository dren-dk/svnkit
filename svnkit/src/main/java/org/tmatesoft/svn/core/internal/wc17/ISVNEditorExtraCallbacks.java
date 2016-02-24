package org.tmatesoft.svn.core.internal.wc17;

public interface ISVNEditorExtraCallbacks {
    void startEdit(long revision);
    void targetRevision(long revision);
}
