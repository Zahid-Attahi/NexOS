public class MainMemory{
    public static final int BLOCK_SIZE=1024;
    public static final int NUMBER_OF_BLOCKS =100;
    public static final int CACHE_LINE_SIZE=8;
    private int[] memory;

    public MainMemory(){
 memory = new int[BLOCK_SIZE * NUMBER_OF_BLOCKS];
    }

    public int[] loadLine(int baseAddress){
int[] line=new int[CACHE_LINE_SIZE];
System.arraycopy(memory, baseAddress, line, 0, CACHE_LINE_SIZE);

return line;
    }

    public void storeLine(int baseAddress, int[] value){
        System.arraycopy(value, 0,memory, baseAddress, CACHE_LINE_SIZE);
        
    }
}