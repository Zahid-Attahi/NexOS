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

        sendMessage(new Message(0, KernelMessageTypes.exit.ordinal()));
    }
}