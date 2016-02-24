package org.apache.subversion.javahl.remote;

import org.apache.subversion.javahl.ISVNEditor;
import org.apache.subversion.javahl.callback.RemoteStatus;

public class JavaHLRemoteAccessUtil {

    public static ISVNEditor remoteStatusCallbackAsEditor(RemoteStatus callback) {
        return new StatusEditor(callback);
    }
}
