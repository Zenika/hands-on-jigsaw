package process;

import java.io.IOException;

public class SleepProcessApi {
    public static void main(String[] args) {
        try {
            System.out.println("Current PID " + ProcessHandle.current().pid());

            Process process = new ProcessBuilder("/bin/sleep", "2").start();
            System.out.println("PID of exec prog " + process.pid());

            process.children().forEach(p -> System.out.println( p.pid()));
            System.out.println(process.waitFor());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
