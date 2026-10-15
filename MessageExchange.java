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

                    // Safety net for lost wakeups: a process marked as waiting
                    // whose condition is already false must be made Runnable.
                    if (pcb.state == ProcessState.InboxEmpty && !process.incoming.isEmpty()) {
                        pcb.state = ProcessState.Runnable;
                    }
                    if (pcb.state == ProcessState.OutboxFull && process.outgoing.hasRoom()) {
                        pcb.state = ProcessState.Runnable;
                    }

                    // Forward any pending message for the kernel (reschedule requests)
                    if (process instanceof UserProcess) {
                        UserProcess userProcess = (UserProcess) process;
                        Message messageForKernel = userProcess.getForKernel();

                        if (messageForKernel != null) {
                            messageForKernel.senderPid = pcb.pid;
                            // only clear it if the kernel inbox accepted it
                            if (holder[0].process.incoming.writeMessage(messageForKernel)) {
                                userProcess.forKernel = null;
                            }
                        }
                    }

                    // Check the process's outgoing queue
                    Optional<Message> message = process.outgoing.getMessage();

                    if (message.isPresent()) {

                        Message actualMessage = message.get();

                        actualMessage.senderPid = pcb.pid;
                        if (pcb.state == ProcessState.OutboxFull) {
                            pcb.state = ProcessState.Runnable;
                        }

                        // Find the recipient
                        PCB recipientPCB = null;

                        for (int j = 0; j < holder.length; j++) {
                            if (holder[j] != null && holder[j].pid == actualMessage.targetPid) {
                                recipientPCB = holder[j];
                                break;
                            }
                        }

                        if (recipientPCB != null) {

                            Process recipientProcess = recipientPCB.process;

                            // Keep FIFO order: only go straight to the inbox
                            // when there is no older backlog waiting.
                            if (recipientPCB.messages.isEmpty()
                                    && recipientProcess.incoming.writeMessage(actualMessage)) {
                                if (recipientPCB.state == ProcessState.InboxEmpty) {
                                    recipientPCB.state = ProcessState.Runnable;
                                }
                            } else {
                                recipientPCB.messages.add(actualMessage);
                            }
                        }
                    }

                    // Deliver backlog oldest-first, stop at the first failure
                    while (!pcb.messages.isEmpty()) {
                        if (!process.incoming.writeMessage(pcb.messages.peekFirst())) {
                            break;
                        }
                        pcb.messages.removeFirst();
                        if (pcb.state == ProcessState.InboxEmpty) {
                            pcb.state = ProcessState.Runnable;
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
