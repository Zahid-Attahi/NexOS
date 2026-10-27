public class Init extends UserProcess {
    public Init() {
        super("Init");
    }

    @Override
    public void run() {

        waitForSchedule();
        Console console=new Console();
        sendMessage(new Message(0, KernelMessageTypes.createProcess.ordinal(), console));

        HelloWorld helloWorld= new HelloWorld();
        sendMessage(new Message(0, KernelMessageTypes.createProcess.ordinal(), helloWorld));

        GoodByeWorld goodByeWorld= new GoodByeWorld();
        sendMessage(new Message(0, KernelMessageTypes.createProcess.ordinal(), goodByeWorld));

        Disk disk = new Disk();
             sendMessage(new Message(0, KernelMessageTypes.createProcess.ordinal(), disk));
        DiskTest diskTest = new DiskTest();
                sendMessage(new Message(0, KernelMessageTypes.createProcess.ordinal(), diskTest));

        sendMessage(new Message(0, KernelMessageTypes.exit.ordinal()));
    }
}
