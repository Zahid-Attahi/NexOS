public class GoodByeWorld extends UserProcess {

    public GoodByeWorld() {
        super("GoodByeWorld");

    }

    public void run() {
        waitForSchedule();
        Message message = new Message(0, KernelMessageTypes.locate.ordinal(), "Console");
        sendMessage(message);

        Message msg = getMessage();
        int ConsolePid = (int) msg.getData()[0];
        System.out.println("GoodbyeWorld Located Console, pid:" + ConsolePid);

        for (int i = 1; i < 100; i++) {
            Message msg1 = new Message(ConsolePid, 0, "Goodbye World " + i);
            sendMessage(msg1);
            cooperate();
        }
        sendMessage(new Message(0, KernelMessageTypes.exit.ordinal()));
    }
}
