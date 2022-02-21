package org.tmatesoft.svn.core.internal.io.svn;

import org.apache.sshd.common.config.keys.FilePasswordProvider;
import org.apache.sshd.common.util.security.SecurityUtils;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.util.SVNDebugLog;
import org.tmatesoft.svn.util.SVNLogType;

import java.io.*;
import java.nio.charset.Charset;
import java.security.KeyPair;

public class SVNSSHPrivateKeyUtil {

    public static char[] readPrivateKey(File privateKey) {
        if (privateKey == null || !privateKey.exists() || !privateKey.isFile() || !privateKey.canRead()) {
            SVNDebugLog.getDefaultLog().logFine(SVNLogType.NETWORK, "Can not read private key from '" + privateKey + "'");
            return null;
        }
        Reader reader = null;
        StringWriter buffer = new StringWriter();
        try {
            reader = new BufferedReader(new FileReader(privateKey));
            int ch;
            while (true) {
                ch = reader.read();
                if (ch < 0) {
                    break;
                }
                buffer.write(ch);
            }
        } catch (IOException e) {
            SVNDebugLog.getDefaultLog().logFine(SVNLogType.NETWORK, e);
            return null;
        } finally {
            SVNFileUtil.closeFile(reader);
        }
        return buffer.toString().toCharArray();
    }

    /**
     * @deprecated
     */
    public static boolean isValidPrivateKey(char[] privateKey, String passphrase) {
        return isValidPrivateKey(privateKey, passphrase != null ? passphrase.toCharArray() : null);
    }

    public static boolean isValidPrivateKey(char[] privateKey, char[] passphrase) {

        try {
            final byte[] pkBytes = new String(privateKey).getBytes(Charset.defaultCharset());

            final String password = passphrase != null ? new String(passphrase) : null;
            final Iterable<KeyPair> keyPairs = SecurityUtils.loadKeyPairIdentities(null, () -> "hest",
                    new ByteArrayInputStream(pkBytes), FilePasswordProvider.of(password));
            return keyPairs.iterator().hasNext();
        } catch (Exception e) {
            SVNDebugLog.getDefaultLog().logFine(SVNLogType.NETWORK, e);
            return false;
        }
    }

}
