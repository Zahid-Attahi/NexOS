public class SegFaultTest extends UserProcess{
    public SegFaultTest() {
        super("SegFaultTest");
    }

    @Override
    public void run() {
        waitForSchedule();

        sendMessage(new Message(0, KernelMessageTypes.allocate.ordinal(), 2));
        int base = (int) getMessage().getData()[0];
        if (base < 0) {
            System.out.println(getName() + ": allocate failed");
            sendMessage(new Message(0, KernelMessageTypes.exit.ordinal()));
            return;
        }
        System.out.println(getName() + ": allocated 2 pages at virtual " + base);

        store(base, 42);
        System.out.println(getName() + ": in-bounds write OK");

        store(base + 2 * MainMemory.BLOCK_SIZE, 99);

        System.out.println(getName() + ": FAIL, should have been killed");
        sendMessage(new Message(0, KernelMessageTypes.exit.ordinal()));
    }
}