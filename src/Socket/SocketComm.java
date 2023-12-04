/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Socket;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;

/**
 *
 * @author h.liu
 */
public class SocketComm {

    private ServerSocket serversock_ = null;
    private Socket clientsock_ = null;
    private PrintWriter Out = null;
    private BufferedReader In = null;
    boolean use_server_ = false;

// ----------------------------------- ---------------------------- ----------------------------------
    public boolean isSocketON() {
        if (use_server_) {
            return isServerSockON();
        } else {
            return isClientSockConnected();
        }
    }

    public boolean isServerSockON() {
        if (serversock_ == null) {
            return false;
        } else {
            return serversock_.isBound() & !serversock_.isClosed();
        }
    }

    public boolean isClientSockConnected() {
        if (clientsock_ == null) {
            return false;
        } else {
            boolean clientConnToRightPort = true;
            if (use_server_ && isServerSockON()) {
                clientConnToRightPort = clientsock_.getLocalPort() == serversock_.getLocalPort();
            }

            return !clientsock_.isClosed() & clientsock_.isConnected() & clientConnToRightPort;
        }
    }

    public boolean isClientSockReadyForComm() {
        if (!isClientSockConnected() || Out == null || In == null) {
            return false;
        } else {
            return !clientsock_.isInputShutdown() & !clientsock_.isOutputShutdown();
        }
    }

    public int getSocketPort() {
        if (use_server_ && isServerSockON()) {
            return serversock_.getLocalPort();
        }

        if (!use_server_ && isClientSockConnected()) {
            return clientsock_.getPort();
        }

        return -1;
    }

// ----------------------------------- ---------------------------- ----------------------------------
    public boolean Send(String str, boolean verbose) {
        String sendstr = str + "\r\n";

        if (isClientSockReadyForComm()) {
            Out.write(sendstr);
            Out.flush();
            return true;
        } else {
            if (verbose) {
                System.out.println("Send: Not ready for comm through socket yet");
            }
            return false;
        }
    }

    public String Recv(double TimeOutSec, boolean verbose) {
        String recvstr = null;

        if (isClientSockReadyForComm()) {
            long time0 = System.currentTimeMillis();
            while (!Thread.currentThread().isInterrupted()) {
                if (TimeOutSec > 0) {
                    if (System.currentTimeMillis() - time0 >= TimeOutSec * 1e3) {
                        if (verbose) {
                            System.out.println("Recv: TimeOut - No message from socket");
                        }
                        break;
                    }
                }

                if (!isClientSockReadyForComm()) {
                    if (verbose) {
                        System.out.println("Recv: Not ready for comm through socket now");
                    }
                    break;
                }

                try {
                    if (In != null && In.ready()) {
                        recvstr = In.readLine();
                        if (recvstr != null) {
                            break;
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace(System.out);
                    break;
                }
            }
        } else {
            if (verbose) {
                System.out.println("Recv: Not ready for comm through socket yet");
            }
        }

        return recvstr;
    }

    public ArrayList<String> waitUntilRecv(String endsign, double timeOutSec, boolean verbose) {
        ArrayList<String> recvstrs = new ArrayList<>();
        if(String.valueOf(endsign).equalsIgnoreCase("null")){
            endsign = null;
        }

        if (isClientSockReadyForComm()) {
            double singleRecv_timeOutSec = 0.01; // can be smaller
            String recvstr = null;

            long currentTimeMs = System.currentTimeMillis();
            while (!Thread.currentThread().isInterrupted()) {
                double timeOutLeftSec = singleRecv_timeOutSec;
                if (timeOutSec > 0) {
                    timeOutLeftSec = timeOutSec - 1e-3 * (System.currentTimeMillis() - currentTimeMs);
                    if (timeOutLeftSec <= 0) {
                        if (verbose) {
                            System.out.println("waitUntilRecv: TimeOut now!!!");
                        }
                        break;
                    }
                }

                if (!isClientSockReadyForComm()) {
                    if (verbose) {
                        System.out.println("waitUntilRecv: Not ready for comm through socket now");
                    }
                    break;
                }

                recvstr = null;
                if (timeOutLeftSec < singleRecv_timeOutSec) {
                    recvstr = Recv(timeOutLeftSec, false);
                } else {
                    recvstr = Recv(singleRecv_timeOutSec, false);
                }

                if (recvstr != null) { 
                    recvstrs.add(recvstr);
                    if (endsign != null && !endsign.isEmpty()) {
                        if (recvstr.contains(endsign)) {
                            if (verbose) {
                                System.out.println("waitUntilRecv: endsign \"" + endsign + "\" received before timeOut");
                            }
                            break;
                        }
                    } else {
                        if (endsign!=null && endsign.isEmpty()) {
                            if (verbose) {
                                System.out.println("waitUntilRecv: one line received before timeOut");
                            }
                            break;
                        }
                    }
                }
            }
            
            if (recvstrs.isEmpty() && verbose){
                System.out.println("waitUntilRecv: Nothing received, timeOut");
            }
            
        } else {
            if (verbose) {
                System.out.println("waitUntilRecv: Not ready for comm through socket yet");
            }
        }
        return recvstrs;
    }

// ----------------------------------- ---------------------------- ----------------------------------
    public boolean ConnServerToClient(double TimeOutSec, boolean verbose) {
        if (use_server_) {
            if (isServerSockON()) {
                if (isClientSockConnected()) {
                    if (verbose) {
                        System.out.println("ConnServerToClient: Disconnecting previous client...");
                    }
                    DisconnectClientSock(verbose);
                }

                long time0 = System.currentTimeMillis();
                while (!Thread.currentThread().isInterrupted()) {
                    if (TimeOutSec > 0) {
                        if (System.currentTimeMillis() - time0 >= TimeOutSec * 1e3) {
                            if (verbose) {
                                System.out.println("ConnServerToClient: TimeOut - NO client available for connection");
                            }
                            break;
                        }
                    }

                    if (!isServerSockON()) {
                        if (verbose) {
                            System.out.println("ConnServerToClient: Server socket OFF now");
                        }
                        break;
                    }

                    try {
                        serversock_.setSoTimeout(100);
                        clientsock_ = serversock_.accept();
                        Out = new PrintWriter(clientsock_.getOutputStream(), true);
                        In = new BufferedReader(new InputStreamReader(clientsock_.getInputStream()));
                        break;
                    } catch (SocketTimeoutException e) {
                    } catch (IOException ex) {
                        ex.printStackTrace(System.out);
                        break;
                    }
                }

            } else {
                if (verbose) {
                    System.out.println("ConnServerToClient: Server socket NOT ON yet.");
                }
            }
            if (verbose) {
                System.out.println("ConnServerToClient: Server socket connected to client now? " + String.valueOf(isClientSockConnected()));
            }

            return isClientSockConnected();
        } else {
            if (verbose) {
                System.out.println("ConnServerToClient: Not applicable - use_server_ is " + String.valueOf(use_server_));
            }
            return false;
        }
    }

    public boolean serversocket_OFF(boolean verbose) {
        if (use_server_) {
            if (isServerSockON()) {
                if (isClientSockConnected()) {
                    if (verbose) {
                        System.out.println("serversocket_OFF: Disconnecting from client...");
                    }
                    DisconnectClientSock(verbose);
                }

                if (serversock_ != null) {
                    try {
                        serversock_.close();
                    } catch (Exception e) {
                        e.printStackTrace(System.out);
                    }
                }
                if (verbose) {
                    System.out.println("serversocket_OFF: Server socket now OFF? " + String.valueOf(!isServerSockON()));
                }
            } else {
                if (verbose) {
                    System.out.println("serversocket_OFF: Server socket already OFF? " + String.valueOf(!isServerSockON()));
                }
            }
        } else {
            if (verbose) {
                System.out.println("serversocket_OFF: Not applicable - use_server_ is " + String.valueOf(use_server_));
            }
        }

        return !isServerSockON();
    }

// ----------------------------------- ---------------------------- ----------------------------------
    public boolean DisconnectClientSock(boolean verbose) {
        if (isClientSockConnected()) {
            if (In != null) {
                try {
                    In.close();
                } catch (IOException e) {
                    e.printStackTrace(System.out);
                }
            }

            if (Out != null) {
                Out.close();
            }

            if (clientsock_ != null) {
                try {
                    clientsock_.close();
                } catch (Exception e) {
                    e.printStackTrace(System.out);
                }
            }
            if (verbose) {
                System.out.println("DisconnectClientSock: Now client socket disconnected? " + String.valueOf(!isClientSockConnected()));
            }
        } else {
            if (verbose) {
                System.out.println("DisconnectClientSock: Client socket already disconnected? " + String.valueOf(!isClientSockConnected()));
            }
        }

        return !isClientSockConnected();
    }

    public boolean socket_OFF(boolean verbose) {
        return DisconnectClientSock(verbose) & serversocket_OFF(verbose);
    }

    public boolean socket_ON(int port, boolean use_server, boolean verbose) {
        if (isServerSockON() || isClientSockConnected()) {
            if (verbose) {
                System.out.println("socket_ON: Closing previous socket...");
            }
            socket_OFF(verbose);
        }

        use_server_ = use_server;

        if (use_server_) {
            try {
                serversock_ = new ServerSocket(port);
            } catch (Exception ex) {
                try {
                    serversock_ = new ServerSocket();
                    if (verbose) {
                        System.out.println("socket_ON - server: Assigned port not available. Automatically allocated one.");
                    }
                } catch (Exception ex1) {
                    if (verbose) {
                        System.out.println("socket_ON - server: No available localhost port!");
                    }
                }
            }

            if (verbose) {
                System.out.println("socket_ON - server: Server creation successful? " + String.valueOf(isSocketON())
                        + "\n" + "socket_ON - server: Server on localhost port " + String.valueOf(getSocketPort()));
            }

        } else {
            try {
                clientsock_ = new Socket("localhost", port);
                Out = new PrintWriter(clientsock_.getOutputStream(), true);
                In = new BufferedReader(new InputStreamReader(clientsock_.getInputStream()));
            } catch (Exception ex) {
                if (verbose) {
                    System.out.println("socket_ON - client: Failed to connect to localhost port!");
                }
            }

            if (verbose) {
                System.out.println("socket_ON - client: Client socket creation successful? " + String.valueOf(isSocketON())
                        + "\n" + "socket_ON - client: Client socket connected to localhost port " + String.valueOf(getSocketPort()));
            }

        }

        return isSocketON();
    }

// ----------------------------------- ---------------------------- ----------------------------------
    public static void main(String[] args) {
        SocketComm serversock = new SocketComm();
        SocketComm clientsock = new SocketComm();

        clientsock.socket_ON(9999, false, true);

        serversock.socket_ON(9999, true, true);
        serversock.ConnServerToClient(1, true);

        clientsock.socket_ON(9999, false, true);
        serversock.ConnServerToClient(1, true);

        clientsock.Send("hello from client0", true);
        System.out.println(serversock.waitUntilRecv("this is an error", 1, true));
        clientsock.Send("hello from client1", true);
        clientsock.Send("hello from client2", true);
        System.out.println(serversock.waitUntilRecv("client", 1, true));

        serversock.Send("hello from server", true);
        String recvstr = clientsock.Recv(1, true);
        System.out.println("Client Recv: " + recvstr);

        clientsock.socket_OFF(true);
        serversock.Send("hello from server", true);
        serversock.socket_OFF(true);
    }

}
