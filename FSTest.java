public class FSTest extends UserProcess {
    private int fsPid;

    public FSTest() {
        super("FSTest");
    }

        private void send(FileSystemMessageTypes type, Object a) {
        sendMessage(new Message(fsPid, type.ordinal(), a));
    }

    private void send(FileSystemMessageTypes type, Object a, Object b) {
        sendMessage(new Message(fsPid, type.ordinal(), a, b));
    }

    private void check(String label, boolean ok) {
        System.out.println("FSTest: " + label + (ok ? " PASS" : " FAIL"));
    }

        private String[] list(String path) {
        send(FileSystemMessageTypes.LIST, path);
        Message reply = getMessage();
        if (reply.getWhat() == FileSystemMessageTypes.ERROR.ordinal()) {
            System.out.println("FSTest: List " + path + " got error: " + reply.getData()[0]);
            return null;
        }
        String[] lines = (String[]) reply.getData()[0];
        System.out.println("FSTest: List " + path + " ->");
        for (String line : lines) {
            System.out.println("    " + line);
        }
        return lines;
    }

    private String readFile(String path) {
        send(FileSystemMessageTypes.READ_FILE, path);
        Message reply = getMessage();
        if (reply.getWhat() == FileSystemMessageTypes.ERROR.ordinal()) {
            System.out.println("FSTest: ReadFile " + path + " got error: " + reply.getData()[0]);
            return null;
        }
        return new String((byte[]) reply.getData()[0]);
    }

    private void expectError(String label) {
        Message reply = getMessage();
        if (reply.getWhat() == FileSystemMessageTypes.ERROR.ordinal()) {
            System.out.println("FSTest: " + label + " PASS, FileSystem said: " + reply.getData()[0]);
        } else {
            System.out.println("FSTest: " + label + " FAIL, expected an error");
        }
    }

    private boolean has(String[] lines, String prefix) {
        if (lines == null) {
            return false;
        }
        for (String line : lines) {
            if (line.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void run() {
        waitForSchedule();
        sendMessage(new Message(0, KernelMessageTypes.locate.ordinal(), "FileSystem"));
        fsPid = (int) getMessage().getData()[0];

        if (fsPid < 0) {
            System.out.println("FSTest: FileSystem not found");
            sendMessage(new Message(0, KernelMessageTypes.exit.ordinal()));
            return;
        }
        System.out.println("FSTest: found FileSystem, pid " + fsPid);

        // ---- create ----
        send(FileSystemMessageTypes.ADD_DIRECTORY, "/docs");
        send(FileSystemMessageTypes.ADD_DIRECTORY, "/docs/sub");
        send(FileSystemMessageTypes.ADD_FILE, "/docs/a.txt", "hello".getBytes());
        send(FileSystemMessageTypes.ADD_FILE, "b.txt", "world".getBytes());

        // ---- list ----
        String[] rootList = list("/");
        check("list / shows docs and b.txt", rootList != null && rootList.length == 2
                && has(rootList, "DIR docs ") && has(rootList, "FILE b.txt "));
        String[] docsList = list("/docs");
        check("list /docs shows a.txt and sub", docsList != null && docsList.length == 2
                && has(docsList, "FILE a.txt ") && has(docsList, "DIR sub "));

        // ---- read / write ----
        check("read a.txt", "hello".equals(readFile("/docs/a.txt")));
        send(FileSystemMessageTypes.WRITE_FILE, "/docs/a.txt", "changed!".getBytes());
        check("write then read a.txt", "changed!".equals(readFile("/docs/a.txt")));
        check("read b.txt by relative path", "world".equals(readFile("b.txt")));

        // ---- errors ----
        send(FileSystemMessageTypes.ADD_FILE, "/docs/a.txt", "x".getBytes());
        expectError("add duplicate file");
        send(FileSystemMessageTypes.ADD_DIRECTORY, "/docs");
        expectError("add duplicate directory");
        send(FileSystemMessageTypes.ADD_FILE, "/nope/x.txt", "x".getBytes());
        expectError("add into missing directory");
        send(FileSystemMessageTypes.ADD_FILE, "/docs/a.txt/x.txt", "x".getBytes());
        expectError("add inside a file");
        send(FileSystemMessageTypes.READ_FILE, "/nope.txt");
        expectError("read missing file");
        send(FileSystemMessageTypes.READ_FILE, "/docs");
        expectError("read a directory");
        send(FileSystemMessageTypes.WRITE_FILE, "/nope.txt", "x".getBytes());
        expectError("write missing file");
        send(FileSystemMessageTypes.LIST, "/docs/a.txt");
        expectError("list a file");
        send(FileSystemMessageTypes.LIST, "/nope");
        expectError("list missing path");

                // ---- current directory ----
        send(FileSystemMessageTypes.SET_DIRECTORY, "/docs");
        String[] cwdList = list(".");
        check("list . after SetDirectory /docs", cwdList != null && cwdList.length == 2
                && has(cwdList, "FILE a.txt ") && has(cwdList, "DIR sub "));
        check("read a.txt by relative path", "changed!".equals(readFile("a.txt")));
        send(FileSystemMessageTypes.SET_DIRECTORY, "sub");
        String[] upList = list("..");
        check("list .. from /docs/sub shows docs", upList != null && upList.length == 2
                && has(upList, "FILE a.txt ") && has(upList, "DIR sub "));
        send(FileSystemMessageTypes.SET_DIRECTORY, "/nope");
        expectError("set directory to missing path");
        send(FileSystemMessageTypes.SET_DIRECTORY, "/docs/a.txt");
        expectError("set directory to a file");
        send(FileSystemMessageTypes.SET_DIRECTORY, "/");

        // ---- remove ----
        send(FileSystemMessageTypes.REMOVE_DIRECTORY, "/docs");
        expectError("remove non-empty directory");
        send(FileSystemMessageTypes.REMOVE_FILE, "/docs");
        expectError("remove file on a directory");
        send(FileSystemMessageTypes.REMOVE_DIRECTORY, "/docs/a.txt");
        expectError("remove directory on a file");
        send(FileSystemMessageTypes.REMOVE_FILE, "/nope.txt");
        expectError("remove missing file");
        send(FileSystemMessageTypes.REMOVE_DIRECTORY, "/");
        expectError("remove root");

        send(FileSystemMessageTypes.REMOVE_FILE, "/docs/a.txt");
        send(FileSystemMessageTypes.REMOVE_DIRECTORY, "/docs/sub");
        String[] emptyDocs = list("/docs");
        check("docs empty after removing a.txt and sub", emptyDocs != null && emptyDocs.length == 0);
        send(FileSystemMessageTypes.REMOVE_FILE, "b.txt");
        String[] rootAfter = list("/");
        check("root only has docs after removing b.txt", rootAfter != null && rootAfter.length == 1
                && has(rootAfter, "DIR docs "));
        send(FileSystemMessageTypes.REMOVE_DIRECTORY, "/docs");
        String[] rootEmpty = list("/");
        check("root empty after removing docs", rootEmpty != null && rootEmpty.length == 0);

                        // ---- removing the directory you're standing in moves you to its parent ----
        send(FileSystemMessageTypes.ADD_DIRECTORY, "/temp");
        send(FileSystemMessageTypes.SET_DIRECTORY, "/temp");
        send(FileSystemMessageTypes.REMOVE_DIRECTORY, "/temp");
        String[] afterTemp = list(".");
        check("removed my own cwd, now back in root", afterTemp != null && afterTemp.length == 0);
        

        // ---- two processes, different current directories ----
        send(FileSystemMessageTypes.ADD_DIRECTORY, "/shared");
        send(FileSystemMessageTypes.ADD_FILE, "/shared/x.txt", "hi".getBytes());
        send(FileSystemMessageTypes.SET_DIRECTORY, "/shared");
        sendMessage(new Message(0, KernelMessageTypes.createProcess.ordinal(), new FSTestB()));
        String[] sharedList = list(".");
        check("FSTest cwd is /shared, list . shows x.txt", sharedList != null && sharedList.length == 1
                && has(sharedList, "FILE x.txt "));
               
        System.out.println("FSTest: all tests done");
        sendMessage(new Message(0, KernelMessageTypes.exit.ordinal()));
        
    }
}