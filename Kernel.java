public class Kernel extends Process {
    private PCB[] processTable;
    private boolean[] pageInUse = new boolean[MainMemory.NUMBER_OF_BLOCKS];
private int runningPid = -1;

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

                
                int foundPid = -1;

                for (int i = 0; i < processTable.length; i++) {
                    if (processTable[i] != null && processTable[i].process.getName().equals(name)) {
                        foundPid = processTable[i].pid;
                        break;
                    }
                }
                sendMessage(new Message(message.senderPid, message.getWhat(), foundPid));

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
                int slot = indexOfPid(message.senderPid);
                if (slot < 0) {
                    continue;                    
                }
                ProcessState reason = (ProcessState) message.getData()[0];

                processTable[slot].state =
                    (reason == ProcessState.QuantumExpired) ? ProcessState.Runnable : reason;
                scheduleNext(slot);

          } else if (message.getWhat() == KernelMessageTypes.exit.ordinal()) {
    int slot = indexOfPid(message.senderPid);
    if (slot < 0) {
        continue;
    }
    for (PCB.allocation a : processTable[slot].allocations) {
        for (int page : a.pages()) {
            pageInUse[page] = false;
        }
    }
    processTable[slot].allocations.clear();
    int freePages = 0;
for (int p = 0; p < pageInUse.length; p++) {
    if (!pageInUse[p]) {
        freePages++;
    }
}
System.out.println("Kernel: " + processTable[slot].process.getName()
        + " exited, free pages: " + freePages + "/" + pageInUse.length);
    processTable[slot] = null;
    scheduleNext(slot);

} else if (message.getWhat() == KernelMessageTypes.allocate.ordinal()) {
    handleAllocate(message);

} else if (message.getWhat() == KernelMessageTypes.free.ordinal()) {
    handleFree(message);
}
        }
    }

    
    private int indexOfPid(int pid) {
        for (int i = 0; i < processTable.length; i++) {
            if (processTable[i] != null && processTable[i].pid == pid) {
                return i;
            }
        }
        return -1;
    }
    private Hardware.mapping[] buildMappings(PCB pcb) {
    int total = 0;
    for (PCB.allocation a : pcb.allocations) {
        total += a.pages().length;
    }
    Hardware.mapping[] result = new Hardware.mapping[total];
    int n = 0;
    for (PCB.allocation a : pcb.allocations) {
        for (int k = 0; k < a.pages().length; k++) {
            result[n] = new Hardware.mapping(
                a.vAddress() + k * MainMemory.BLOCK_SIZE,
                a.pages()[k] * MainMemory.BLOCK_SIZE);
            n++;
        }
    }
    return result;
}

    
    private void scheduleNext(int currentSlot) {
        int start = (currentSlot + 1) % processTable.length;
        for (int i = 0; i < processTable.length; i++) {
            int index = (start + i) % processTable.length;

            if (processTable[index] == null || index == 0) {
                continue;
            }
            if (processTable[index].state == ProcessState.Runnable) {
    Hardware.getInstance().remap(buildMappings(processTable[index]));
    runningPid = processTable[index].pid;
    ((UserProcess) processTable[index].process).release();
    return;
}
        }
    }
    private void handleAllocate(Message message) {
    int slot = indexOfPid(message.senderPid);
    Object[] args = message.getData();
    if (slot < 0 || args.length < 1 || !(args[0] instanceof Integer) || (int) args[0] <= 0
        || (int) args[0] > pageInUse.length) {
        sendMessage(new Message(message.senderPid, KernelMessageTypes.allocate.ordinal(), -1));
        return;
    }
    int count = (int) args[0];

    int[] found = new int[count];
    int n = 0;
    for (int p = 0; p < pageInUse.length && n < count; p++) {
        if (!pageInUse[p]) {
            found[n] = p;
            n++;
        }
    }
    if (n < count) {
        sendMessage(new Message(message.senderPid, KernelMessageTypes.allocate.ordinal(), -1));
        return;
    }

    for (int i = 0; i < count; i++) {
        pageInUse[found[i]] = true;
    }
    PCB pcb = processTable[slot];
    int vAddress = pcb.nextVirtualAddress;
    pcb.allocations.add(new PCB.allocation(vAddress, found));
    pcb.nextVirtualAddress += count * MainMemory.BLOCK_SIZE;

    if (pcb.pid == runningPid) {
        Hardware.getInstance().remap(buildMappings(pcb));
    }
    sendMessage(new Message(message.senderPid, KernelMessageTypes.allocate.ordinal(), vAddress));
}
private void handleFree(Message message) {
    int slot = indexOfPid(message.senderPid);
    Object[] args = message.getData();
    if (slot < 0 || args.length < 1 || !(args[0] instanceof Integer)) {
        return;
    }
    int vAddress = (int) args[0];
    PCB pcb = processTable[slot];

    PCB.allocation target = null;
    for (PCB.allocation a : pcb.allocations) {
        if (a.vAddress() == vAddress) {
            target = a;
            break;
        }
    }
    if (target == null) {
        return;
    }

    for (int page : target.pages()) {
        pageInUse[page] = false;
    }
    pcb.allocations.remove(target);

    if (pcb.pid == runningPid) {
        Hardware.getInstance().remap(buildMappings(pcb));
    }
}
}
