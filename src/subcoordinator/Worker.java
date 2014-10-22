/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package subcoordinator;

import jade.core.ProfileImpl;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import jade.core.Runtime;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;

/**
 *
 * @author Ethan_Hunt
 */
public class Worker implements Runnable{

    protected Socket clientSocket = null;
    protected String serverText   = null;
    private jade.wrapper.ContainerController agentSmithContainer;
    
    private static final Logger LOGGER =
        Logger.getLogger(Worker.class.getName());
    
    
    public Worker(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }
    
    @Override
    public void run() {
        LOGGER.entering(getClass().getName(), "run()");
        
        try {
            InputStreamReader inputStreamReader  = new InputStreamReader(clientSocket.getInputStream());
            final BufferedReader input = new BufferedReader(inputStreamReader);
            final PrintStream printStream = new PrintStream(clientSocket.getOutputStream(),true);
            
            LOGGER.log(Level.INFO, "Connection from " + clientSocket.getInetAddress().getHostAddress());
            String inputLine;        
            
            //for for starting: start#interval#serverAddress#serverPort
            while ((inputLine = input.readLine()) != null) {
                final String commands[] = inputLine.split("#");
                if (commands[0].equalsIgnoreCase("start")){
                    long interval = Long.decode(commands[1]) ;
                    String serverAddress = commands[2];
                    int serverPort = Integer.decode(commands[3]);
                    launchAgentSmith(interval,serverAddress,serverPort);
                    //printStream.println(result);
                    LOGGER.log(Level.INFO, "Agent Started");
                    break;
                }
            } 
            LOGGER.log(Level.INFO, "Client has left"); 
            
            input.close();
            printStream.close();
            clientSocket.close();
            
            
        } catch (IOException e) {
            //report exception somewhere.
            e.printStackTrace();
        }
        
        LOGGER.exiting(getClass().getName(), "run()");
        
    }

    private void launchAgentSmith(long interval, String serverAddress, int serverPort){
        Runtime rt = Runtime.instance();
        // Exit the JVM when there are no more containers around
        rt.setCloseVM(true);
        System.out.print("runtime created\n");
        
        //agentsList = new ArrayList<>();
        //listOfProcesses = new ArrayList<>();
        AgentController agentSmith;
        //Profile mProfile = new ProfileImpl("192.168.0.102", startingPort+i,"Platform-"+i+":"+(startingPort+i),false);
        //Profile mProfile = new ProfileImpl("192.168.0.102", startingPort+i,null);
        //jade.wrapper.AgentContainer mainContainer = rt.createMainContainer(mProfile);
        //System.out.println("main container created "+mainContainer);
        //mainContainersList.add(mainContainer);
        ProfileImpl pContainer = new ProfileImpl();//null, startingPort+i,null);
        agentSmithContainer = rt.createAgentContainer(pContainer);
        System.out.println("containers created "+pContainer);
        Object[] smithArgs = new Object[4];
        smithArgs[0] = interval;
        smithArgs[1] = serverAddress;
        smithArgs[2] = serverPort;
        //smithArgs[3] = getAID(); //the subcoordinator's aid
        try {
            agentSmith = agentSmithContainer.createNewAgent("_Smith-"+SubCoordinator.agentNumber,
                    "agentsmith.AgentSmith", smithArgs);
            agentSmith.start();
        } catch (StaleProxyException ex) {
            Logger.getLogger(Worker.class.getName()).log(Level.SEVERE, null, ex);
        }
        SubCoordinator.agentNumber++;
    }
}
