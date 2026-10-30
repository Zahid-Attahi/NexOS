import java.util.Arrays;

public class DiskTest extends UserProcess {

    public DiskTest() {
        super("DiskTest");
    }

    @Override
    public void run() {
        waitForSchedule();
        sendMessage(new Message(0, KernelMessageTypes.locate.ordinal(), "Disk"));
        int diskPid = (int) getMessage().getData()[0];

        if (diskPid < 0) {

            System.out.println("DiskTest: Disk not found");
            sendMessage(new Message(0, KernelMessageTypes.exit.ordinal()));

            return;
        }
        System.out.println("DiskTest: found Disk, pid " + diskPid);
        sendMessage(new Message(diskPid, DiskMessageTypes.GetSize.ordinal()));
        Message sizeReply = getMessage();

        if (sizeReply.getWhat() == DiskMessageTypes.GetSize.ordinal()) {
            System.out.println("DiskTest: disk size = " + (int) sizeReply.getData()[0] + " blocks");

        } else {
            System.out.println("DiskTest: GetSize FAILED: " + sizeReply.getData()[0]);

        }

        int[] blocks = { 0, 5, 99 };

        for (int i = 0; i < blocks.length; i++) {
            byte[] data = new byte[4096];
            Arrays.fill(data, (byte) (blocks[i] + 1));

            sendMessage(new Message(diskPid, DiskMessageTypes.Write.ordinal(), blocks[i], data));

            System.out.println("DiskTest: wrote block " + blocks[i]);
            cooperate();

        }

        for (int i = 0; i < blocks.length; i++) {
            sendMessage(new Message(diskPid, DiskMessageTypes.Read.ordinal(), blocks[i]));
            Message readReply = getMessage();

            if (readReply.getWhat() == DiskMessageTypes.Read.ordinal()) {
                byte[] actual = (byte[]) readReply.getData()[0];

                byte[] data2 = new byte[4096];
                Arrays.fill(data2, (byte) (blocks[i] + 1));

                if (Arrays.equals(data2, actual)) {
                    System.out.println("DiskTest: block " + blocks[i] + " PASS");
                } else {
                    System.out.println("DiskTest: block " + blocks[i] + " FAIL");
                }
            } else {
                System.out.println("DiskTest: Read FAILED: " + readReply.getData()[0]);
            }

            cooperate();
        }

        sendMessage(new Message(diskPid, DiskMessageTypes.Read.ordinal(), 100));
        Message badBlockReply = getMessage();

        if (badBlockReply.getWhat() == DiskMessageTypes.Error.ordinal()) {
            System.out.println("DiskTest: bad block PASS, Disk said: " + badBlockReply.getData()[0]);

        } else {
            System.out.println("DiskTest: bad block FAIL, Disk accepted block 100");

        }

        sendMessage(new Message(diskPid, DiskMessageTypes.Write.ordinal(), 3, new byte[10]));
        Message badSizeReply = getMessage();

        if (badSizeReply.getWhat() == DiskMessageTypes.Error.ordinal()) {
            System.out.println("DiskTest: bad size PASS, Disk said: " + badSizeReply.getData()[0]);

        } else {
            System.out.println("DiskTest: bad size FAIL, Disk sent: " + badSizeReply.getWhat());
        }

                sendMessage(new Message(diskPid, DiskMessageTypes.Write.ordinal(), 100, new byte[4096]));
        System.out.println("DiskTest: write out of range " + (getMessage().getWhat() == DiskMessageTypes.Error.ordinal() ? "PASS" : "FAIL"));

        sendMessage(new Message(diskPid, DiskMessageTypes.Write.ordinal(), 3));
        System.out.println("DiskTest: write missing data " + (getMessage().getWhat() == DiskMessageTypes.Error.ordinal() ? "PASS" : "FAIL"));

        sendMessage(new Message(diskPid, DiskMessageTypes.Read.ordinal(), -1));
        System.out.println("DiskTest: read negative block " + (getMessage().getWhat() == DiskMessageTypes.Error.ordinal() ? "PASS" : "FAIL"));

        sendMessage(new Message(diskPid, DiskMessageTypes.Read.ordinal(), "zero"));
        System.out.println("DiskTest: read bad argument " + (getMessage().getWhat() == DiskMessageTypes.Error.ordinal() ? "PASS" : "FAIL"));
       
        sendMessage(new Message(0, KernelMessageTypes.exit.ordinal()));
    }
}