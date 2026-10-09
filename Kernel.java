public class Kernel extends Process {
    private PCB[] processTable;

    public Kernel(Process[] process) {
        super("Kernel");

        MessageExchange exchange = new MessageExchange();
        processTable = exchange.holder;

        processTable[0] = new PCB(this);

        for (int i = 0; i < process.length; i++) {
            processTable[i + 1] = new PCB(process[i]);
            process[i].start();                   
        }

        this.start();                             
        exchange.start();                          
        scheduleNext(0);
    }

    public void run() {
        while (true) {
            Message message = getMessage();
           

            if (message.getWhat() == KernelMessageTypes.locate.ordinal()) {
                String name = (String) message.getData()[0];

                for (int i = 0; i < processTable.length; i++) {
                    if (processTable[i] != null && processTable[i].process.getName().equals(name)) {
                        sendMessage(new Message(message.senderPid, message.getWhat(), processTable[i].pid));
                        break;
                    }
                }

            } else if (message.getWhat() == KernelMessageTypes.createProcess.ordinal()) {
                Process process = (Process) message.getData()[0];

                for (int i = 0; i < processTable.length; i++) {
                    if (processTable[i] == null) {
                        processTable[i] = new PCB(process);
                        process.start();           
                        break;
                    }
                }

            } else if (message.getWhat() == KernelMessageTypes.reschedule.ordinal()) {
                int currentPid = message.senderPid;
                ProcessState reason = (ProcessState) message.getData()[0];

               
                processTable[currentPid].state =
                    (reason == ProcessState.QuantumExpired) ? ProcessState.Runnable : reason;
                scheduleNext(currentPid);

            } else if (message.getWhat() == KernelMessageTypes.exit.ordinal()) {
                int currentPid = message.senderPid;
                processTable[currentPid] = null;
                scheduleNext(currentPid);
            }
        }
    }

    private void scheduleNext(int currentPid) {
        int start = (currentPid + 1) % processTable.length;
        for (int i = 0; i < processTable.length; i++) {
            int index = (start + i) % processTable.length;

            if (processTable[index] == null || index == 0) {
                continue;
            }
            if (processTable[index].state == ProcessState.Runnable) {
                ((UserProcess) processTable[index].process).release();
                return;
            }
        }
    }
}