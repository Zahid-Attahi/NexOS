public class Idle extends UserProcess {

    public Idle() {
        super("Idle");
    }

    @Override
    public void run() {
          waitForSchedule();
        while (true) { 
            cooperate();
        
        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
}