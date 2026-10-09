import java.util.Optional;


public class Process  implements Runnable  {

    
    private Thread thread;
    protected MessageQueue incoming;
    protected MessageQueue outgoing;
    private String name;



    public Process(String name) {
        this.name=name;
        
        incoming=new MessageQueue(100);
        outgoing=new MessageQueue(100);
        thread=new Thread(this);
      
    }
public void start() { thread.start(); }

@Override
public void run() {
    
}

protected void sendMessage(Message m) {
    while (!outgoing.writeMessage(m)) {
       

    
    }
}

protected Message getMessage() {
    Optional<Message> message = incoming.getMessage();
    while (message.isEmpty()) {
        message = incoming.getMessage();
    }
    return message.get();
}

protected String getName() {
    return name;
}

}
