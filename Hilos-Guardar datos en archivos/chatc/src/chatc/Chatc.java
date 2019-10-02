package chatc;

import java.awt.BorderLayout;
import static java.awt.BorderLayout.SOUTH;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOError;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public class Chatc {

    String serverAddress;
    Scanner in;
    PrintWriter out;
    JFrame frame = new JFrame("Chatter");
    JTextField textField = new JTextField(50);
    JTextArea messageArea = new JTextArea(16, 5);
    JPasswordField contraseña = new JPasswordField(50);

    public Chatc(String serverAddress) {
        this.serverAddress = serverAddress;
        textField.setEditable(false);
        messageArea.setEditable(false);
        frame.getContentPane().add(textField, BorderLayout.SOUTH);
        frame.getContentPane().add(new JScrollPane(messageArea), BorderLayout.CENTER);
        frame.pack();
        textField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                out.println(textField.getText());
                textField.setText("");
            }
        });

    }

    private void run() throws IOException {
        try {
            Socket socket = new Socket(serverAddress, 59001);
            in = new Scanner(socket.getInputStream());
            out = new PrintWriter(socket.getOutputStream(), true);
            while (in.hasNextLine()) {
                String line = in.nextLine();
                if (line.startsWith("MESSAGE ")) {
                    messageArea.append(line.substring(8) + "\n");
                } else if (line.startsWith("SUBMITNAME")) {
                    out.println(JOptionPane.showInputDialog(frame, "Usuario: ", "Ingreso de usuario", JOptionPane.PLAIN_MESSAGE));
                } else if (line.startsWith("SUBMITPASSWORD")) {
                    out.println(JOptionPane.showInputDialog(frame, "Contraseña: ", "Ingreso de contraseña", JOptionPane.PLAIN_MESSAGE));
                } else if (line.startsWith("ERRORPASSWORD")) {
                    JOptionPane.showMessageDialog(frame, "La contraseña no es correcta", "Error contraseña", JOptionPane.ERROR_MESSAGE);
                } else if (line.startsWith("ERRORUSER")) {
                    JOptionPane.showMessageDialog(frame, "El usuario ya inicio sesión", "Error usuario", JOptionPane.ERROR_MESSAGE);
                } else if (line.startsWith("NAMEACCEPTED")) {
                    this.frame.setTitle("Chateador - " + line.substring(13));
                    textField.setEditable(true);
                }
            }
        } finally {
            frame.setVisible(false);
            frame.dispose();
        }

    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("Pass the server ip as the solve ");
            return;
        }
        Chatc client = new Chatc(args[0]);
        client.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        client.frame.setVisible(true);
        client.run();

    }

}
