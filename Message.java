
import java.util.Arrays;

public class Message {
    
       protected int senderPid;
         protected int targetPid;
       private int what;
        private Object[] data;

     public Message(int targetPid, int what) {
        this.targetPid=targetPid;
        this.what=what;
        this.data= new Object[0];
     }

     public Message(int targetPid, int what, Object data){
        this.targetPid=targetPid;
        this.what=what;
        this.data= new Object[] {data };

     }

     public Message(int targetPid, int what, Object data, Object data2) {
        this.targetPid=targetPid;
        this.what=what;
        this.data=new Object[] {data, data2};
        

     }
     @Override
     public String toString() {
       
        return "senderPid=" + senderPid + " targetPid=" + targetPid + " what="+ what + " data=" + Arrays.toString(data);
    
     }
 protected int getWhat() {
    return what;
}


protected Object[] getData() {
    return data;
}
    }
   