This package contains most of the code that wraps the ssh client implementation.

The interesting classes are:

# SshSessionPool

A pool of sshHosts, so that it's easy to get a session from an already known host.

# SshHost

A host with a pool of SshConnections that can be added to and handed out for reuse.

# SshConnection

An ssh connection to an SshHost, which can be used by several SshSession's in turn, one at a time.

# SshSession

A higher level wrapper around SshConnection, which is again wrapped by SVNSSHConnector elsewhere.


