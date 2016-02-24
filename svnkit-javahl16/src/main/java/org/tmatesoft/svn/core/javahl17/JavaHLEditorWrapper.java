package org.tmatesoft.svn.core.javahl17;

import org.apache.subversion.javahl.ClientException;
import org.apache.subversion.javahl.ISVNEditor;
import org.tmatesoft.svn.core.*;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.internal.wc17.ISVNEditor2;
import org.tmatesoft.svn.core.wc2.SvnChecksum;
import org.tmatesoft.svn.util.SVNLogType;

import java.io.InputStream;
import java.util.List;

public class JavaHLEditorWrapper implements ISVNEditor2 {

    private final ISVNEditor delegate;

    public JavaHLEditorWrapper(ISVNEditor delegate) {
        this.delegate = delegate;
    }

    public void dispose() {
        delegate.dispose();
    }

    public void addDir(String path, List<String> children, SVNProperties props, long replacesRev) throws SVNException {
        try {
            delegate.addDirectory(path, children, SVNClientImpl.getProperties(props), replacesRev);
        } catch (ClientException e) {
            SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.UNKNOWN, e);
            SVNErrorManager.error(errorMessage, SVNLogType.CLIENT);
        }
    }

    public void addFile(String path, SvnChecksum checksum, InputStream contents, SVNProperties props, long replacesRev) throws SVNException {
        try {
            delegate.addFile(path, SVNClientImpl.getChecksum(checksum), contents, SVNClientImpl.getProperties(props), replacesRev);
        } catch (ClientException e) {
            SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.UNKNOWN, e);
            SVNErrorManager.error(errorMessage, SVNLogType.CLIENT);
        }
    }

    public void addSymlink(String path, String target, SVNProperties props, long replacesRev) throws SVNException {
        try {
            delegate.addSymlink(path, target, SVNClientImpl.getProperties(props), replacesRev);
        } catch (ClientException e) {
            SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.UNKNOWN, e);
            SVNErrorManager.error(errorMessage, SVNLogType.CLIENT);
        }
    }

    public void addAbsent(String path, SVNNodeKind kind, long replacesRev) throws SVNException {
        try {
            delegate.addAbsent(path, SVNClientImpl.getNodeKind(kind), replacesRev);
        } catch (ClientException e) {
            SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.UNKNOWN, e);
            SVNErrorManager.error(errorMessage, SVNLogType.CLIENT);
        }
    }

    public void alterDir(String path, long revision, List<String> children, SVNProperties props) throws SVNException {
        try {
            delegate.alterDirectory(path, revision, children, SVNClientImpl.getProperties(props));
        } catch (ClientException e) {
            SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.UNKNOWN, e);
            SVNErrorManager.error(errorMessage, SVNLogType.CLIENT);
        }
    }

    public void alterFile(String path, long revision, SVNProperties props, SvnChecksum checksum, InputStream newContents) throws SVNException {
        try {
            delegate.alterFile(path, revision, SVNClientImpl.getChecksum(checksum), newContents, SVNClientImpl.getProperties(props));
        } catch (ClientException e) {
            SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.UNKNOWN, e);
            SVNErrorManager.error(errorMessage, SVNLogType.CLIENT);
        }
    }

    public void alterSymlink(String path, long revision, SVNProperties props, String target) throws SVNException {
        try {
            delegate.alterSymlink(path, revision, target, SVNClientImpl.getProperties(props));
        } catch (ClientException e) {
            SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.UNKNOWN, e);
            SVNErrorManager.error(errorMessage, SVNLogType.CLIENT);
        }
    }

    public void rotate(List<String> relPaths, List<String> revisions) throws SVNException {
        //rotate() method seems to be removed
        throw new UnsupportedOperationException("rotate() method is deprecated");
    }

    public void delete(String relativePath, long revision) throws SVNException {
        try {
            delegate.delete(relativePath, revision);
        } catch (ClientException e) {
            SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.UNKNOWN, e);
            SVNErrorManager.error(errorMessage, SVNLogType.CLIENT);
        }
    }

    public void copy(String sourceRelativePath, long sourceRevision, String destinationRelativePath, long replacesRevision) throws SVNException {
        try {
            delegate.copy(sourceRelativePath, sourceRevision, destinationRelativePath, replacesRevision);
        } catch (ClientException e) {
            SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.UNKNOWN, e);
            SVNErrorManager.error(errorMessage, SVNLogType.CLIENT);
        }
    }

    public void move(String sourceRelativePath, long sourceRevision, String destinationRelativePath, long replacesRevision) throws SVNException {
        try {
            delegate.move(sourceRelativePath, sourceRevision, destinationRelativePath, replacesRevision);
        } catch (ClientException e) {
            SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.UNKNOWN, e);
            SVNErrorManager.error(errorMessage, SVNLogType.CLIENT);
        }
    }

    public void complete() throws SVNException {
        try {
            delegate.complete();
        } catch (ClientException e) {
            SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.UNKNOWN, e);
            SVNErrorManager.error(errorMessage, SVNLogType.CLIENT);
        }
    }

    public void abort() throws SVNException {
        try {
            delegate.abort();
        } catch (ClientException e) {
            SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.UNKNOWN, e);
            SVNErrorManager.error(errorMessage, SVNLogType.CLIENT);
        }
    }
}
