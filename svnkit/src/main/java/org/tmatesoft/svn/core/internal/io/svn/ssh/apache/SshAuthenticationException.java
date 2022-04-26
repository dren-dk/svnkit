package org.tmatesoft.svn.core.internal.io.svn.ssh.apache;

import java.io.IOException;

public class SshAuthenticationException extends IOException {

    private static final long serialVersionUID = 1L;
    
    public SshAuthenticationException(String message) {
        super(message);
    }

}
