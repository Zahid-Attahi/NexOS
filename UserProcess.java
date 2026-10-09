import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class UserProcess extends Process {

      private Semaphore semaphore;
      private  volatile boolean isTimeUp;
      protected volatile Message forKernel;
      private ScheduledExecutorService timer;

    public UserProcess(String name) {
        
        super(name);
        isTimeUp = false;
        semaphore= new Semaphore(0);
        timer=Executors.newScheduledThreadPool(1);
    }


  public void release() {
    isTimeUp=false;
    semaphore.release();
    timer.schedule(() -> isTimeUp = true, 100, TimeUnit.MILLISECONDS);
  }

  public void cooperate() {
    if (isTimeUp) {
        ProcessState reason = ProcessState.QuantumExpired;
        forKernel = new Message(0, KernelMessageTypes.reschedule.ordinal(), reason);

        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
@Override
protected void sendMessage(Message m) {
    while (!outgoing.writeMessage(m)) {
        forKernel = new Message(0, KernelMessageTypes.reschedule.ordinal(),
                ProcessState.OutboxFull);

        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

@Override
protected Message getMessage() {
    Optional<Message> message = incoming.getMessage();

    while (message.isEmpty()) {
        forKernel = new Message(0, KernelMessageTypes.reschedule.ordinal(),
                ProcessState.InboxEmpty);

        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        message = incoming.getMessage();
    }

    return message.get();
}


protected void waitForSchedule() {
    try {
        semaphore.acquire();
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    }
}
protected Message getForKernel() {
    return forKernel;
}

}