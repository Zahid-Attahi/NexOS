public class File extends Node {
    private byte[] data;

    public File(String name, byte[] data) {
        super(name);
        this.data = data;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
        touch();
    }

    public int getSizeInBlocks() {
        return (data.length + 4095) / 4096;
    }
}