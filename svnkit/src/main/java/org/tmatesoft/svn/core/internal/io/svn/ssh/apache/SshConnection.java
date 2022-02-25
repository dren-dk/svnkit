package org.tmatesoft.svn.core.internal.io.svn.ssh.apache;

import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.config.hosts.DefaultConfigFileHostEntryResolver;
import org.apache.sshd.client.future.ConnectFuture;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.config.keys.FilePasswordProvider;
import org.apache.sshd.common.io.nio2.Nio2ServiceFactoryFactory;
import org.apache.sshd.common.util.security.SecurityUtils;
import org.tmatesoft.svn.core.SVNException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.KeyPair;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SshConnection {
    private static final Logger log = Logger.getLogger(SshConnection.class.getName());

    private final SshClient client;
    private final SshHost host;
    private ClientSession session;
    private long lastAccessTime = System.currentTimeMillis();
    private int sessionCount;

    public SshConnection(SshHost host) throws Exception {
        this.host = host;
        client = SshClient.setUpDefaultClient();
        client.setIoServiceFactoryFactory(new Nio2ServiceFactoryFactory());

        byte[] privateKey = host.getPrivateKey();
        if (privateKey != null) {
            final Iterable<KeyPair> keyPairs = SecurityUtils.loadKeyPairIdentities(null, () -> "private key",
                    new ByteArrayInputStream(privateKey), FilePasswordProvider.of(host.getPrivateKeyPassphrase()));
            if (!keyPairs.iterator().hasNext()) {
                throw new RuntimeException("Did not find a keypair in the provided private key string: " + privateKey);
            }
            client.addPublicKeyIdentity(keyPairs.iterator().next());
        }

        if (host.getPassword() != null) {
            client.addPasswordIdentity(host.getPassword());
        }

/*
        if (!knownHostsFile.exists()) {
            if (!knownHostsFile.getParentFile().mkdirs()) {
                log.warning("Failed to mkdir "+knownHostsFile.getParentFile());
            }
            FileUtils.touch(knownHostsFile);
            Files.setPosixFilePermissions(knownHostsFile.toPath(), PosixFilePermissions.fromString("rw-------"));
        }
 			DefaultKnownHostsServerKeyVerifier knownHosts = new DefaultKnownHostsServerKeyVerifier(AcceptAllServerKeyVerifier.INSTANCE, true, knownHostsFile);
     			knownHosts.setModifiedServerKeyAcceptor((clientSession, remoteAddress, entry, expected, actual) -> true);
  */

        client.setServerKeyVerifier((clientSession, remoteAddress, serverKey) -> {
            try {
                host.getHostVerifier().verifyHostKey(host.getHostName(), host.getPort(), serverKey.getAlgorithm(), serverKey.getEncoded());
                return true;
            } catch (SVNException e) {
                log.log(Level.SEVERE, "Failed while verifying host key: " + host.getKey(), e);
                return false;
            }
        });

        client.setHostConfigEntryResolver(DefaultConfigFileHostEntryResolver.INSTANCE);

        try {
            client.start();

            ConnectFuture connectFuture = client.connect(host.getUserName(), host.getHostName(), host.getPort());
            connectFuture.await();

            session = connectFuture.getClientSession();
            session.auth().verify(10000);
        } catch (Exception e) {
            client.close();
            throw e;
        }
    }

    public ApacheSshSession openSession() throws IOException {
        sessionCount++;
        return new ApacheSshSession(this);
    }

    public void sessionClosed(ApacheSshSession sshSession) {
        lastAccessTime = System.currentTimeMillis();
        sessionCount--;
    }

    public int getSessionsCount() {
        return sessionCount;
    }

    public void close() {
        if (session != null) {
            session.close(false);
            session = null;
        }
        client.stop();
        sessionCount = 0;
    }

    public long lastAcccessTime() {
        return lastAccessTime;
    }

    public ClientSession getSession() {
        return session;
    }
}
