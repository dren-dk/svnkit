/*
 * ====================================================================
 * Copyright (c) 2004-2012 TMate Software Ltd.  All rights reserved.
 *
 * This software is licensed as described in the file COPYING, which
 * you should have received as part of this distribution.  The terms
 * are also available at http://svnkit.com/license.html
 * If newer versions of this license are posted there, you may use a
 * newer version instead, at your option.
 * ====================================================================
 */
package org.tmatesoft.svn.core.internal.io.dav.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;

import org.tmatesoft.svn.core.auth.SVNPasswordAuthentication;
import org.tmatesoft.svn.core.internal.util.SVNBase64;

/**
 * @version 1.3
 * @author  TMate Software Ltd.
 */
class HTTPBasicAuthentication extends HTTPAuthentication {

    private String myCharset;

    public HTTPBasicAuthentication (SVNPasswordAuthentication credentials, String charset) {
        super(credentials);
        myCharset = charset;
    }

    protected HTTPBasicAuthentication (String name, char[] password, String charset) {
        super(name, password);
        myCharset = charset;
    }

    protected HTTPBasicAuthentication (String charset) {
        myCharset = charset;
    }

    public String authenticate() {
        if (getUserName() == null || getPassword() == null) {
            return null;
        }
        
        StringBuffer result = new StringBuffer();

        Charset charset;
        try {
            charset = Charset.forName(myCharset);
        } catch (UnsupportedCharsetException e) {
            charset = Charset.defaultCharset();
        }
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            bos.write(getUserName().getBytes(charset));
            bos.write(":".getBytes(charset));
            
            final CharBuffer buffer = CharBuffer.wrap(getPassword());
            final ByteBuffer encodedBuffer = charset.newEncoder().encode(buffer);
            final byte[] bytes = new byte[encodedBuffer.limit()];
            try {
                encodedBuffer.get(bytes);
                bos.write(bytes);
            } finally {
                HTTPAuthentication.clear(bytes);
                if (encodedBuffer.hasArray()) {
                    HTTPAuthentication.clear(encodedBuffer.array());
                }
            }
        } catch (IOException e) {
            //
        }

        result.append("Basic ");
        result.append(SVNBase64.byteArrayToBase64(bos.toByteArray()));
        return result.toString();
    }

    public String getAuthenticationScheme(){
        return "Basic";
    }

}
