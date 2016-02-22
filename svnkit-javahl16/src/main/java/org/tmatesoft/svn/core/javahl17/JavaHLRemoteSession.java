package org.tmatesoft.svn.core.javahl17;

import org.apache.subversion.javahl.ClientException;
import org.apache.subversion.javahl.ISVNEditor;
import org.apache.subversion.javahl.ISVNRemote;
import org.apache.subversion.javahl.ISVNReporter;
import org.apache.subversion.javahl.callback.*;
import org.apache.subversion.javahl.types.*;
import org.tmatesoft.svn.core.*;
import org.tmatesoft.svn.core.internal.util.SVNPathUtil;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.io.SVNLocationSegment;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.util.SVNLogType;

import java.io.OutputStream;
import java.util.*;

public class JavaHLRemoteSession implements ISVNRemote, ISVNCanceller {

    private final SVNRepository svnRepository;
    private boolean cancelled;

    public JavaHLRemoteSession(SVNRepository svnRepository) {
        this.svnRepository = svnRepository;
    }

    public void dispose() {
        if (svnRepository != null) {
            svnRepository.closeSession();
        }
    }

    public void cancelOperation() throws ClientException {
        cancelled = true;
    }

    public void reparent(String url) throws ClientException {
        try {
            svnRepository.setLocation(SVNURL.parseURIEncoded(url), false);
        } catch (SVNException e) {
            throw ClientException.fromException(e);
        }
    }

    public String getSessionUrl() throws ClientException {
        return svnRepository.getLocation().toString();
    }

    public String getSessionRelativePath(String urlString) throws ClientException {
        try {
            final SVNURL url = SVNURL.parseURIEncoded(urlString);

            return SVNPathUtil.getPathAsChild(svnRepository.getLocation().getPath(), url.getPath());
        } catch (SVNException e) {
            throw ClientException.fromException(e);
        }
    }

    public String getReposRelativePath(String urlString) throws ClientException {
        try {
            final SVNURL url = SVNURL.parseURIEncoded(urlString);
            final SVNURL repositoryRoot = svnRepository.getRepositoryRoot(false);

            return SVNPathUtil.getPathAsChild(repositoryRoot.getPath(), url.getPath());
        } catch (SVNException e) {
            throw ClientException.fromException(e);
        }
    }

    public String getReposUUID() throws ClientException {
        try {
            return svnRepository.getRepositoryUUID(false);
        } catch (SVNException e) {
            throw ClientException.fromException(e);
        }
    }

    public String getReposRootUrl() throws ClientException {
        try {
            final SVNURL repositoryRoot = svnRepository.getRepositoryRoot(false);
            return repositoryRoot.toString();
        } catch (SVNException e) {
            throw ClientException.fromException(e);
        }
    }

    public long getLatestRevision() throws ClientException {
        try {
            return svnRepository.getLatestRevision();
        } catch (SVNException e) {
            throw ClientException.fromException(e);
        }
    }

    public long getRevisionByDate(Date date) throws ClientException {
        try {
            return svnRepository.getDatedRevision(date);
        } catch (SVNException e) {
            throw ClientException.fromException(e);
        }
    }

    public long getRevisionByTimestamp(long timestamp) throws ClientException {
        return getRevisionByDate(new Date(timestamp));
    }

    public void changeRevisionProperty(long revision, String propertyName, byte[] oldValue, byte[] newValue) throws ClientException {
        try {
            svnRepository.setRevisionPropertyValue(revision, propertyName, SVNPropertyValue.create(propertyName, newValue));
        } catch (SVNException e) {
            throw ClientException.fromException(e);
        }
    }

    public Map<String, byte[]> getRevisionProperties(long revision) throws ClientException {
        try {
            final SVNProperties revisionProperties = svnRepository.getRevisionProperties(revision, null);
            return SVNClientImpl.getProperties(revisionProperties);
        } catch (SVNException e) {
            throw ClientException.fromException(e);
        }
    }

    public byte[] getRevisionProperty(long revision, String propertyName) throws ClientException {
        try {
            final SVNPropertyValue propertyValue = svnRepository.getRevisionPropertyValue(revision, propertyName);
            return SVNPropertyValue.getPropertyAsBytes(propertyValue);
        } catch (SVNException e) {
            throw ClientException.fromException(e);
        }
    }

    public ISVNEditor getCommitEditor(Map<String, byte[]> revisionProperties, CommitCallback commitCallback, Set<Lock> lockTokens, boolean keepLocks, ISVNEditor.ProvideBaseCallback getBase, ISVNEditor.ProvidePropsCallback getProps, ISVNEditor.GetNodeKindCallback getCopyfromKind) throws ClientException {
        //TODO
        throw new UnsupportedOperationException("Editor V2 is not supported");
    }

    public ISVNEditor getCommitEditor(Map<String, byte[]> revisionProperties, CommitCallback commitCallback, Set<Lock> lockTokens, boolean keepLocks) throws ClientException {
        //TODO
        throw new UnsupportedOperationException("Editor V2 is not supported");
    }

    public long getFile(long revision, String path, OutputStream contents, Map<String, byte[]> properties) throws ClientException {
        final SVNProperties svnProperties = new SVNProperties();
        long fileRevision;
        try {
            fileRevision = svnRepository.getFile(path, revision, svnProperties, contents);
        } catch (SVNException e) {
            throw ClientException.fromException(e);
        }
        if (properties != null) {
            properties.putAll(SVNClientImpl.getProperties(svnProperties));
        }
        return fileRevision;
    }

    public long getDirectory(long revision, String path, int direntFields, final Map<String, DirEntry> dirents, Map<String, byte[]> properties) throws ClientException {
        final SVNProperties svnProperties = new SVNProperties();
        long fileRevision;
        try {
            final String reposRelativePath = getRepositoryRelativePath(path);
            fileRevision = svnRepository.getDir(path, revision, svnProperties, direntFields, new ISVNDirEntryHandler() {
                @Override
                public void handleDirEntry(SVNDirEntry dirEntry) throws SVNException {
                    dirents.put(dirEntry.getRelativePath(),
                            SVNClientImpl.getDirEntry(dirEntry, reposRelativePath));
                }
            });
        } catch (SVNException e) {
            throw ClientException.fromException(e);
        }
        if (properties != null) {
            properties.putAll(SVNClientImpl.getProperties(svnProperties));
        }
        return fileRevision;
    }

    public Map<String, Mergeinfo> getMergeinfo(Iterable<String> paths, long revision, Mergeinfo.Inheritance inherit, boolean includeDescendants) throws ClientException {
        try {
            final Map<String, SVNMergeInfo> mergeInfo = svnRepository.getMergeInfo(SVNClientImpl.getStrings(paths), revision, SVNClientImpl.getMergeInfoInheritance(inherit), includeDescendants);
            return SVNClientImpl.getMergeInfo(mergeInfo);
        } catch (SVNException e) {
            throw ClientException.fromException(e);
        }
    }

    public ISVNReporter status(final String statusTarget, final long revision, final Depth depth, RemoteStatus receiver) throws ClientException {
        //TODO
        return new JavaHLReporter() {
            @Override
            public long finishReport() throws ClientException {
                if (2+2==4) {
                    throw new UnsupportedOperationException("Editor V2 is not supported yet");
                }
                try {
                    svnRepository.status(revision, statusTarget, SVNClientImpl.getDepth(depth), getReporter(), null);
                    return -1;
                } catch (SVNException e) {
                    throw ClientException.fromException(e);
                }
            }
        };
    }

    public void getLog(Iterable<String> paths, long startRevision, long endRevision, int limit, boolean strictNodeHistory, boolean discoverPath, boolean includeMergedRevisions, Iterable<String> revisionProperties, LogMessageCallback callback) throws ClientException {
        try {
            svnRepository.log(SVNClientImpl.getStrings(paths), startRevision, endRevision, discoverPath, strictNodeHistory, limit, includeMergedRevisions, SVNClientImpl.getStrings(revisionProperties), SVNClientImpl.getLogEntryHandler(callback));
        } catch (SVNException e) {
            throw ClientException.fromException(e);
        }
    }

    public NodeKind checkPath(String path, long revision) throws ClientException {
        try {
            return SVNClientImpl.getNodeKind(svnRepository.checkPath(path, revision));
        } catch (SVNException e) {
            throw ClientException.fromException(e);
        }
    }

    public DirEntry stat(String path, long revision) throws ClientException {
        try {
            final SVNDirEntry dirEntry = svnRepository.info(path, revision);
            return SVNClientImpl.getDirEntry(dirEntry, getRepositoryRelativePath(path));
        } catch (SVNException e) {
            throw ClientException.fromException(e);
        }
    }

    public Map<Long, String> getLocations(String path, long pegRevision, Iterable<Long> locationRevisions) throws ClientException {
        try {
            return svnRepository.getLocations(path, (Map) null, pegRevision, SVNClientImpl.getLongs(locationRevisions));
        } catch (SVNException e) {
            throw ClientException.fromException(e);
        }
    }

    public void getLocationSegments(String path, long pegRevision, long startRevision, long endRevision, RemoteLocationSegmentsCallback handler) throws ClientException {
        try {
            svnRepository.getLocationSegments(path, pegRevision, startRevision, endRevision, SVNClientImpl.getLocationSegmentHandler(handler));
        } catch (SVNException e) {
            throw ClientException.fromException(e);
        }
    }

    public List<LocationSegment> getLocationSegments(String path, long pegRevision, long startRevision, long endRevision) throws ClientException {
        try {
            final List<SVNLocationSegment> locationSegments = svnRepository.getLocationSegments(path, pegRevision, startRevision, endRevision);
            return SVNClientImpl.getLocationSegments(locationSegments);
        } catch (SVNException e) {
            throw ClientException.fromException(e);
        }
    }

    public void getFileRevisions(String path, long startRevision, long endRevision, boolean includeMergedRevisions, RemoteFileRevisionsCallback handler) throws ClientException {
        try {
            svnRepository.getFileRevisions(path, startRevision, endRevision, includeMergedRevisions, SVNClientImpl.getFileRevisionHandler(handler));
        } catch (SVNException e) {
            throw ClientException.fromException(e);
        }
    }

    public List<FileRevision> getFileRevisions(String path, long startRevision, long endRevision, boolean includeMergedRevisions) throws ClientException {
        //TODO
        return null;
    }

    public Map<String, Lock> getLocks(String path, Depth depth) throws ClientException {
        try {
            final SVNLock[] locks = svnRepository.getLocks(path);
            return SVNClientImpl.getLocks(locks);
        } catch (SVNException e) {
            throw ClientException.fromException(e);
        }
    }

    public boolean hasCapability(Capability capability) throws ClientException {
        try {
            return svnRepository.hasCapability(SVNClientImpl.getCapability(capability));
        } catch (SVNException e) {
            throw ClientException.fromException(e);
        }
    }

    public void checkCancelled() throws SVNCancelException {
        if (cancelled) {
            SVNErrorManager.cancel("Cancelled.", SVNLogType.CLIENT);
        }
    }

    private String getRepositoryRelativePath(String path) throws ClientException, SVNException {
        return getReposRelativePath(svnRepository.getLocation().appendPath(path, false).toString());
    }
}
