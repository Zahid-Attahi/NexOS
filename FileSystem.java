
import java.util.HashMap;
import java.util.Map;

public class FileSystem extends UserProcess{
    private final Directory root;
    private final Map<Integer, Directory> currentDirs;


    public FileSystem() {
        super("FileSystem");
        this.root=new Directory("/");
        this.currentDirs=new HashMap<>();
    }


    @Override 
    public void run() {
        waitForSchedule();

        while(true) {
            Message request=getMessage();

            int type=request.getWhat();
            

             if (type == FileSystemMessageTypes.SET_DIRECTORY.ordinal()) {
                handleSetDirectory(request);
            } else if (type == FileSystemMessageTypes.ADD_FILE.ordinal()) {
                handleAddFile(request);
            } else if (type == FileSystemMessageTypes.ADD_DIRECTORY.ordinal()) {
                handleAddDirectory(request);
            } else if (type == FileSystemMessageTypes.REMOVE_FILE.ordinal()) {
                handleRemoveFile(request);
            } else if (type == FileSystemMessageTypes.REMOVE_DIRECTORY.ordinal()) {
                handleRemoveDirectory(request);
            } else if (type == FileSystemMessageTypes.LIST.ordinal()) {
                handleList(request);
            } else if (type == FileSystemMessageTypes.READ_FILE.ordinal()) {
                handleReadFile(request);
            } else if (type == FileSystemMessageTypes.WRITE_FILE.ordinal()) {
                handleWriteFile(request);
            } else {
                sendError(request, "Unknown request type");
            }

            cooperate();
        }

    }

    public void sendError(Message request, String reason) {
   sendMessage(new Message(request.senderPid, FileSystemMessageTypes.ERROR.ordinal(), reason));
    }

    private Directory getCurrentDir(int pid) {
        Directory dir=currentDirs.get(pid);
                if (dir == null) {
            dir = root;
            currentDirs.put(pid, dir);
        }
         return dir;
    }
    private Node resolve(String path, Directory start){
        if (path == null || path.isEmpty()) {
            return null;
        }
                Node current = path.startsWith("/") ? root : start;
                        String[] parts = path.split("/");

                         for (String part : parts) {
                              if (part.isEmpty() || part.equals(".")) {
                continue;
            }
              if (!(current instanceof Directory)) {
                return null;
            }
            Directory dir = (Directory) current;

             if (part.equals("..")) {
                Directory up = dir.getParent();
                current = (up == null) ? dir : up;
            } else {
                Node next = dir.findChild(part);
                if (next == null) {
                    return null;
                }
                current = next;
            }
                         }
                       return current;
    }


    private void handleSetDirectory(Message request) {
Object[] args=request.getData();

if(args.length<1 || !(args[0] instanceof String)) {
sendError(request, "SetDirectory needs a path");
            return;
}
 String path=(String) args[0];
   Node target = resolve(path, getCurrentDir(request.senderPid));

    if (target == null) {
            sendError(request, "Path not found: " + path);
            return;
        }
         if (!(target instanceof Directory)) {
            sendError(request, "Not a directory: " + path);
            return;
        }
                currentDirs.put(request.senderPid, (Directory) target);
    }

    private void handleAddFile(Message request) {
        Object[] args=request.getData();

            if (args.length < 2 || !(args[0] instanceof String) || !(args[1] instanceof byte[])) {
        sendError(request, "AddFile needs a path and data");
        return;
    
            }
            String path = (String) args[0];
                byte[] data = (byte[]) args[1];
                    Directory parent = checkCreate(request, path);
                        if (parent == null) {
        return;
    }
       parent.addChild(new File(getName(path), data.clone()));
    }

    private String getName(String path) {
    return path.substring(path.lastIndexOf("/") + 1);
}

private Directory checkCreate(Message request, String path) {
    String name = getName(path);
    if (name.isEmpty() || name.equals(".") || name.equals("..")) {
        sendError(request, "Invalid name: " + path);
        return null;
    }

    int slash = path.lastIndexOf("/");
    Directory start = getCurrentDir(request.senderPid);
    Node parent;
    if (slash < 0) {
        parent = start;
    } else if (slash == 0) {
        parent = root;
    } else {
        parent = resolve(path.substring(0, slash), start);
    }

    if (parent == null) {
        sendError(request, "Path not found: " + path);
        return null;
    }
    if (!(parent instanceof Directory)) {
        sendError(request, "Not a directory: " + path);
        return null;
    }
    Directory dir = (Directory) parent;

    if (dir.findChild(name) != null) {
        sendError(request, "Already exists: " + path);
        return null;
    }
    return dir;
}
    private void handleAddDirectory(Message request) {
        Object[] args = request.getData();
        if (args.length < 1 || !(args[0] instanceof String)) {
         sendError(request, "AddDirectory needs a path");
        return;
        }

        String path = (String) args[0];
            Directory parent = checkCreate(request, path);
                if (parent == null) {
        return;
    }
        parent.addChild(new Directory(getName(path)));
    }

private Directory checkParent(Message request, String path) {
    String name=getName(path);
    if(name.isEmpty() || name.equals(".") || name.equals("..")) {
        sendError(request, "Invalid name: " + path);
        return null;
    }
int slash = path.lastIndexOf("/");
Directory start = getCurrentDir(request.senderPid);
Node parent;

if(slash<0) {
    parent=start;
} else if(slash==0) {
    parent=root;
}else {
    parent = resolve(path.substring(0, slash), start);
}

 if (parent == null) {
        sendError(request, "Path not found: " + path);
        return null;
 }
 if (!(parent instanceof Directory)) {
        sendError(request, "Not a directory: " + path);
        return null;
    }
    return (Directory) parent;
}

private void handleRemoveFile(Message request){
    Object[] args=request.getData();

    if(args.length<1 || !(args[0] instanceof String)) {
sendError(request, "RemoveFile needs a path");
        return;
    }

     String path = (String) args[0];
     Directory parent = checkParent(request, path);
     if(parent==null) {
        return;
     }
     Node target=parent.findChild(getName(path));
     if(target==null) {
        sendError(request, "Path not found: " + path);
        return;
     }

     if(!(target instanceof File)) {
         sendError(request, "Not a file: " + path);
         return;
     }

     parent.removeChild(target);
}
private void handleRemoveDirectory(Message request) {
    Object[] args = request.getData();
    if (args.length < 1 || !(args[0] instanceof String)) {
        sendError(request, "RemoveDirectory needs a path");
        return;
    }

    String path = (String) args[0];
    Directory parent = checkParent(request, path);
    if (parent == null) {
        return;
    }

    Node target = parent.findChild(getName(path));
    if (target == null) {
        sendError(request, "Path not found: " + path);
        return;
    }
    if (!(target instanceof Directory)) {
        sendError(request, "Not a directory: " + path);
        return;
    }
    if (!((Directory) target).isEmpty()) {
        sendError(request, "Directory not empty: " + path);
        return;
    }
            for (Map.Entry<Integer, Directory> e : currentDirs.entrySet()) {
        if (e.getValue() == target) {
            e.setValue(parent);
        }
    }
    parent.removeChild(target);
}
// Reply carries the whole String[] as getData()[0]: one string per directory entry.
public void handleList(Message request) {
    Object [] args=request.getData();
    if(args.length<1 || !(args[0] instanceof String)) {
        sendError(request, "List needs a path");
        return;
    }
    String path = (String) args[0];
    Node target = resolve(path, getCurrentDir(request.senderPid));
    if (target == null) {
        sendError(request, "Path not found: " + path);
        return;
    }
    if (!(target instanceof Directory)) {
        sendError(request, "Not a directory: " + path);
        return;
    }

     Directory dir = (Directory) target;
    String[] lines = new String[dir.getChildren().size()];
    int i = 0;
    for (Node child : dir.getChildren()) {
        if (child instanceof File) {
            lines[i] = "FILE " + child.getName()
                    + " created=" + child.getCreated()
                    + " modified=" + child.getLastModified()
                    + " size=" + ((File) child).getSizeInBlocks() + " blocks";
        } else {
            lines[i] = "DIR " + child.getName()
                    + " created=" + child.getCreated()
                    + " modified=" + child.getLastModified();
        }
        i++;
    }
    sendMessage(new Message(request.senderPid, FileSystemMessageTypes.LIST.ordinal(), (Object[]) lines));
}

public void handleReadFile(Message request) {
    Object[] arg=request.getData();
    if(arg.length<1 || !(arg[0] instanceof String)) {
        sendError(request, "ReadFile needs a path");
        return;
    }

    String path=(String) arg[0];
     Node target = resolve(path, getCurrentDir(request.senderPid));
     if(target==null){
        sendError(request, "Path not found: " + path);
        return;
     }
     if(!(target instanceof File)) {
        sendError(request, "Not a file: " + path);
        return;
     }
   sendMessage(new Message(request.senderPid, FileSystemMessageTypes.READ_FILE.ordinal(), ((File) target).getData().clone()));
}

private void handleWriteFile(Message request) {
    Object[] args = request.getData();
    if (args.length < 2 || !(args[0] instanceof String) || !(args[1] instanceof byte[])) {
        sendError(request, "WriteFile needs a path and data");
        return;
    }

    String path = (String) args[0];
    byte[] data = (byte[]) args[1];
    Node target = resolve(path, getCurrentDir(request.senderPid));
    if (target == null) {
        sendError(request, "Path not found: " + path);
        return;
    }
    if (!(target instanceof File)) {
        sendError(request, "Not a file: " + path);
        return;
    }
    ((File) target).setData(data.clone());
}
}
