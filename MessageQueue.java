import java.util.Optional;

public class MessageQueue {
    private final Message[] messages;
    private volatile int head;   // next slot to write; only the writer thread changes it
    private volatile int tail;   // next slot to read; only the reader thread changes it

    public MessageQueue(int capacity) {
        // one slot stays empty so "full" and "empty" can be told apart
        messages = new Message[capacity + 1];
    }

    protected boolean writeMessage(Message m) {
        int next = (head + 1) % messages.length;
        if (next == tail) {
            return false;                  // full, reject
        }
        messages[head] = m;
        head = next;
        return true;
    }

    protected Optional<Message> getMessage() {
        if (tail == head) {
            return Optional.empty();       // nothing to read
        }
        Message m = messages[tail];
        tail = (tail + 1) % messages.length;
        return Optional.of(m);
    }

    protected boolean isEmpty() {
        return tail == head;
    }

    protected boolean hasRoom() {
        return (head + 1) % messages.length != tail;
    }
}
