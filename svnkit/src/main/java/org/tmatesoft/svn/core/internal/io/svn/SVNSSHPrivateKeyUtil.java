package org.tmatesoft.svn.core.internal.io.svn;

import com.trilead.ssh2.auth.AgentProxy;
import org.apache.sshd.common.config.keys.FilePasswordProvider;
import org.apache.sshd.common.util.security.SecurityUtils;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.util.SVNDebugLog;
import org.tmatesoft.svn.util.SVNLogType;

import java.io.*;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.security.KeyPair;

public class SVNSSHPrivateKeyUtil {
    
    private static final String TRILEAD_AGENT_PROXY_CLASS = "com.jcraft.jsch.agentproxy.TrileadAgentProxy";
    private static final String CONNECTOR_FACTORY_CLASS = "com.jcraft.jsch.agentproxy.ConnectorFactory";
    private static final String CONNECTOR_CLASS = "com.jcraft.jsch.agentproxy.Connector";

    /**
     * @deprecated This method uses the AgentProxy class which is part of trilead-ssh2 and thus abandoned
     */
    public static AgentProxy createOptionalSSHAgentProxy() {
        try {
            final Class<?> connectorClass = Class.forName(CONNECTOR_CLASS);
            final Method connectorFactoryGetDefault = Class.forName(CONNECTOR_FACTORY_CLASS).getMethod("getDefault");
            final Method connectorFactoryCreateConnector = Class.forName(CONNECTOR_FACTORY_CLASS).getMethod("createConnector");
            
            final Object connectorFactory = connectorFactoryGetDefault.invoke(null);
            final Object connector = connectorFactoryCreateConnector.invoke(connectorFactory);
            
            final Class<?> proxyClass = Class.forName(TRILEAD_AGENT_PROXY_CLASS);
            return (AgentProxy) proxyClass.getConstructor(connectorClass).newInstance(connector);
        } catch (Throwable th) {
            SVNDebugLog.getDefaultLog().logFine(SVNLogType.NETWORK, "Failed to load TrileadAgentProxy");            
            SVNDebugLog.getDefaultLog().logFine(SVNLogType.NETWORK, th);            
        }
        return null;
    }


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
            while(true) {
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
            final Iterable<KeyPair> keyPairs = SecurityUtils.loadKeyPairIdentities(null, () -> "byte array",
                    new ByteArrayInputStream(pkBytes), FilePasswordProvider.of(password));
            return keyPairs.iterator().hasNext();
        } catch (Exception e) {
            SVNDebugLog.getDefaultLog().logFine(SVNLogType.NETWORK, e);
            return false;
        }
    }


}
