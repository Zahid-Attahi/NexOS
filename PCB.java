import java.util.LinkedList;

public class PCB {

    public record allocation(int vAddress, int[] pages) {}
    private static int nextPid;
    protected int pid;
    protected Process process;
    protected LinkedList<Message> messages;
    protected ProcessState state;
    protected LinkedList<allocation> allocations;
    protected int nextVirtualAddress;

    public PCB(Process process) {
        this.process = process;
        this.pid = nextPid;
        nextPid++;
        messages = new LinkedList<>();
        state = ProcessState.Runnable;
        allocations = new LinkedList<>();
        nextVirtualAddress = 0;
    }
}
