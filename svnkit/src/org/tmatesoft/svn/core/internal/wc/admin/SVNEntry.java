/*
 * ====================================================================
 * Copyright (c) 2004-2007 TMate Software Ltd.  All rights reserved.
 *
 * This software is licensed as described in the file COPYING, which
 * you should have received as part of this distribution.  The terms
 * are also available at http://svnkit.com/license.html
 * If newer versions of this license are posted there, you may use a
 * newer version instead, at your option.
 * ====================================================================
 */
package org.tmatesoft.svn.core.internal.wc.admin;

import java.io.File;
import java.util.Iterator;
import java.util.Map;

import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNProperty;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.util.SVNEncodingUtil;
import org.tmatesoft.svn.core.internal.util.SVNPathUtil;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.io.SVNRepository;


/**
 * @version 1.1.1
 * @author  TMate Software Ltd.
 */
public class SVNEntry implements Comparable {

    private Map myAttributes;
    private SVNAdminArea myAdminArea;
    private String myName;
    private File myPath;

    public SVNEntry(Map attributes, SVNAdminArea adminArea, String name) {
        myAttributes = attributes;
        myName = name;
        myAdminArea = adminArea;
        if (!myAttributes.containsKey(SVNProperty.NAME)) {
            myAttributes.put(SVNProperty.NAME, name);
        }
    }

    public boolean equals(Object obj) {
        if (obj == null || obj.getClass() != SVNEntry.class) {
            return false;
        }
        SVNEntry entry = (SVNEntry) obj;
        return entry.myAttributes == myAttributes && entry.myName.equals(myName);
    }

    public int hashCode() {
        return myAttributes.hashCode() + 17 * myName.hashCode();
    }

    public int compareTo(Object obj) {
        if (obj == this) {
            return 0;
        }
        if (obj == null || obj.getClass() != SVNEntry.class) {
            return 1;
        }
        if (isThisDir()) {
            return -1;
        }
        SVNEntry entry = (SVNEntry) obj;
        return myName.toLowerCase().compareTo(entry.myName.toLowerCase());
    }
    
    public boolean isThisDir() {
        return "".equals(getName());
    }

    public String getURL() {
        String url = (String)myAttributes.get(SVNProperty.URL);
        if (url == null && myAdminArea != null && !myAdminArea.getThisDirName().equals(myName)) {
            SVNEntry rootEntry = null; 
            try {    
                rootEntry = myAdminArea.getEntry(myAdminArea.getThisDirName(), true); 
            } catch (SVNException svne) {
                return url;
            }
            url = rootEntry.getURL();
            url = SVNPathUtil.append(url, SVNEncodingUtil.uriEncode(myName));
        }
        return url;
    }
    
    public SVNURL getSVNURL() throws SVNException {
        String url = getURL();
        if (url != null) {
            return SVNURL.parseURIEncoded(url);
        }
        return null;
    }

    public String getName() {
        return myName;
    }

    public boolean isDirectory() {
        return SVNProperty.KIND_DIR.equals(myAttributes.get(SVNProperty.KIND));
    }

    public long getRevision() {
        String revStr = (String)myAttributes.get(SVNProperty.REVISION);
        if (revStr == null && myAdminArea != null && !myAdminArea.getThisDirName().equals(myName)) {
            SVNEntry rootEntry = null;
            try {
                rootEntry = myAdminArea.getEntry(myAdminArea.getThisDirName(), true);
            } catch (SVNException svne) {
                return SVNRepository.INVALID_REVISION;
            }
            return rootEntry.getRevision();
        }
        return revStr != null ? Long.parseLong(revStr) : SVNRepository.INVALID_REVISION;
    }

    public boolean isScheduledForAddition() {
        return SVNProperty.SCHEDULE_ADD.equals(myAttributes.get(SVNProperty.SCHEDULE));
    }

    public boolean isScheduledForDeletion() {
        return SVNProperty.SCHEDULE_DELETE.equals(myAttributes.get(SVNProperty.SCHEDULE));
    }

    public boolean isScheduledForReplacement() {
        return SVNProperty.SCHEDULE_REPLACE.equals(myAttributes.get(SVNProperty.SCHEDULE));
    }

    public boolean isHidden() {
        return (isDeleted() || isAbsent()) && !isScheduledForAddition()
                && !isScheduledForReplacement();
    }

    public boolean isFile() {
        return SVNProperty.KIND_FILE.equals(myAttributes.get(SVNProperty.KIND));
    }

    public String getLockToken() {
        return (String)myAttributes.get(SVNProperty.LOCK_TOKEN);
    }

    public boolean isDeleted() {
        return Boolean.TRUE.toString().equals(myAttributes.get(SVNProperty.DELETED));
    }

    public boolean isAbsent() {
        return Boolean.TRUE.toString().equals(myAttributes.get(SVNProperty.ABSENT));
    }

    public String toString() {
        return myName;
    }

    private boolean setAttributeValue(String name, String value) {
        if (value == null) {
            return myAttributes.remove(name) != null;
        }
        Object oldValue = myAttributes.put(name, value);
        return !value.equals(oldValue);            
    }
    
    public boolean setRevision(long revision) {
        return setAttributeValue(SVNProperty.REVISION, Long.toString(revision));
    }

    public boolean setChangelistName(String changelistName) {
        return setAttributeValue(SVNProperty.CHANGELIST, changelistName);
    }
    
    public String getChangelistName() {
        return (String)myAttributes.get(SVNProperty.CHANGELIST);
    }
    
    public boolean setWorkingSize(long size) {
        if (getKind() == SVNNodeKind.FILE) {
            return setAttributeValue(SVNProperty.WORKING_SIZE, Long.toString(size));
        }
        return false;
    }

    public long getWorkingSize() {
        String workingSize = (String)myAttributes.get(SVNProperty.WORKING_SIZE);
        if (workingSize == null) {
            return SVNProperty.WORKING_SIZE_UNKNOWN;
        }
        return Long.parseLong(workingSize);
    }

    public SVNDepth getDepth() {
        String depthString = (String) myAttributes.get(SVNProperty.DEPTH);
        return SVNDepth.fromString(depthString);
    }

    public void setDepth(SVNDepth depth) {
        setAttributeValue(SVNProperty.DEPTH, depth.getName());
    }
    
    public boolean setURL(String url) {
        return setAttributeValue(SVNProperty.URL, url);
    }

    public void setIncomplete(boolean incomplete) {
        setAttributeValue(SVNProperty.INCOMPLETE, incomplete ? Boolean.TRUE.toString() : null);
    }

    public boolean isIncomplete() {
        return Boolean.TRUE.toString().equals(myAttributes.get(SVNProperty.INCOMPLETE));
    }

    public String getConflictOld() {
        return (String)myAttributes.get(SVNProperty.CONFLICT_OLD);
    }

    public void setConflictOld(String name) {
        setAttributeValue(SVNProperty.CONFLICT_OLD, name);
    }

    public String getConflictNew() {
        return (String)myAttributes.get(SVNProperty.CONFLICT_NEW);
    }

    public void setConflictNew(String name) {
        setAttributeValue(SVNProperty.CONFLICT_NEW, name);
    }

    public String getConflictWorking() {
        return (String)myAttributes.get(SVNProperty.CONFLICT_WRK);
    }

    public void setConflictWorking(String name) {
        setAttributeValue(SVNProperty.CONFLICT_WRK, name);
    }

    public String getPropRejectFile() {
        return (String)myAttributes.get(SVNProperty.PROP_REJECT_FILE);
    }

    public void setPropRejectFile(String name) {
        setAttributeValue(SVNProperty.PROP_REJECT_FILE, name);
    }

    public String getAuthor() {
        return (String)myAttributes.get(SVNProperty.LAST_AUTHOR);
    }

    public String getCommittedDate() {
        return (String)myAttributes.get(SVNProperty.COMMITTED_DATE);
    }

    public long getCommittedRevision() {
        String rev = (String)myAttributes.get(SVNProperty.COMMITTED_REVISION);
        if (rev == null) {
            return SVNRepository.INVALID_REVISION ;
        }
        return Long.parseLong(rev);
    }

    public void setTextTime(String time) {
        setAttributeValue(SVNProperty.TEXT_TIME, time);
    }

    public void setKind(SVNNodeKind kind) {
        String kindStr = kind == SVNNodeKind.DIR ? SVNProperty.KIND_DIR : (kind == SVNNodeKind.FILE ? SVNProperty.KIND_FILE : null);
        setAttributeValue(SVNProperty.KIND, kindStr);
    }

    public void setAbsent(boolean absent) {
        setAttributeValue(SVNProperty.ABSENT, absent ? Boolean.TRUE.toString() : null);
    }

    public void setDeleted(boolean deleted) {
        setAttributeValue(SVNProperty.DELETED, deleted ? Boolean.TRUE.toString() : null);
    }

    public SVNNodeKind getKind() {
        String kind = (String)myAttributes.get(SVNProperty.KIND);
        if (SVNProperty.KIND_DIR.equals(kind)) {
            return SVNNodeKind.DIR;
        } else if (SVNProperty.KIND_FILE.equals(kind)) {
            return SVNNodeKind.FILE;
        }
        return SVNNodeKind.UNKNOWN;
    }
    
    public String getTextTime() {
        return (String)myAttributes.get(SVNProperty.TEXT_TIME);
    }

    public String getChecksum() {
        return (String)myAttributes.get(SVNProperty.CHECKSUM);
    }

    public void setLockComment(String comment) {
        myAttributes.put(SVNProperty.LOCK_COMMENT, comment);
    }

    public void setLockOwner(String owner) {
        setAttributeValue(SVNProperty.LOCK_OWNER, owner);
    }

    public void setLockCreationDate(String date) {
        setAttributeValue(SVNProperty.LOCK_CREATION_DATE, date);
    }

    public void setLockToken(String token) {
        setAttributeValue(SVNProperty.LOCK_TOKEN, token);
    }

    public void setUUID(String uuid) {
        setAttributeValue(SVNProperty.UUID, uuid);
    }

    public void unschedule() {
        setAttributeValue(SVNProperty.SCHEDULE, null);
    }

    public void scheduleForAddition() {
        setAttributeValue(SVNProperty.SCHEDULE, SVNProperty.SCHEDULE_ADD);
    }

    public void scheduleForDeletion() {
        setAttributeValue(SVNProperty.SCHEDULE, SVNProperty.SCHEDULE_DELETE);
    }

    public void scheduleForReplacement() {
        setAttributeValue(SVNProperty.SCHEDULE, SVNProperty.SCHEDULE_REPLACE);
    }

    public void setSchedule(String schedule) {
        setAttributeValue(SVNProperty.SCHEDULE, schedule);
    }

    public void setCopyFromRevision(long revision) {
        setAttributeValue(SVNProperty.COPYFROM_REVISION, revision >= 0 ? Long.toString(revision) : null);
    }

    public boolean setCopyFromURL(String url) {
        return setAttributeValue(SVNProperty.COPYFROM_URL, url);
    }

    public void setCopied(boolean copied) {
        setAttributeValue(SVNProperty.COPIED, copied ? Boolean.TRUE.toString() : null);
    }

    public String getCopyFromURL() {
        return (String)myAttributes.get(SVNProperty.COPYFROM_URL);
    }

    public SVNURL getCopyFromSVNURL() throws SVNException {
        String url = getCopyFromURL();
        if (url != null) {
            return SVNURL.parseURIEncoded(url);
        }
        return null;
    }

    public long getCopyFromRevision() {
        String rev = (String)myAttributes.get(SVNProperty.COPYFROM_REVISION);
        if (rev == null) {
            return SVNRepository.INVALID_REVISION;
        }
        return Long.parseLong(rev);
    }

    public String getPropTime() {
        return (String)myAttributes.get(SVNProperty.PROP_TIME);
    }

    public void setPropTime(String time) {
        setAttributeValue(SVNProperty.PROP_TIME, time);
    }

    public boolean isCopied() {
        return Boolean.TRUE.toString().equals(myAttributes.get(SVNProperty.COPIED));
    }

    public String getUUID() {
        return (String)myAttributes.get(SVNProperty.UUID);
    }

    public String getRepositoryRoot() {
        return (String)myAttributes.get(SVNProperty.REPOS);
    }

    public SVNURL getRepositoryRootURL() throws SVNException {
        String url = getRepositoryRoot();
        if (url != null) {
            return SVNURL.parseURIEncoded(url);
        }
        return null;
    }
    
    public boolean setRepositoryRoot(String url) {
        return setAttributeValue(SVNProperty.REPOS, url);
    }

    public boolean setRepositoryRootURL(SVNURL url) {
        return setRepositoryRoot(url == null ? null : url.toString());
    }

    public void loadProperties(Map entryProps) {
        if (entryProps == null) {
            return;
        }
        for (Iterator propNames = entryProps.keySet().iterator(); propNames.hasNext();) {
            String propName = (String) propNames.next();
            setAttributeValue(propName, (String) entryProps.get(propName));
        }
    }

    public String getLockOwner() {
        return (String)myAttributes.get(SVNProperty.LOCK_OWNER);
    }

    public String getLockComment() {
        return (String)myAttributes.get(SVNProperty.LOCK_COMMENT);
    }

    public String getLockCreationDate() {
        return (String)myAttributes.get(SVNProperty.LOCK_CREATION_DATE);
    }

    public String getSchedule() {
        return (String)myAttributes.get(SVNProperty.SCHEDULE);
    }

    public void setCachableProperties(String[] cachableProps) {
        if (cachableProps != null) {
            myAttributes.put(SVNProperty.CACHABLE_PROPS, cachableProps);
        } else {
            myAttributes.remove(SVNProperty.CACHABLE_PROPS);
        }
    }

    public void setKeepLocal(boolean keepLocal) {
        setAttributeValue(SVNProperty.KEEP_LOCAL, keepLocal ? Boolean.TRUE.toString() : null);
    }

    public boolean isKeepLocal() {
        return Boolean.TRUE.toString().equals(myAttributes.get(SVNProperty.KEEP_LOCAL));
    }

    public String[] getCachableProperties() {
        return (String[])myAttributes.get(SVNProperty.CACHABLE_PROPS);
    }

    public String[] getPresentProperties() {
        return (String[])myAttributes.get(SVNProperty.PRESENT_PROPS);
    }

    public Map asMap() {
        return myAttributes;
    }
    
    public SVNAdminArea getAdminArea() {
        return myAdminArea;
    }
    
    public boolean isSwitched() throws SVNException {
        File thisPath = getPath(); 
        File parent = thisPath.getParentFile();
        if (parent == null) {
            return false;
        }
        
        SVNWCAccess access = SVNWCAccess.newInstance(myAdminArea.getWCAccess());
        SVNAdminArea parentAdminArea = null;
        SVNEntry parentEntry = null;
        try {
            parentAdminArea = access.open(parent, false, 0);
            parentEntry = parentAdminArea.getVersionedEntry(parentAdminArea.getThisDirName(), false);
        } catch (SVNException svne) {
            if (svne.getErrorMessage().getErrorCode() == SVNErrorCode.WC_NOT_DIRECTORY) {
                return false;
            } 
            throw svne;
        } finally {
            access.close();
        }
        
        SVNURL parentSVNURL = parentEntry.getSVNURL();
        SVNURL thisSVNURL = getSVNURL(); 
        if (parentSVNURL == null || thisSVNURL == null) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.ENTRY_MISSING_URL, "Cannot find a URL for ''{0}''", parentSVNURL == null ? parent : thisPath);
            SVNErrorManager.error(err);
        }
        
        SVNURL expectedSVNURL = parentSVNURL.appendPath(myName, false);
        return !thisSVNURL.equals(expectedSVNURL);
    }
    
    private File getPath() {
        if (myPath == null) {
            myPath = myAdminArea.getFile(myName);
        }
        return myPath;
    }
}