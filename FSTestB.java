public class FSTestB extends UserProcess {

    public FSTestB() {
        super("FSTestB");
    }

    @Override
    public void run() {
        waitForSchedule();
        sendMessage(new Message(0, KernelMessageTypes.locate.ordinal(), "FileSystem"));
        int fsPid = (int) getMessage().getData()[0];

        sendMessage(new Message(fsPid, FileSystemMessageTypes.LIST.ordinal(), "."));
        Message reply = getMessage();

        if (reply.getWhat() == FileSystemMessageTypes.ERROR.ordinal()) {
            System.out.println("FSTestB: List . got error: " + reply.getData()[0] + " FAIL");
        } else {
            String[] lines = (String[]) reply.getData()[0];
            System.out.println("FSTestB: List . ->");
            for (String line : lines) {
                System.out.println("    " + line);
            }
            boolean ok = lines.length == 1 && lines[0].startsWith("DIR shared ");
            System.out.println("FSTestB: my cwd is root, not /shared " + (ok ? "PASS" : "FAIL"));
        }

        sendMessage(new Message(0, KernelMessageTypes.exit.ordinal()));
    }
}