import java.util.function.BiConsumer;
import java.util.function.Function;
public class Cache{

    private static class Line {
    boolean valid;
    boolean dirty;
    int baseAddress;
    int[] data;
}

private Line[] lines;
private int ways;
private int numSets;
private int[] nextVictim;
private Function<Integer, int[]> nextLoad;
private BiConsumer<Integer, int[]> nextStore;

public Cache(int numLines, int ways, Function<Integer, int[]> nextLoad, BiConsumer<Integer, int[]> nextStore){
    this.ways=ways;
    this.numSets=numLines/ways;
    this.nextLoad=nextLoad;
    this.nextStore=nextStore; 
    lines=new Line[numLines];
    for(int i=0; i<lines.length; i++) {
        lines[i]=new Line();
       
    }
     nextVictim=new int[numSets];
}
private int setOf(int baseAddress) {
    return (baseAddress / MainMemory.CACHE_LINE_SIZE) % numSets;
}
private int findLine(int baseAddress){
    int first=setOf(baseAddress) * ways;
    for(int i=first; i<first+ways; i++){
        if(lines[i].valid && lines[i].baseAddress==baseAddress){
            return i;
        }
        
    }
    return -1;
}
private int fill(int baseAddress){
    int set=setOf(baseAddress);
    int victim=set*ways+nextVictim[set];
    nextVictim[set]=(nextVictim[set]+1)% ways;
    Line line=lines[victim];
    if(line.valid&& line.dirty){
        nextStore.accept(line.baseAddress, line.data);

    }
    line.data=nextLoad.apply(baseAddress);
line.baseAddress=baseAddress;
line.valid=true;
line.dirty=false;
return victim;
}


public int load(int address){
    int base=address-address%MainMemory.CACHE_LINE_SIZE;
    int offset=address%MainMemory.CACHE_LINE_SIZE;
    int index=findLine(base);
    if(index<0){
        index=fill(base);

    }
    return lines[index].data[offset];
}

public void store(int address, int value){
    int base=address-address% MainMemory.CACHE_LINE_SIZE;
    int offset=address%MainMemory.CACHE_LINE_SIZE;
    int index=findLine(base);
    if(index<0){
        index=fill(base);
    }
     lines[index].data[offset] = value;
    lines[index].dirty = true;
}
public int[] loadLine(int baseAddress){
    int index=findLine(baseAddress);
    if(index<0){
        index=fill(baseAddress);

    }
    return lines[index].data.clone();
}

public void storeLine(int baseAddress, int[] value){
    int index=findLine(baseAddress);
    if(index<0){
        index=fill(baseAddress);
    }
    lines[index].data=value.clone();
    lines[index].dirty=true;
}

public void flush(){
    for(int i=0; i<lines.length; i++){
        Line line=lines[i];
        if(line.valid&& line.dirty){
nextStore.accept(line.baseAddress, line.data);

        }
        line.valid=false;
        line.dirty=false;
    }
    for (int s = 0; s < nextVictim.length; s++) {
        nextVictim[s] = 0;
    }
}
}