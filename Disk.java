import java.io.RandomAccessFile;
import java.io.IOException;

public class Disk extends UserProcess {

    private static final int BLOCK_SIZE = 4096;
    private static final int NUM_BLOCKS = 100;
    private RandomAccessFile diskFile;

    public Disk() {
        super("Disk");
        try {
            diskFile = new RandomAccessFile("disk.img", "rw");
            diskFile.setLength((long) NUM_BLOCKS * BLOCK_SIZE);
        } catch (IOException e) {
            System.out.println("Disk: could not open disk.img: " + e.getMessage());
        }
    }

    @Override
    public void run() {
        waitForSchedule();

        while (true) {
            Message request = getMessage();
            int type = request.getWhat();

            if (type == DiskMessageTypes.Read.ordinal()) {

                handleRead(request);
            } else if (type == DiskMessageTypes.Write.ordinal()) {
                handleWrite(request);
            } else if (type == DiskMessageTypes.GetSize.ordinal()) {
                handleGetSize(request);
            } else {
                sendError(request, " unknown request type");
            }
            cooperate();
        }
    }

    private void sendError(Message request, String reason) {
        sendMessage(new Message(request.senderPid, DiskMessageTypes.Error.ordinal(), reason));
    }

    private void handleGetSize(Message request) {
        sendMessage(new Message(request.senderPid, DiskMessageTypes.GetSize.ordinal(), NUM_BLOCKS));
    }

    private void handleRead(Message request) {
        int block = (int) request.getData()[0];
        if (block < 0 || block >= NUM_BLOCKS) {
            sendError(request, "Block out of range");
            return;
        }
        try {
            byte[] data = new byte[BLOCK_SIZE];
            diskFile.seek((long) block * BLOCK_SIZE);
            diskFile.readFully(data);
            sendMessage(new Message(request.senderPid, DiskMessageTypes.Read.ordinal(), data));
        } catch (IOException e) {
            sendError(request, "Disk read failed: " + e.getMessage());
        }
    }

    private void handleWrite(Message request) {
        Object[] args = request.getData();

        if (args.length < 2 || !(args[1] instanceof byte[])) {
            sendError(request, "Write needs a block number and a byte[]");
            return;
        }
        int block = (int) args[0];
        byte[] data = (byte[]) args[1];

        if (block < 0 || block >= NUM_BLOCKS) {
            sendError(request, "Block out of range");
            return;
        }

        if (data.length != BLOCK_SIZE) {
            sendError(request, "Data must be exactly " + BLOCK_SIZE + " bytes");
            return;
        }

        try {
            diskFile.seek((long) block * BLOCK_SIZE);
            diskFile.write(data);

        } catch (IOException e) {

            sendError(request, "Disk write failed: " + e.getMessage());
        }

    }
}