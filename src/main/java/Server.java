import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;


public class Server {
    private static int uniqueId;
    private ArrayList<ClientThread> clientThreads;
    private ServerGUI serverGUI;
    private SimpleDateFormat simpleDateFormat;
    private int port;
    private boolean keepGoing;
    private  String chatWith;

    public Server(int port) {
        this(port, null);
    }

    public Server(int port, ServerGUI serverGUI) {
        this.serverGUI = serverGUI;
        this.port = port;
        simpleDateFormat = new SimpleDateFormat("HH:mm:ss");
        clientThreads = new ArrayList<>();
    }


    public static void main(String[] args) {
        int portNumber = 1500;
        switch (args.length) {
            case 1:
                try {
                    portNumber = Integer.parseInt(args[0]);
                } catch (Exception e) {
                    System.out.println("Invalid port number.");
                    System.out.println("Usage is: > java Server [portNumber]");
                    return;
                }
            case 0:
                break;
            default:
                System.out.println("Usage is: > java Server [portNumber]");
                return;

        }
        Server server = new Server(portNumber);
        server.start();
    }

    public void start() {
        keepGoing = true;
        try {
            ServerSocket serverSocket = new ServerSocket(port);

            while (keepGoing) {
                display("Server waiting for Clients on port " + port + ".");

                Socket socket = serverSocket.accept();
                if (!keepGoing)
                    break;
                ClientThread t = new ClientThread(socket);
                clientThreads.add(t);
                t.start();
            }
            try {
                serverSocket.close();
                for (int i = 0; i < clientThreads.size(); ++i) {
                    ClientThread tc = clientThreads.get(i);
                    try {
                        tc.sInput.close();
                        tc.sOutput.close();
                        tc.socket.close();
                    } catch (IOException ioE) {
                        System.out.println(ioE.toString());
                    }
                }
            } catch (Exception e) {
                display("Exception closing the server and clients: " + e);
            }
        }
        catch (IOException e) {
            String msg = simpleDateFormat.format(new Date()) + " Exception on new ServerSocket: " + e + "\n";
            display(msg);
        }
    }

    protected void stop() {
        keepGoing = false;

        try {
            new Socket("localhost", port);
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    private void display(String msg) {
        String time = simpleDateFormat.format(new Date()) + " " + msg;
        if (serverGUI == null)
            System.out.println(time);
        else
            serverGUI.appendEvent(time + "\n");
    }


    private synchronized void broadcast(String message) {
        String time = simpleDateFormat.format(new Date());

        String[] w = message.split(" ",3);
        chatWith=w[0]+w[1];

        boolean isPrivate = false;
        if(w[1].charAt(0)=='-') {
            isPrivate = true;
        }
        if(isPrivate)
        {
            String tocheck=w[1].substring(1, w[1].length());
            System.out.println("tocheck"+tocheck);
            message=w[0]+w[2];
            String messageLf = time + " " + message + "\n";
            boolean found=false;
            for(int y=clientThreads.size(); --y>=0;)
            {
                ClientThread ct1=clientThreads.get(y);
                String check=ct1.getUsername();
                if(check.equals(tocheck))
                {
                    if(!ct1.writeMsg(messageLf)) {
                        clientThreads.remove(y);
                        display("Disconnected Client " + ct1.username + " removed from list.");
                    }
                    found=true;
                    break;
                }

            }
        }
        else if (serverGUI == null){
            String messageLf = time + " " + message + "\n";
            System.out.print(messageLf);
        }
        else {
            String messageLf = time + " " + message + "\n";
            serverGUI.appendRoom(messageLf);

        for (int i = clientThreads.size(); --i >= 0; ) {
            ClientThread ct = clientThreads.get(i);
            // try to write to the Client if it fails remove it from the list
            if (!ct.writeMsg(messageLf)) {
                clientThreads.remove(i);
                display("Disconnected Client " + ct.username + " removed from list.");
            }
        }
        }
    }

    synchronized void remove(int id) {
        for (int i = 0; i < clientThreads.size(); ++i) {
            ClientThread ct = clientThreads.get(i);
            if (ct.id == id) {
                clientThreads.remove(i);
                return;
            }
        }
    }

    class ClientThread extends Thread {
        Socket socket;
        ObjectInputStream sInput;
        ObjectOutputStream sOutput;
        int id;
        String username;
        ChatMessage cm;
        String date;

        ClientThread(Socket socket) {
            id = ++uniqueId;
            this.socket = socket;
            System.out.println("Thread trying to create Object Input/Output Streams");
            try {
                sOutput = new ObjectOutputStream(socket.getOutputStream());
                sInput = new ObjectInputStream(socket.getInputStream());
                username = (String) sInput.readObject();
                display(username + " just connected.");
            } catch (IOException e) {
                display("Exception creating new Input/output Streams: " + e);
                return;
            }

            catch (ClassNotFoundException e) {
            }
            date = new Date().toString() + "\n";
        }
        public String getUsername() {
            return username;
        }
        public void run() {
            boolean keepGoing = true;
            while (keepGoing) {
                try {
                    cm = (ChatMessage) sInput.readObject();
                } catch (IOException e) {
                    display(username + " Exception reading Streams: " + e);
                    break;
                } catch (ClassNotFoundException e2) {
                    break;
                }
                String message = cm.getMessage();

                switch (cm.getType()) {

                    case ChatMessage.MESSAGE:
                        broadcast(username + ": " + message);
                        break;
                    case ChatMessage.LOGOUT:
                        display(username + " disconnected with a LOGOUT message.");
                        keepGoing = false;
                        break;
                    case ChatMessage.WHOISIN:
                        writeMsg("List of the users connected at " + simpleDateFormat.format(new Date()) + "\n");
                        // scan clientThreads the users connected
                        for (int i = 0; i < clientThreads.size(); ++i) {
                            ClientThread ct = clientThreads.get(i);
                            writeMsg((i + 1) + ") " + ct.username + " since " + ct.date);
                        }
                        break;
                }
            }

            remove(id);
            close();
        }

        private void close() {
            try {
                if (sOutput != null)
                    sOutput.close();
            } catch (Exception e) {
                writeMsg(e.toString());
            }
            try {
                if (sInput != null)
                    sInput.close();
            } catch (Exception e) {
                writeMsg(e.toString());
            }
            try {
                if (socket != null)
                    socket.close();
            } catch (Exception e) {
                writeMsg(e.toString());
            }
        }

        private boolean writeMsg(String msg) {
            // if Client is still connected send the message to it
            if (!socket.isConnected()) {
                close();
                return false;
            }
            // write the message to the stream
            try {
                sOutput.writeObject(msg);
            }
            // if an error occurs, do not abort just inform the user
            catch (IOException e) {
                display("Error sending message to " + username);
                display(e.toString());
            }
            return true;
        }
    }
}
