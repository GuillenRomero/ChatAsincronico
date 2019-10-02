package chat;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Handler;

public class Chat {


    private static HashMap<String, PrintWriter> usuarios = new HashMap<>();

 
            
    public static void main(String[] args) throws Exception {
        System.out.println("The chat server is running... ");
        ExecutorService pool = Executors.newFixedThreadPool(500);
        try (ServerSocket listener = new ServerSocket(59001)) {
            while (true) {
                pool.execute(new Handler(listener.accept()));

            }
        } catch (Exception ex) {
        }

    }

    private static class Handler implements Runnable {

        private String name;
        private Socket socket;
        private Scanner in;
        private PrintWriter out;

        public Handler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                in = new Scanner(socket.getInputStream());
                out = new PrintWriter(socket.getOutputStream(), true);

                while (true) {
                    out.println("SUBMITNAME");
                    name = in.nextLine();
                    
                   
                    
                    if (name == null ) {
                        return;
                    }
                    
                    
                    synchronized (usuarios) {
                        if (!usuarios.containsKey(name)) {
                            usuarios.put(name, out);
                           
                            break;

                        }
                    }
                }
                out.println("NAMEACCEPTED " + name);

                for (Entry<String, PrintWriter> usuario : usuarios.entrySet()) {
                    usuario.getValue().println("MESSAGE " + name + ": Connected.");
                }

                while (true) {
                    String input = in.nextLine();
                    if (input.toLowerCase().startsWith("/quit")) {
                        return;
                    }
                    if (input.toLowerCase().startsWith("/privado")) {
                        int inicio = input.indexOf(" ");
                        int fin = input.indexOf(" ", inicio + 1);
                        String a = input.substring(inicio + 1, fin);
                        if (usuarios.containsKey(a) == true) {
                          
                                usuarios.get(a).println("MESSAGE " + name + ": " + input.substring(8).substring(a.length()).substring(2));
                            
                            out.println("MESSAGE " + name + ": " + input.substring(8).substring(a.length()).substring(2));
                        } else {
                            out.println("MESSAGE " + a + " no esta conectado.");
                        }
                    }  else {
                        for (Entry<String, PrintWriter> usuario : usuarios.entrySet()) {
                        
                                usuario.getValue().println("MESSAGE " + name + ": " + input);
                      
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println(e);
            } finally {
                if (out != null) {
                    usuarios.remove(out);
                }
                if (name != null) {
                    System.out.println(" is leaving");
                    usuarios.remove(name);
                    for (Entry<String, PrintWriter> usuario : usuarios.entrySet()) {
                        usuario.getValue().println("MESSAGE " + name + " has left");
                    }
                }
                try {
                    socket.close();
                } catch (IOException e) {
                
                }
            }
        }

    }

}
