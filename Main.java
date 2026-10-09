public class Main {
    public static void main(String[] args) {
        Process[] processes = {
            new Idle(),
            new Init()
        };

        new Kernel(processes);
    }
}