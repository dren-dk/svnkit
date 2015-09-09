package org.tmatesoft.svn.core.internal.server.dav.handlers;

import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.server.dav.DAVRepositoryManager;
import org.tmatesoft.svn.core.internal.server.dav.DAVResource;
import org.tmatesoft.svn.core.internal.util.SVNDate;

public class DAVHeadHandler extends ServletDAVHandler {

    private DAVDeleteRequest myDAVRequest;

    protected DAVHeadHandler(DAVRepositoryManager connector, HttpServletRequest request, HttpServletResponse response) {
        super(connector, request, response);
    }

    @Override
    public void execute() throws SVNException {
        readInput(false);
        DAVResource resource = getRequestedDAVResource(false, false);
        if (!resource.exists()) {
            sendError(HttpServletResponse.SC_NOT_FOUND, null);
            return;
        }
        try {
            setResponseContentType(resource.getContentType());
        } catch (SVNException e) {
            //nothing to do we just skip this header
        }
        setResponseHeader(ACCEPT_RANGES_HEADER, ACCEPT_RANGES_DEFAULT_VALUE);
        try {
            Date lastModifiedTime = resource.getLastModified();
            if (lastModifiedTime != null) {
                setResponseHeader(LAST_MODIFIED_HEADER, SVNDate.formatRFC1123Date(lastModifiedTime));
            }
        } catch (SVNException e) {
            //nothing to do we just skip this header
        }

        String eTag = resource.getETag();
        if (eTag != null) {
            setResponseHeader(ETAG_HEADER, eTag);
        }
        setResponseStatus(HttpServletResponse.SC_NO_CONTENT);
    }

    @Override
    protected DAVRequest getDAVRequest() {
        if (myDAVRequest == null) {
            myDAVRequest = new DAVDeleteRequest();
        }
        return myDAVRequest;
    }
}