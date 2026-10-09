import java.util.Optional;

public class MessageQueue {
    private final Message[] messages;
    private volatile int writeIndex;  
    private volatile int readIndex;   

    public MessageQueue(int capacity) {
        messages = new Message[capacity + 1];  
    }

    protected boolean writeMessage(Message m) {
        int next = (writeIndex + 1) % messages.length;
        if (next == readIndex) {
            return false;                      
        }
        messages[writeIndex] = m;
        writeIndex = next;
        return true;
    }

    protected Optional<Message> getMessage() {
        if (readIndex == writeIndex) {
            return Optional.empty();           
        }
        Message m = messages[readIndex];
        readIndex = (readIndex + 1) % messages.length;
        return Optional.of(m);
    }
}