public class MemTest extends UserProcess{
private int id;
private int pages;
public MemTest(int id, int pages){
    super("MemTest" + id);
        this.id = id;
        this.pages = pages;
}

@Override
public void run() {
    waitForSchedule();

    
    sendMessage(new Message(0, KernelMessageTypes.allocate.ordinal(), pages));
    int base = (int) getMessage().getData()[0];
    if (base < 0) {
        System.out.println(getName() + ": allocate failed");
        sendMessage(new Message(0, KernelMessageTypes.exit.ordinal()));
        return;
    }

    
    int size = pages * MainMemory.BLOCK_SIZE;
    int errors = 0;

    for (int round = 0; round < 2000; round++) {
    for (int i = 0; i < size; i++) {
        store(base + i, id * 100000000 + round * 10000 + i);
        cooperate();
    }
    for (int i = 0; i < size; i++) {
        if (load(base + i) != id * 100000000 + round * 10000 + i) {
            errors++;
        }
        cooperate();
    }
    if (round % 100 == 0) {
        System.out.println(getName() + ": round " + round);
    }
}
    
    System.out.println(getName() + ": " + (errors == 0 ? "PASS" : "FAIL, errors = " + errors));
    sendMessage(new Message(0, KernelMessageTypes.free.ordinal(), base));
    sendMessage(new Message(0, KernelMessageTypes.exit.ordinal()));
}
}