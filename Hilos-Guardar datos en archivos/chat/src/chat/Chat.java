package chat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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
    private static HashMap<String, ArrayList<String>> bloqueos;
    private static HashMap<String, String> sesion;

    public static void main(String[] args) throws Exception {

        File archivoBloqueos = new File("bloqueos.txt");
        File archivoSesiones = new File("sesion.txt");

        if (archivoBloqueos.exists()) {
            ObjectInputStream entradaArchivoBloqueos = new ObjectInputStream(new FileInputStream(archivoBloqueos));
            bloqueos = (HashMap<String, ArrayList<String>>) entradaArchivoBloqueos.readObject();
            entradaArchivoBloqueos.close();
        } else {
            bloqueos = new HashMap<String, ArrayList<String>>();
        }

        if (archivoSesiones.exists()) {
            ObjectInputStream entradaArchivoSesiones = new ObjectInputStream(new FileInputStream(archivoSesiones));
            sesion = (HashMap<String,String>) entradaArchivoSesiones.readObject();
            entradaArchivoSesiones.close();
        } else {
            sesion = new HashMap<String, String>();
        }

        System.out.println("The chat server is running... ");
        ExecutorService pool = Executors.newFixedThreadPool(500);
        try (ServerSocket listener = new ServerSocket(59001)) {
            while (true) {
                pool.execute(new Handler(listener.accept()));

            }
        } catch (Exception ex) {
        }

    }

    private static void actualizarArchivoBloqueos() throws Exception {
        ObjectOutputStream salidaArchivoBloqueos = new ObjectOutputStream(new FileOutputStream("bloqueos.txt", false));
        salidaArchivoBloqueos.writeObject(bloqueos);
        salidaArchivoBloqueos.close();
    }

    private static void actualizarArchivoSesion() throws Exception {
        System.out.println(sesion);
        ObjectOutputStream salidaArchivoSesiones = new ObjectOutputStream(new FileOutputStream("sesion.txt", false));
        salidaArchivoSesiones.writeObject(sesion);
        salidaArchivoSesiones.close();
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

                    if (name == null) {
                        return;
                    }

                    out.println("SUBMITPASSWORD");
                    String contra = in.nextLine();
                    if (contra == null) {
                        return;
                    }

                    synchronized (usuarios) {
                        synchronized (bloqueos) {
                            synchronized (sesion) {
                                if (!usuarios.containsKey(name)) {
                                    if (sesion.containsKey(name)) {
                                        if (sesion.get(name).equals(contra)) {
                                            usuarios.put(name, out);
                                            usuarios.get(name).println("NAMEACCEPTED " + name);
                                            break;
                                        } else {
                                            out.println("ERRORPASSWORD");
                                        }
                                    } else {
                                        sesion.put(name, contra);
                                        actualizarArchivoSesion();
                                        usuarios.put(name, out);
                                        bloqueos.put(name, new ArrayList<String>());
                                        actualizarArchivoBloqueos();
                                        usuarios.get(name).println("NAMEACCEPTED " + name);
                                        break;
                                    }
                                } else {
                                    out.println("ERRORUSER");
                                }
                            }
                        }
                    }
                }

                for (Entry<String, PrintWriter> usuario : usuarios.entrySet()) {
                    usuario.getValue().println("MESSAGE " + name + ": Connected.");
                }

                while (true) {
                    String input = in.nextLine();
                    if (input.toLowerCase().startsWith("/quit")) {
                        return;
                    }
                    if (input.toLowerCase().startsWith("/privado")) {
                        synchronized (usuarios) {
                            synchronized (bloqueos) {
                                int inicio = input.indexOf(" ");
                                int fin = input.indexOf(" ", inicio + 1);
                                String a = input.substring(inicio + 1, fin);
                                if (usuarios.containsKey(a) == true) {
                                    if (!bloqueos.get(a).contains(name)) {
                                        usuarios.get(a).println("MESSAGE " + name + ": " + input.substring(8).substring(a.length()).substring(2));
                                    }
                                    usuarios.get(name).println("MESSAGE " + name + ": " + input.substring(8).substring(a.length()).substring(2));
                                } else {
                                    usuarios.get(name).println("MESSAGE " + a + " no esta conectado.");
                                }
                            }
                        }
                    } else if (input.toLowerCase().startsWith("/bloquear")) {
                        synchronized (usuarios) {
                            synchronized (bloqueos) {
                                String a = input.substring(10);
                                if (!bloqueos.get(name).contains(a) && !a.equals(name)) {
                                    bloqueos.get(name).add(a);
                                    actualizarArchivoBloqueos();
                                    usuarios.get(name).println("MESSAGE " + a + " Bloqueado.");
                                } else {
                                    usuarios.get(name).println("MESSAGE " + a + " Ya esta bloqueado o no puedes bloquearte a ti mismo.");
                                }
                            }
                        }
                    } else if (input.toLowerCase().startsWith("/desbloquear")) {
                        synchronized (usuarios) {
                            synchronized (bloqueos) {
                                String a = input.substring(13);
                                if (bloqueos.get(name).remove(a)) {
                                    actualizarArchivoBloqueos();
                                    usuarios.get(name).println("MESSAGE " + a + " desbloqueado.");
                                } else {
                                    usuarios.get(name).println("MESSAGE " + a + " ya esta desbloqueado.");
                                }
                            }
                        }
                    }
                    else {
                        synchronized (usuarios) {
                            synchronized (bloqueos) {
                                for (Entry<String, PrintWriter> usuario : usuarios.entrySet()) {
                                    if (!bloqueos.get(usuario.getKey()).contains(name)) {
                                        usuario.getValue().println("MESSAGE " + name + ": " + input);
                                    }
                                }
                            }
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
