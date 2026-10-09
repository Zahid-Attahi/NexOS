import java.util.LinkedList;

public class PCB {
    private static int nextPid;
    protected int pid;
    protected Process process;
    protected LinkedList<Message> messages;
    protected ProcessState state;


    public PCB(Process process) {
        this.process=process;
        this.pid=nextPid;
        nextPid++;
        messages = new LinkedList<>();
        state=ProcessState.Runnable;
    }
}