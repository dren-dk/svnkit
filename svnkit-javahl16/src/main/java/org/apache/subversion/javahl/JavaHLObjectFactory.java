package org.apache.subversion.javahl;

import java.util.List;

import org.apache.subversion.javahl.types.NodeKind;

public class JavaHLObjectFactory {
    
    public static CommitItem createCommitItem(String p, NodeKind nk, int sf, String u, String cu, long r, String mf) {
        return new CommitItem(p, nk, sf, u, cu, r, mf);
    }

    public static ClientException createClientException(String message, Throwable cause, String source,
                                                        int aprError, List<ClientException.ErrorMessage> messageStack) {
        return new ClientException(message, cause, source, aprError, messageStack);
    }
}
