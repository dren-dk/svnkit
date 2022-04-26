This package contains the two implementations of the ssh client.
  
* trilead is abandoned and severely obsolete to the point that it doesn't work with modern OpenSSH servers.
* apache uses the apache sshd client library which is very actively developed.

The goal with the interfaces is to allow a clean switch between trilead and apache at runtime, that bit is handed
by SessionPoolFactory.

The initial default is to use trilead, to switch to apache add this jvmarg to set a system property:

```
-Dsvnkit.ssh.client=apache
```
                    

This command should always return nothing to confirm that no trilead parts have remained behind:

```
find -type f -name '*.java' | xargs grep trilead \
  | grep -v svnkit/src/main/java/org/tmatesoft/svn/core/internal/io/svn/ssh/trilead \
  | grep -v svnkit/src/main/java/org/tmatesoft/svn/core/internal/io/svn/ssh/SessionPoolFactory.java \
  | grep -v SVNClientManagerTest   
  | grep -v AgentProxy
```
                                                                                               
Note that I exclude AgentProxy, this is because uses of that interface is spread far and wide and it will take
a larger effort to eradicate it, but that can be left until the time when trilead is finally removed entirely.

The apache implementation simply ignores AgentProxy entirely.

