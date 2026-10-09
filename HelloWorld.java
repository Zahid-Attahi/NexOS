public class HelloWorld extends UserProcess {

    public HelloWorld() {
        super("HelloWorld");
    }

    public void run() {

        waitForSchedule();
        Message msg=new Message(0, KernelMessageTypes.locate.ordinal(), "Console");
        sendMessage(msg);

        Message message = getMessage();
      
        int consolePid=(int) message.getData()[0];
        System.out.println("HelloWorld Located Console, pid:" + consolePid);

        for(int i=1; i<100; i++) {

            Message msge= new Message(consolePid, 0, "Hello World " + i ); 
            sendMessage(msge);
            cooperate();
        }
        sendMessage(new Message(0, KernelMessageTypes.exit.ordinal()));
    }
}
