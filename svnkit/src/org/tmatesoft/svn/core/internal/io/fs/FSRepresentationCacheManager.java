/*
 * ====================================================================
 * Copyright (c) 2004-2009 TMate Software Ltd.  All rights reserved.
 *
 * This software is licensed as described in the file COPYING, which
 * you should have received as part of this distribution.  The terms
 * are also available at http://svnkit.com/license.html.
 * If newer versions of this license are posted there, you may use a
 * newer version instead, at your option.
 * ====================================================================
 */
package org.tmatesoft.svn.core.internal.io.fs;

import java.io.File;

import org.tmatesoft.sqljet.core.SqlJetErrorCode;
import org.tmatesoft.sqljet.core.SqlJetException;
import org.tmatesoft.sqljet.core.internal.table.SqlJetTable;
import org.tmatesoft.sqljet.core.schema.ISqlJetSchema;
import org.tmatesoft.sqljet.core.schema.ISqlJetTableDef;
import org.tmatesoft.sqljet.core.table.ISqlJetCursor;
import org.tmatesoft.sqljet.core.table.ISqlJetRunnableWithLock;
import org.tmatesoft.sqljet.core.table.SqlJetDb;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.util.SVNDebugLog;
import org.tmatesoft.svn.util.SVNLogType;


/**
 * @version 1.3
 * @author  TMate Software Ltd.
 */
public class FSRepresentationCacheManager {
    
    public static final String REP_CACHE_TABLE = "rep_cache";
    private static final int REP_CACHE_DB_FORMAT =  1;
    private static final String REP_CACHE_DB_SQL =   "create table rep_cache (hash text not null primary key, " +
                                                    "                        revision integer not null, " + 
                                                    "                        offset integer not null, " + 
                                                    "                        size integer not null, " +
                                                    "                        expanded_size integer not null); ";

    private SqlJetDb myRepCacheDB;
    private SqlJetTable myTable;
    private FSFS myFSFS;

    public static FSRepresentationCacheManager openRepresentationCache(FSFS fsfs) throws SVNException {
        final FSRepresentationCacheManager cacheObj = new FSRepresentationCacheManager();
        try {
            cacheObj.myRepCacheDB = SqlJetDb.open(fsfs.getRepositoryCacheFile(), true);
            cacheObj.myRepCacheDB.runWithLock(new ISqlJetRunnableWithLock() {
                public Object runWithLock(SqlJetDb db) throws SqlJetException {
                    checkFormat(db);
                    cacheObj.myTable = db.getTable(REP_CACHE_TABLE);
                    return null;
                }
            });
        } catch (SqlJetException e) {
            SVNErrorManager.error(convertError(e), SVNLogType.FSFS);
        }
        return cacheObj;
    }
    
    private static void checkFormat(SqlJetDb db) throws SqlJetException {
        ISqlJetSchema schema = db.getSchema();
        int version = schema.getMeta().getUserCookie();
        if (version < REP_CACHE_DB_FORMAT) {
            db.beginTransaction();
            try {
                schema.getMeta().setUserCookie(REP_CACHE_DB_FORMAT);
                schema.getMeta().setAutovacuum(true);
                schema.createTable(FSRepresentationCacheManager.REP_CACHE_DB_SQL);
                db.commit();
            } catch (SqlJetException e) {
                db.rollback();
                throw e;
            }
        } else if (version > REP_CACHE_DB_FORMAT) {
            throw new SqlJetException("Schema format " + version + " not recognized");   
        }
        
    }
    
    public static void createRepresentationCache(File path) throws SVNException {
        SqlJetDb db = null;
        try {
            db = SqlJetDb.open(path, true);
            db.runWithLock(new ISqlJetRunnableWithLock() {
                public Object runWithLock(SqlJetDb db) throws SqlJetException {
                    checkFormat(db);
                    return null;
                }
            });
        } catch (SqlJetException e) {
            SVNErrorManager.error(FSRepresentationCacheManager.convertError(e), SVNLogType.FSFS);
        } finally {
            if (db != null) {
                try {
                    db.close();
                } catch (SqlJetException e) {
                    SVNDebugLog.getDefaultLog().logFine(SVNLogType.FSFS, e);
                }
            }
        }
    }
    
    public void insert(final FSRepresentation representation, boolean rejectDup) throws SVNException {
        if (representation.getSHA1HexDigest() == null) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.BAD_CHECKSUM_KIND, 
                    "Only SHA1 checksums can be used as keys in the rep_cache table.\n");
            SVNErrorManager.error(err, SVNLogType.FSFS);
        }
        
        FSRepresentation oldRep = getRepresentationByHash(representation.getSHA1HexDigest());
        if (oldRep != null) {
            if (rejectDup && (oldRep.getRevision() != representation.getRevision() || oldRep.getOffset() != representation.getOffset() ||
                    oldRep.getSize() != representation.getSize() || oldRep.getExpandedSize() != representation.getExpandedSize())) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_CORRUPT, "Representation key for checksum ''{0}'' exists in " + 
                        "filesystem ''{1}'' with a different value ({2},{3},{4},{5}) than what we were about to store ({6},{7},{8},{9})", 
                        new Object[] { representation.getSHA1HexDigest(), myFSFS.getRepositoryRoot(), String.valueOf(oldRep.getRevision()), 
                        String.valueOf(oldRep.getOffset()), String.valueOf(oldRep.getSize()), String.valueOf(oldRep.getExpandedSize()), 
                        String.valueOf(representation.getRevision()), String.valueOf(representation.getOffset()), 
                        String.valueOf(representation.getSize()), String.valueOf(representation.getExpandedSize()) });
                SVNErrorManager.error(err, SVNLogType.FSFS);
            }
            
            return;
        }
        
        try {
            myRepCacheDB.runWithLock(new ISqlJetRunnableWithLock() {

                public Object runWithLock(SqlJetDb db) throws SqlJetException {
                    ISqlJetCursor lookup = null;
                    try {
                        lookup = myTable.lookup(myTable.getPrimaryKeyIndex(), new Object[] { representation.getSHA1HexDigest() });
                        if (!lookup.eof()) {
                            return null;
                        }
                    } finally {
                        if (lookup != null) {
                            lookup.close();
                        }
                    }
                    db.beginTransaction();
                    try {
                        myTable.insert(new Object[] { representation.getSHA1HexDigest(), new Long(representation.getRevision()),
                                new Long(representation.getOffset()), new Long(representation.getSize()), 
                                new Long(representation.getExpandedSize()) });
                        db.commit();
                    } catch (SqlJetException e) {
                        db.rollback();
                        throw e;
                    }
                    return null;
                }
            });
        } catch (SqlJetException e) {
            SVNErrorManager.error(convertError(e), SVNLogType.FSFS);
        }
    }

    public void close() throws SVNException {
        try {
            myRepCacheDB.runWithLock(new ISqlJetRunnableWithLock() {
                public Object runWithLock(SqlJetDb db) throws SqlJetException {
                    myRepCacheDB.close();
                    return null;
                }
            });
        } catch (SqlJetException e) {
            SVNErrorManager.error(convertError(e), SVNLogType.FSFS);
        }
    }
    
    public FSRepresentation getRepresentationByHash(String hash) throws SVNException {
        FSRepresentationCacheRecord cache = getByHash(hash);
        if (cache != null) {
            FSRepresentation representation = new FSRepresentation();
            representation.setExpandedSize(cache.getExpandedSize());
            representation.setOffset(cache.getOffset());
            representation.setRevision(cache.getRevision());
            representation.setSize(cache.getSize());
            return representation;
        }
        return null;
    }

    private FSRepresentationCacheRecord getByHash(final String hash) throws SVNException {
        try {
            return (FSRepresentationCacheRecord) myRepCacheDB.runWithLock(new ISqlJetRunnableWithLock() {

                public Object runWithLock(SqlJetDb db) throws SqlJetException {
                    ISqlJetCursor lookup = null;
                    try {
                        lookup = myTable.lookup(myTable.getPrimaryKeyIndex(), new Object[] { hash });
                        if (!lookup.eof()) {
                            return new FSRepresentationCacheRecord(lookup);
                        }
                    } finally {
                        if (lookup != null) {
                            lookup.close();
                        }
                    }
                    return null;
                }
            });
        } catch (SqlJetException e) {
            SVNErrorManager.error(convertError(e), SVNLogType.FSFS);
        }
        return null;
    }

    private static SVNErrorMessage convertError(SqlJetException e) {
        SVNErrorMessage err = SVNErrorMessage.create(convertErrorCode(e), e.getMessage());
        return err;
    }
    
    private static SVNErrorCode convertErrorCode(SqlJetException e) {
        SqlJetErrorCode sqlCode = e.getErrorCode();
        if (sqlCode == SqlJetErrorCode.READONLY) {
            return SVNErrorCode.SQLITE_READONLY;
        } 
        return SVNErrorCode.SQLITE_ERROR;
    }
    
}
