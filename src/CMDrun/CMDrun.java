/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package CMDrun;

import Utils.utils;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author h.liu
 */
public class CMDrun {

    ProcessBuilder builder;
    Process process;

    BufferedReader reader;
    BufferedWriter writer;

    Thread exe_reader;

    ArrayList<String> Readers;
    int marker;
    utils ut;

// -----------------------  ----------------------- ----------------------- -----------------------
    public CMDrun() {
        builder = new ProcessBuilder();
        ut = new utils();

        String osname = System.getProperty("os.name");
        if (osname.startsWith("Windows")) {
            String[] cmds = {"cmd.exe"};
            builder.command(cmds);
        } else {
            String[] cmds = {"/bin/sh"};
            builder.command(cmds);
        }

        builder.directory(Paths.get(System.getProperty("user.home")).toFile());
        builder.redirectErrorStream(true);
    }

// -----------------------  ----------------------- ----------------------- -----------------------
    /**
     * write a string to CMD std in. Return true if write successful; return
     * false if write unsuccessful
     */
    public boolean Write(String str, boolean verbose) {
        if (isProcessAlive()) {
            if (!iswriterOFF()) {
                try {
                    writer.write(str);
                    writer.newLine();
                    writer.flush();
                    return true;
                } catch (IOException ex) {
                    ex.printStackTrace(System.out);
                    return false;
                }
            } else {
                if (verbose) {
                    System.out.println("Write: write stream already closed");
                }
                return false;
            }
        } else {
            if (verbose) {
                System.out.println("Write: NO alive cmd process");
            }
            return false;
        }
    }

    /**
     * wait until timeout or endsign (contained) received. Return an arraylist
     * of strings sent back if wait succeed if last str in arraylist of strings
     * not contain endsign -- timeout
     */
    public ArrayList<String> waitUntilRecv(String endsign, double timeOutSec, boolean verbose) {
        ArrayList<String> recvstrs = new ArrayList<>();
        if (String.valueOf(endsign).equalsIgnoreCase("null")) {
            endsign = null;
        }

        if (isProcessAlive()) {
            boolean endnow = false;

            long currentTimeMs = System.currentTimeMillis();
            while (!Thread.currentThread().isInterrupted()) {
                if (timeOutSec > 0) {
                    if (System.currentTimeMillis() - currentTimeMs > timeOutSec * 1e3) {
                        if (verbose) {
                            System.out.println("WaitUntilRecv: TimeOut before endsign received");
                        }
                        break;
                    }
                }

                if (!isProcessAlive()) {
                    if (verbose) {
                        System.out.println("WaitUntilRecv: not completed as process not alive now");
                    }
                    break;
                }

                if (Readers.size() > marker) {
                    int added_marker = 0;
                    int size = Readers.size();
                    for (int i = marker; i < size; i++) {
                        String tempstr = Readers.get(i);
                        recvstrs.add(tempstr);
                        added_marker += 1;

                        if (endsign != null && !endsign.isEmpty()) {
                            if (tempstr != null && !tempstr.isEmpty() && tempstr.contains(endsign)) {
                                endnow = true;
                                break;
                            }
                        } else {
                            if (endsign != null && endsign.isEmpty()) {
                                // if endsign == "", only detect one line;
                                if (tempstr != null && !tempstr.isEmpty()) {
                                    endnow = true;
                                    break;
                                }
                            } // if endsign == null, wait until timeOut 
                        }
                    }
                    marker += added_marker;
                }

                if (endnow) {
                    break;
                }
            }
        } else {
            if (verbose) {
                System.out.println("WaitUntilRecv: not applicable as process not alive yet");
            }
        }

        return recvstrs;
    }

// -----------------------  ----------------------- ----------------------- -----------------------
    public boolean startProcess(boolean verbose) {
        if (verbose) {
            System.out.println("startProcess: terminating previous process if applicable...");
        }
        stopProcess(0.1, verbose);

        try {
            process = builder.start();
        } catch (IOException ex) {
            ex.printStackTrace(System.out);
        }

        if (isProcessAlive()) {
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));

            Readers = new ArrayList<>();
            marker = Readers.size();

            exe_reader = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (!Thread.currentThread().isInterrupted()) {
                        if (!isProcessAlive()) {
                            break;
                        }

                        if (!isreaderOFF()) {
                            try {
                                reader.lines().forEachOrdered(line -> Readers.add(line));
                            } catch (Exception ex) {
                                break;
                            }
                        } else {
                            break;
                        }
                    }
                }
            });

            exe_reader.start();
        }

        if (verbose) {
            System.out.println("startProcess: process ON? " + String.valueOf(isProcessAlive()));
        }
        return isProcessAlive();
    }

    public boolean stopProcess(double TimeOutSec_hf, boolean verbose) {
        if (isProcessAlive()) {
            process.destroyForcibly();
            boolean is_normalexit = false;
            try {
                is_normalexit = process.waitFor(Math.round(TimeOutSec_hf * 1e3), TimeUnit.MILLISECONDS);
            } catch (Exception ex) {
                ex.printStackTrace(System.out);
            }

            if (verbose) {
                System.out.println("stopProcess: process OFF? " + String.valueOf(!isProcessAlive()));
                System.out.println("stopProcess: abnormal exiting? " + String.valueOf(!is_normalexit));
            }
        } else {
            if (verbose) {
                System.out.println("stopProcess: process already OFF? " + String.valueOf(!isProcessAlive()));
            }
        }

        if (!iswriterOFF()) {
            CloseWriter(verbose);
        }

        if (!isreaderOFF()) {
            CloseReader(verbose);
        }

        boolean isStopped = true;
        if (!isExeReaderOFF()) {
            isStopped = ut.stop_thread(exe_reader, TimeOutSec_hf, verbose);
        }

        return (!isProcessAlive()) & isreaderOFF() & iswriterOFF() & isExeReaderOFF() & isStopped;
    }

// -----------------------  ----------------------- ----------------------- -----------------------
    private void CloseWriter(boolean verbose) {
        if (!iswriterOFF()) {
            try {
                writer.close();
            } catch (Exception e) {
                e.printStackTrace(System.out);
            }

            if (verbose) {
                System.out.println("CloseWriter: writer stream closed? " + String.valueOf(iswriterOFF()));
            }
        } else {
            if (verbose) {
                System.out.println("CloseWriter: writer stream already closed? " + String.valueOf(iswriterOFF()));
            }
        }
    }

    private void CloseReader(boolean verbose) {
        if (!isreaderOFF()) {
            try {
                reader.close();
            } catch (Exception e) {
                e.printStackTrace(System.out);
            }

            if (verbose) {
                System.out.println("CloseReader: reader stream closed? " + String.valueOf(isreaderOFF()));
            }
        } else {
            if (verbose) {
                System.out.println("CloseReader: reader stream already closed? " + String.valueOf(isreaderOFF()));
            }
        }
    }

// -----------------------  ----------------------- ----------------------- -----------------------
    public boolean isProcessAlive() {
        if (process == null) {
            return false;
        } else {
            return process.isAlive();
        }
    }

    public boolean isreaderOFF() {
        if (reader == null) {
            return true;
        } else {
            try {
                reader.ready();
                return false;
            } catch (IOException ex) {
                return true;
            } catch (Exception ex1) {
                ex1.printStackTrace(System.out);
                return true;
            }
        }
    }

    public boolean iswriterOFF() {
        if (writer == null) {
            return true;
        } else {
            try {
                writer.write("");
                return false;
            } catch (IOException ex) {
                return true;
            } catch (Exception ex1) {
                ex1.printStackTrace(System.out);
                return true;
            }
        }
    }

    public boolean isExeReaderOFF() {
        if (exe_reader == null) {
            return true;
        } else {
            return !exe_reader.isAlive();
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
        boolean verbose = true;

        CMDrun cmds = new CMDrun();

        cmds.startProcess(verbose);
        cmds.Write("ls", verbose);
        System.out.println(cmds.waitUntilRecv(null, 1, verbose));

        cmds.startProcess(verbose);
        cmds.Write("ls", verbose);
        System.out.println(cmds.waitUntilRecv(null, 1, verbose));

        cmds.stopProcess(0.1, verbose);

    }
}
