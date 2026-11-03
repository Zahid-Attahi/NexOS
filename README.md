#  Microkernel

A small educational microkernel written in Java. Part 3 adds a **Disk** user process backed by a real file on the host machine, and an in-memory **File System** user process that clients talk to purely through messages.

Everything in the system is a user process communicating via the kernel's `MessageExchange`. There are no direct method calls between processes.

## What's new in Part 3

- **Disk**: a user process that wraps a `RandomAccessFile` and serves read/write requests over messages.
- **FileSystem**: a user process that keeps an in-memory tree of files and directories, with a per-process current directory.
- **Tests**: `DiskTest`, `FSTest`, and `FSTestB`, all started from `Init`.

Parts 1 and 2 (kernel, `MessageExchange`, round-robin cooperative scheduling with a quantum timer, `Idle`, `Init`) are unchanged in design and are what this builds on.

## Architecture

```
                 +-----------+
                 |   Init    |  starts everything
                 +-----+-----+
                       |
     +-----------+-----+------+-------------+
     |           |            |             |
+----v---+  +----v----+  +----v-----+  +----v-----+
|  Disk  |  | DiskTest|  |FileSystem|  | FSTest/B |
+--------+  +---------+  +----------+  +----------+
     ^           |            ^             |
     +-----------+            +-------------+
        messages via Kernel / MessageExchange
```

## Files

| File | Role |
|---|---|
| `DiskMessageTypes.java` | Message type constants for the Disk process |
| `Disk.java` | User process: handles disk requests using a `RandomAccessFile`, validates arguments |
| `DiskTest.java` | Exercises the Disk, including bad-argument cases |
| `FileSystemMessageTypes.java` | Message type constants (ALL_CAPS), including `ERROR` |
| `Node.java` | Abstract base for file system entries: `name`, `created`, `lastModified`, `touch()` |
| `File.java` | Extends `Node`; holds a `byte[]` of data, size in blocks computed on demand |
| `Directory.java` | Extends `Node`; child list, `parent`, `findChild`, `addChild`, `removeChild` |
| `FileSystem.java` | User process: dispatches requests and implements all file system operations |
| `FSTest.java` | End-to-end tests for create, list, read, write, remove, current directory, and error cases |
| `FSTestB.java` | Second process used to verify current directory is tracked per process |
| `Init.java` | Updated to start `Disk`, `DiskTest`, `FileSystem`, and the test processes |

## File system operations

Clients send a message to `FileSystem` and get a reply message back.

| Operation | Arguments | Reply |
|---|---|---|
| Set directory | path | silent on success |
| Add file | path, `byte[]` | silent on success |
| Add directory | path | silent on success |
| Remove file | path | silent on success |
| Remove directory | path | silent on success |
| List | path | one message, one `String` per entry (returned as a `String[]`) |
| Read file | path | file contents |
| Write file | path, `byte[]` | silent on success |

Failures reply with an `ERROR` message instead of crashing the process.

### Behavior notes

- **Per-process current directory.** Relative paths resolve against the sender's own current directory.
- **Removing a directory you're standing in** moves that process up to the parent directory.
- **Errors are replies, not exceptions.** Bad paths, duplicate names, wrong node type, and removing non-existent entries all return `ERROR`.

## Build and run

```bash
javac *.java
java <MainClass>
```

Replace `<MainClass>` with your project's entry point. On startup, `Init` launches the Disk, FileSystem, and test processes. Each test prints `PASS` or `FAIL` per case and sends an exit message when it finishes.

## Test coverage

- **DiskTest**: normal reads and writes plus invalid argument handling.
- **FSTest Part A/B**: create, list, read, write, and 9 error cases.
- **FSTest Part C**: current directory, remove, and a two-process cwd test via `FSTestB`.

All tests pass on the final run.

# Known limitations
- **File system is in memory only. Contents are lost when the kernel exits.
- **Disk and file system are not connected: files don't persist to the RandomAccessFile.
- **Scheduling is cooperative, so a process that never calls cooperate() can starve others.





