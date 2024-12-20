/*
 * BSD 3-Clause License
 *
 * Copyright (c) 2023, Imperial College London 
 * All rights reserved.

 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package Utils;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


public class utils {

    /**
     * main function for testing methods in this class
     */
    public static void main(String args[]) {
        utils ut = new utils();
    }

    /**
     * stop the input thread within the input TimeOutSec
     *
     * 1. if thread not null and still alive, wait for TimeOutSec/2; 2. Then, if
     * still alive, interrupt and wait for TimeOutSec/2; 3. Return if the thread
     * has been stopped;
     *
     * -- if verbose, print descriptions along the way; -- non-positive
     * TimeOutSec is equal to infinite TimeOutSec;
     *
     * @param thread_: the thread to be closed
     * @param TimeOutSec: timeOut in seconds
     * @param verbose:
     * @return isThreadStopped
     */
    public boolean stop_thread(Thread thread_, double TimeOutSec, boolean verbose) {
        boolean isThreadStopped;
        if (thread_ == null) {
            if (verbose) {
                System.out.println("stop_thread: this Thread is null");
            }
            isThreadStopped = true;
        } else {
            if (thread_.isAlive()) {
                // wait for half-timeOutSec if timeOutSec is positive
                if (TimeOutSec > 0) {
                    long currentTime = System.currentTimeMillis();
                    while (!Thread.currentThread().isInterrupted()) {
                        if (System.currentTimeMillis() - currentTime >= (TimeOutSec / 2) * 1e3) {
                            break;
                        }

                        if (!thread_.isAlive()) {
                            break;
                        }
                    }
                }

                if (thread_.isAlive()) {
                    if (verbose) {
                        System.out.println("stop_thread: interrupting this thread...");
                    }

                    thread_.interrupt();

                    if (TimeOutSec > 0) {
                        long currentTime = System.currentTimeMillis();
                        while (!Thread.currentThread().isInterrupted()) {
                            if (System.currentTimeMillis() - currentTime >= (TimeOutSec / 2) * 1e3) {
                                break;
                            }

                            if (!thread_.isAlive()) {
                                break;
                            }
                        }
                    }
                }
            } else {
                if (verbose) {
                    System.out.println("stop_thread: this thread is already terminated");
                }
            }

            isThreadStopped = !thread_.isAlive();
        }

        return isThreadStopped;
    }

    /**
     * get Path variable from a path string
     * if string not a valid path variable, return null
     * 
     * @param PathStr
     * @param verbose
     * @return 
     */
    public Path getPathFromStr(String PathStr, boolean verbose) {
        try {
            return Paths.get(Paths.get(PathStr).toAbsolutePath().toFile().getCanonicalPath());
        } catch (Exception e) {
            if (verbose) {
                System.out.println("getPathFromStr: Invalid path! " + PathStr);
                e.printStackTrace(System.out);
            }
            return null;
        }
    }

    /**
     * given path string of a directory; if it does not exist, try to create it; 
     * return null if directory not exist at the end of the function
     * otherwise return the path to the directory
     * Note: this function cannot create a dir whose parent also not exist
     * 
     * @param DirPathStr
     * @param verbose
     * @return targetDirPath
     */
    public Path CreateifTargetDirNotExist(String DirPathStr, boolean verbose) {
        Path targetDirPath = getPathFromStr(DirPathStr, verbose);
        if (targetDirPath == null) {
            if (verbose) {
                System.out.println("CreateifTargetDirNotExist: Invalid path! " + DirPathStr);
            }
            return null;
        }

        if (!Files.exists(targetDirPath)) {
            try {
                if (verbose) {
                    System.out.println("Trying to create target directory: " + targetDirPath.toString());
                }
                Files.createDirectory(targetDirPath);
                if (verbose) {
                    System.out.println("Succeeded to create target directory: " + targetDirPath.toString());
                }
            } catch (Exception ex) {
                if (verbose) {
                    System.out.println("Failed to create target directory: " + targetDirPath.toString());
                    ex.printStackTrace(System.out);
                }
                return null;
            }
        } else {
            if (verbose) {
                System.out.println("Target directory already exists: " + targetDirPath.toString());
            }
        }

        try {
            targetDirPath = targetDirPath.toRealPath();
        } catch (Exception ex) {
        }

        return targetDirPath;
    }

    /**
     * Check if the target directory exist; If not, try to create it.Return the
     * path to the directory if all good; otherwise return null;
     * 
     * Note: this function use recursion to create all directories in the path string that do not exist
     *
     * @param DirPathStr
     * @param verbose
     * @return targetDirPath
     */
    public Path CreateifTargetDirsNotExist(String DirPathStr, boolean verbose) {
        Path targetDirPath = CreateifTargetDirNotExist(DirPathStr, verbose);

        if (targetDirPath == null) {
            if (verbose) {
                System.out.println("Failed to directly create directory from path string: " + DirPathStr);
            }

            if (getPathFromStr(DirPathStr, verbose) == null) {
                if (verbose) {
                    System.out.println("CreateifTargetDirsNotExist: Invalid path! " + DirPathStr);
                }
                return null;
            } else {
                targetDirPath = getPathFromStr(DirPathStr, verbose);
                
                if (verbose) {
                    System.out.println("Try to resolve by creating parent directory of " + targetDirPath.toString());
                }

                Path parentDirPath = targetDirPath.getParent();
                if (parentDirPath == null) {
                    if (verbose) {
                        System.out.println("No parent for this directory: " + targetDirPath.toString());
                    }
                    return null;
                } else {
                    if (CreateifTargetDirNotExist(parentDirPath.toString(), verbose) == null) {
                        if (verbose) {
                            System.out.println("Failed to create parent directory: " + parentDirPath.toString());
                        }
                        return null;
                    } else {
                        if (verbose) {
                            System.out.println("Suceeded to create parent directory: " + parentDirPath.toString());
                        }
                        return CreateifTargetDirNotExist(DirPathStr, verbose);
                    }
                }
            }
        } else {
            return targetDirPath;
        }
    }

    /**
     * Check if a file exist; if not, try to copy & paste from resources package;
     * return if file exist now
     * 
     * 
     * @param FilePathStr
     * @param verbose
     * @return 
     */
    public boolean ReadFromResources_IfTargetNotExist(String FilePathStr, boolean verbose) {
        Path FilePath = Paths.get(FilePathStr);
        String DirPathStr = Paths.get(FilePathStr).getParent().toAbsolutePath().toString();
        String FileNameStr = Paths.get(FilePathStr).getFileName().toString();

        boolean isFileExist = FilePath.toFile().exists();
        if (!isFileExist) {
            isFileExist = writeFileFromResources(FileNameStr, null, DirPathStr, true, verbose);
        }
        return isFileExist;
    }

    /**
     * Read a file in a dir/classResources and copy into another dir.
     *
     * Return if the whole process has been successful.
     *
     * @param filename
     * @param fromDir
     * @param toDir
     * @param overwrite
     * @param verbose
     * @return succeeded
     */
    public boolean writeFileFromResources(String filename, String fromDir, String toDir, boolean overwrite, boolean verbose) {
        String default_toDirSTR = System.getProperty("user.dir");
        String default_fromDirSTR = "/Resources/";

        // if toDir is not an existing dir, try to create it
        String targetDirstr = toDir == null ? default_toDirSTR : toDir;
        Path targetDir = CreateifTargetDirsNotExist(targetDirstr, verbose);

        if (targetDir == null) {
            return false;
        } else {
            if (verbose) {
                System.out.println("Target directory ready now: " + targetDir.toAbsolutePath());
            }

            Path targetFilePath = targetDir.resolve(filename);
            boolean filealreadyexisted = targetFilePath.toFile().exists();
            boolean needRead = !filealreadyexisted | (filealreadyexisted & overwrite);

            if (!needRead) {
                if (verbose) {
                    System.out.println("Target file already exists & not overwritten: " + targetFilePath.toAbsolutePath());
                }
                return false;
            } else {
                if (fromDir == null) {
                    InputStream in = null;
                    try {
                        in = this.getClass().getResourceAsStream(default_fromDirSTR + filename);
                    } catch (Exception e) {
                        if (verbose) {
                            System.out.println("Failed to read source file from Resources: " + default_fromDirSTR + filename);
                        }
                        return false;
                    }

                    if (in != null) {
                        try {
                            Files.copy(in, targetFilePath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                            if (verbose) {
                                System.out.println("Successfully write source file to target: " + targetFilePath);
                            }
                            return true;
                        } catch (Exception ex) {
                            if (verbose) {
                                System.out.println("Failed to write source file to target: " + targetFilePath);
                            }
                            return false;
                        }
                    } else {
                        if (verbose) {
                            System.out.println("Failed to read source file from Resources: " + default_fromDirSTR + filename);
                        }
                        return false;
                    }
                } else {
                    Path sourceFilePath = Paths.get(fromDir + System.getProperty("file.separator") + filename);
                    if (sourceFilePath.toFile().exists()) {
                        try {
                            Files.copy(sourceFilePath, targetFilePath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                            if (verbose) {
                                System.out.println("Successfully write source file to target: " + sourceFilePath.toAbsolutePath());
                            }
                            return true;
                        } catch (Exception ex) {
                            if (verbose) {
                                System.out.println("Failed to write source file to target: " + sourceFilePath.toAbsolutePath());
                            }
                            return false;
                        }
                    } else {
                        if (verbose) {
                            System.out.println("Source file not exists: " + sourceFilePath.toAbsolutePath());
                        }
                        return false;
                    }
                }
            }
        }
    }

}
