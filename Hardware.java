public class Hardware{

    public record mapping(int vAddress, int pAddress) {}

    private static final int L1_LINES = 8;
private static final int L1_WAYS = 8;
private static final int L2_LINES = 32;
private static final int L2_WAYS = 2;
private static final int L3_LINES = 64;
private static final int L3_WAYS = 1;
private static final int TLB_SIZE = 2;

private static Hardware instance = new Hardware();
private MainMemory memory;
private Cache l1;
private Cache l2;
private Cache l3;

private mapping[] mappings;
private int[] tlbVirtual;
private int[] tlbPhysical;
private boolean[] tlbValid;
private int nextTlbVictim;


private Hardware(){
memory=new MainMemory();

l3 = new Cache(L3_LINES, L3_WAYS, memory::loadLine, memory::storeLine);
    l2 = new Cache(L2_LINES, L2_WAYS, l3::loadLine, l3::storeLine);
    l1 = new Cache(L1_LINES, L1_WAYS, l2::loadLine, l2::storeLine);
    mappings=new mapping[0];
    tlbVirtual = new int[TLB_SIZE];
    tlbPhysical = new int[TLB_SIZE];
    tlbValid = new boolean[TLB_SIZE];
}
public static Hardware getInstance() {
    return instance;
}

private int translate(int vaddress) throws MemoryAccessViolation{
    if(vaddress<0)
    {
        throw new MemoryAccessViolation("Bad address " + vaddress);
    }
int page=vaddress-vaddress% MainMemory.BLOCK_SIZE;
int offset=vaddress% MainMemory.BLOCK_SIZE;

for(int i=0; i<TLB_SIZE; i++){
    if(tlbValid[i]&& tlbVirtual[i]==page){

        return tlbPhysical[i] + offset;
    }
}

for(int i=0; i<mappings.length; i++){
    if(mappings[i].vAddress()==page){
int slot = nextTlbVictim;
 nextTlbVictim = (nextTlbVictim + 1) % TLB_SIZE;
            tlbVirtual[slot] = page;
            tlbPhysical[slot] = mappings[i].pAddress();
            tlbValid[slot] = true;
            return mappings[i].pAddress() + offset;
    }

}
throw new MemoryAccessViolation("No mapping for virtual address " + vaddress);
}
public synchronized int load(int vaddress) throws MemoryAccessViolation {
    return l1.load(translate(vaddress));
}
public synchronized void store(int vaddress, int value) throws MemoryAccessViolation {
    l1.store(translate(vaddress), value);
}

public synchronized void remap(mapping[] newOne) {
    l1.flush();
    l2.flush();
    l3.flush();

    for (int i = 0; i < TLB_SIZE; i++) {
        tlbValid[i] = false;
    }
    nextTlbVictim = 0;

    mappings = newOne;
}
}