package org.tmatesoft.svn.core.internal.io.svn.ssh;

import org.tmatesoft.svn.core.auth.ISVNSSHHostVerifier;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class SshHost {

    private static final int CONNECTION_INACTIVITY_TIMEOUT = Integer.parseInt(System.getProperty("svnkit.ssh.connection.inactivity.timeout.secs", "600")) * 1000; // 10 minutes
    private static final int MAX_CONCURRENT_OPENERS = Integer.parseInt(System.getProperty("svnkit.ssh.max.concurrent.connection.openers", "3"));
    private static final int MAX_SESSIONS_PER_CONNECTION = Integer.parseInt(System.getProperty("svnkit.ssh.max.sessions.per.connection", "8"));

    private final String hostName;
    private final int port;

    private char[] privateKey;
    private char[] privateKeyPassphrase;
    private char[] password;
    private String userName;

    private int myConnectTimeout;
    private boolean locked;
    private boolean disposed;

    private final List<SshConnection> connections = new ArrayList<>();
    final private Object OPENER_LOCK = new Object();
    private int openersCount;
    private int readTimeout;
    private ISVNSSHHostVerifier hostVerifier;

    public SshHost(String hostName, int port) {
        this.hostName = hostName;
        this.port = port;
    }

    public void setConnectionTimeout(int timeout) {
        myConnectTimeout = timeout;
    }

    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }

    public void setCredentials(String userName, char[] privateKey, char[] privateKeyPassphrase, char[] password) {
        this.userName = userName;
        this.privateKey = privateKey;
        this.privateKeyPassphrase = privateKeyPassphrase;
        this.password = password;
    }

    public boolean purge() {
        try {
            lock();
            int size = connections.size();
            long time = System.currentTimeMillis();
            for (Iterator<SshConnection> connections = this.connections.iterator(); connections.hasNext(); ) {
                SshConnection connection = connections.next();
                if (connection.getSessionsCount() == 0) {
                    if (this.connections.size() == 1) {
                        long timeout = time - connection.lastAcccessTime();
                        if (timeout >= CONNECTION_INACTIVITY_TIMEOUT) {
                            connection.close();
                            connections.remove();
                        }
                    } else {
                        connection.close();
                        connections.remove();
                    }
                }
            }
            if (connections.size() == 0 && size > 0) {
                setDisposed(true);
            }
            return isDisposed();
        } finally {
            unlock();
        }

    }

    public boolean isDisposed() {
        return disposed;
    }

    public void setDisposed(boolean disposed) {
        this.disposed = disposed;
        if (disposed) {
            for (SshConnection connection : connections) {
                connection.close();
            }
            connections.clear();
        }
    }

    public String getKey() {
        String key = userName + ":" + hostName + ":" + port;
        if (privateKey != null) {
            key += ":" + new String(privateKey);
        }
        if (privateKeyPassphrase != null) {
            key += ":" + new String(privateKeyPassphrase);
        }
        if (password != null) {
            key += ":" + new String(password);
        }
        return key;
    }

    void lock() {
        synchronized (connections) {
            while (locked) {
                try {
                    connections.wait();
                } catch (InterruptedException e) {
                }
            }
            locked = true;
        }
    }

    void unlock() {
        synchronized (connections) {
            locked = false;
            connections.notifyAll();
        }
    }

    public SshSession openSession() throws IOException {
        SshSession session = useExistingConnection();
        if (session != null) {
            return session;
        }
        SshConnection newConnection = null;
        addOpener();
        try {
            session = useExistingConnection();
            if (session != null) {
                return session;
            }
            newConnection = openConnection();
        } finally {
            removeOpener();
        }

        if (newConnection != null) {
            lock();
            try {
                if (isDisposed()) {
                    newConnection.close();
                    throw new SshHostDisposedException();
                }
                connections.add(newConnection);
                return newConnection.openSession();
            } finally {
                unlock();
            }
        }
        throw new IOException("Cannot establish SSH connection with " + hostName + ":" + port);
    }

    private SshSession useExistingConnection() throws IOException {
        lock();
        try {
            if (isDisposed()) {
                throw new SshHostDisposedException();
            }
            for (Iterator<SshConnection> connections = this.connections.iterator(); connections.hasNext(); ) {
                final SshConnection connection = connections.next();

                if (connection.getSessionsCount() < MAX_SESSIONS_PER_CONNECTION) {
                    try {
                        return connection.openSession();
                    } catch (IOException e) {
                        // this connection has been closed by server.
                        if (e.getMessage() != null && e.getMessage().contains("connection is closed")) {
                            connection.close();
                            connections.remove();
                        } else {
                            throw e;
                        }
                    }
                }
            }
        } finally {
            unlock();
        }
        return null;
    }


    private void removeOpener() {
        synchronized (OPENER_LOCK) {
            openersCount--;
            OPENER_LOCK.notifyAll();
        }
    }

    private void addOpener() {
        synchronized (OPENER_LOCK) {
            while (openersCount >= MAX_CONCURRENT_OPENERS) {
                try {
                    OPENER_LOCK.wait();
                } catch (InterruptedException e) {
                }
            }
            openersCount++;
        }
    }

    private SshConnection openConnection() throws IOException {
        try {
            return new SshConnection(this);
        } catch (Exception e) {
            throw new IOException("Failed to connect to "+getKey(), e);
        }
    }

    public String toString() {
        return userName + "@" + hostName + ":" + port + ":" + connections.size();
    }

    public void setHostVerifier(ISVNSSHHostVerifier hostVerifier) {
        this.hostVerifier = hostVerifier;
    }

    public ISVNSSHHostVerifier getHostVerifier() {
        return hostVerifier;
    }

    public byte[] getPrivateKey() {
        if (privateKey != null) {
            return new String(privateKey).getBytes(StandardCharsets.UTF_8);
        } else {
            return null;
        }
    }

    public String getHostName() {
        return hostName;
    }

    public int getPort() {
        return port;
    }

    public String getUserName() {
        return userName;
    }

    public String getPassword() {
        if (password != null) {
            return new String(password);
        } else {
            return null;
        }

    }

    public String getPrivateKeyPassphrase() {
        if (privateKeyPassphrase != null) {
            return new String(privateKeyPassphrase);
        } else {
            return null;
        }
    }
}
