/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package subcoordinator;

import jade.core.ProfileImpl;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import logger.MyLogger;
import jade.core.Runtime;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;

/**
 *
 * @author Ethan_Hunt
 */
public class SubCoordinator implements Runnable {
    public static int agentNumber = 0;

    protected int          serverPort   = 8080;
    protected ServerSocket serverSocket = null;
    protected boolean      isStopped    = false;
    protected Thread       runningThread= null;
    protected ExecutorService threadPool ;// Executors.newFixedThreadPool(30);
    private int sizeOfThreadPool = 5000;
    private static final Logger LOGGER =
        Logger.getLogger(SubCoordinator.class.getName());
    
    public SubCoordinator(int port, int sizeOfThreadPool){
        this.serverPort = port;
        this.sizeOfThreadPool = sizeOfThreadPool;
        this.threadPool= Executors.newFixedThreadPool(sizeOfThreadPool);
    }

    public void run(){
        LOGGER.entering(getClass().getName(), "run() method");
        synchronized(this){
            this.runningThread = Thread.currentThread();
        }
        openServerSocket();
        LOGGER.log(Level.INFO, "Server started at port{0}", serverPort);
        
        while(! isStopped()){
            Socket clientSocket = null;
            try {
                clientSocket = this.serverSocket.accept();
            } catch (IOException e) {
                if(isStopped()) {
                    LOGGER.log(Level.INFO, "Server Stopped");
                    return;
                }
                LOGGER.log(Level.SEVERE, "Error accepting client connection", e);
                throw new RuntimeException(
                    "Error accepting client connection", e);
            }
            this.threadPool.execute(new Worker(clientSocket));
        }
        this.threadPool.shutdown();
        //System.out.println("Server Stopped.") ;
        LOGGER.log(Level.INFO, "Server Stopped");
        LOGGER.exiting(getClass().getName(), "Server thread stopped");
        
    }
    
    private synchronized boolean isStopped() {
        return this.isStopped;
    }

    public synchronized void stop(){
        this.isStopped = true;
        try {
            this.serverSocket.close();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Cannon Open Port", new RuntimeException("Error"));
            throw new RuntimeException("Error closing server", e);
            
        }
    }
    
     private void openServerSocket() {
        LOGGER.entering(getClass().getName(), "openServerSocket()");
        try {
            this.serverSocket = new ServerSocket(this.serverPort);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Cannon Open Port", new RuntimeException("Error"));
            throw new RuntimeException("Cannot open port "+serverPort, e);
        }
        LOGGER.exiting(getClass().getName(), "openServerSocket");
    }

    public static void main(String[] args){
        try {
              MyLogger.setup();
            } catch (IOException ex) {
                LOGGER.log(Level.SEVERE, "Cannot Open file", ex);
        }
        
        
        int sizeOfThreadPool=10;
        if (args.length == 0){
            throw new IllegalArgumentException("Must specify a port!");
	}else if (args.length>=2){
            String level = args[1];
            MyLogger.setLevel(Integer.decode(level));
            sizeOfThreadPool = Integer.decode(args[2]);
        }
        
        /*
        Runtime rt = Runtime.instance();
        // Exit the JVM when there are no more containers around
        rt.setCloseVM(true);
        System.out.print("runtime created\n");
        
        
        //start main container
        ProfileImpl mProfile = new ProfileImpl(null,1099,null);
        jade.wrapper.AgentContainer mainContainer = rt.createMainContainer(mProfile);
        
        
        //starting RMA agent for monitoring purposes
        try {
            AgentController agentRMA = mainContainer.createNewAgent("RMA","jade.tools.rma.rma", null);
            agentRMA.start();
        } catch (StaleProxyException ex) {
            Logger.getLogger(SubCoordinator.class.getName()).log(Level.SEVERE, null, ex);
        }
                */
        
	int port = Integer.parseInt(args[0]);
        SubCoordinator server = new SubCoordinator(port,sizeOfThreadPool);
        new Thread(server).start();
    
                
    }

}
