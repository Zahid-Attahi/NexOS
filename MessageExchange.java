import java.util.Iterator;
import java.util.Optional;

public class MessageExchange extends Process {

    protected PCB[] holder;

    public MessageExchange() {
        super("MessageExchange");
        holder = new PCB[100];
    }

    @Override
    public void run() {

        try {
            Thread.sleep(100);

            while (true) {

                for (int i = 0; i < holder.length; i++) {

                    PCB pcb = holder[i];

                    if (pcb == null) {
                        continue;
                    }

                    Process process = pcb.process;
                    if (process instanceof UserProcess) {

    UserProcess userProcess = (UserProcess) process;
    Message messageForKernel = userProcess.getForKernel();

    if (messageForKernel != null) {
        messageForKernel.senderPid = pcb.pid;
        holder[0].process.incoming.writeMessage(messageForKernel);
        userProcess.forKernel = null;
    }
}

                    // Check the process's outgoing queue
                    Optional<Message> message = process.outgoing.getMessage();

                    if (message.isPresent()) {

                        Message actualMessage = message.get();

                        // Set the sender PID
                        actualMessage.senderPid = pcb.pid;
                        if (pcb.state == ProcessState.OutboxFull) {
    pcb.state = ProcessState.Runnable;
}

                        // Find the recipient
                        PCB recipientPCB = null;

                        for (int j = 0; j < holder.length; j++) {

                            if (holder[j] != null &&
                                holder[j].pid == actualMessage.targetPid) {

                                recipientPCB = holder[j];
                                break;
                            }
                        }

                        if (recipientPCB != null) {

                            Process recipientProcess = recipientPCB.process;

                            // Try to put the message directly into the recipient's inbox
                            if (recipientProcess.incoming.writeMessage(actualMessage)) {
                if (recipientPCB.state == ProcessState.InboxEmpty) {
                  recipientPCB.state = ProcessState.Runnable;
              }
                            } else {

                                // Inbox is full, so put the message in the recipient's backlog
                                recipientPCB.messages.add(actualMessage);
                            }
                        }
                    }

                    // Try to deliver messages already waiting in this PCB's backlog
                    Iterator<Message> backlog = pcb.messages.iterator();

                    while (backlog.hasNext()) {

                        Message backlogged = backlog.next();

                        if (process.incoming.writeMessage(backlogged)) {
                    if (pcb.state == ProcessState.InboxEmpty) {
                 pcb.state = ProcessState.Runnable;
             }
                            backlog.remove();
                        }
                    }
                }

                Thread.sleep(100);
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}