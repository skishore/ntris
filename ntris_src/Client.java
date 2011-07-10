package ntris_src;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static ntris_src.Constants.*;

public class Client implements Runnable {
    private boolean logMessages = false;
    private Socket socket;
    private BufferedReader reader;
    private OutputStreamWriter writer;
    private BlockingQueue<String> received;
    
    private String IPAddress;
    private int port;
    private long delay;
    private boolean online;
    
    public Client(String IP, int newPort, long frameDelay) {
        IPAddress = IP;
        port = newPort;
        delay = frameDelay;

        received = new LinkedBlockingQueue<String>();
        online = false;
    }
    
    public boolean connect() {
        received.clear();

        try {
            socket = new Socket();
            InetSocketAddress socketaddress = new InetSocketAddress(IPAddress, port);
            socket.connect(socketaddress, 8000);
        } catch (IOException e) {
            System.err.println("Could not listen to server on port " + port);
            return false;
        }

        try {
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new OutputStreamWriter(socket.getOutputStream());
            writer.flush();
        } catch (IOException e) {
            System.err.println("Error creating writer or reader.");
            return false;
        }
        
        online = true;
        Thread th = new Thread(this);
        th.start();
        return true;
    }
    
    public void disconnect() {
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) { 
            }
        }
        online = false;
    }
        
    public void sendCommand(String command) {
        if (!online)
            return;
        
        try {
            writer.write(command + " ");
            writer.flush();
            if (logMessages)
                System.out.println("Sent: " + command);
        } catch (IOException e) {
            System.err.println("Failed to write to server");
            online = false;
        }
    }

    public String getCommand() {
        return received.poll();
    }
    
    public void run() {
        while (online) {
            try {
                String command = reader.readLine();
                if (command != null) {
                    if (logMessages)
                        System.out.println("Received: " + command);
                    try {
                        received.put(command);
                    } catch (InterruptedException e) {
                        System.err.println("Fatal error: dropped command " + command);
                    }
                }
            } catch (IOException e) {
                System.err.println("Lost connection.  Failed to read from server");
                online = false;
            }
            
            try {
                Thread.sleep(delay);
            } catch (InterruptedException ex) {
            }
        }
        
        try {
            socket.close();
        } catch (IOException e) { 
        }
    }
    
    public boolean isOnline() {
        return online;
    }
    
    public static String listToString(List<Integer> list) {
        String ans = "";
        for (int element : list) {
            ans = ans + element + ",";
        }
        ans = ans.substring(0, ans.length() - 1);
        return ans;
    }
    
    public static List<Integer> stringToList(String string) {
        if (string == null)
            return null;
            
        String[] tokens = string.split(",");
        ArrayList<Integer> ans = new ArrayList<Integer>();
        
        for (int i = 0; i < tokens.length; i++) {
            ans.add(Integer.parseInt(tokens[i]));
        }
        return ans;
    }

    public static String sanitizeLogonString(String message, int numTokens) {
        int tokensFound = 0;
        boolean atSeparator = true;
            
        message = message.toLowerCase();

        for (int i = 0; i < message.length(); i++) {
            char c = message.charAt(i);
            if (Character.isLetterOrDigit(c)) {
                if (atSeparator) {
                    tokensFound++;
                    atSeparator = false;
                }   
            } else if (c == '.') {
                if (atSeparator)
                    return null;
                atSeparator = true;
            } else {
                return null;
            }
        }

        if (atSeparator || (tokensFound != numTokens))
            return null; 
        return message;
    }

    public static String escape(String message) {
        if ((message == null) || (message.length() == 0))
            return "_";
        return message.replace(' ', '_').replace('.', '|');
    }

    public static String unescape(String message) {
        return message.replace('|', '.').replace('_', ' ');
    }

    public static String getDateTime() {
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date date = new Date();
        return dateFormat.format(date);
    }
}
