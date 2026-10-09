public class Console extends UserProcess {



    public Console( ) {
        super("Console");
    }


    public void run() {
         waitForSchedule();
        while (true) { 
            
            Message message=getMessage();
            System.out.println(message.getData()[0]);
            cooperate();
        }
       
    }
}