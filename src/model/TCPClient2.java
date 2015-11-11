package model;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import model.exceptions.AbmeldenNichtM�glichException;

public class TCPClient2 extends Thread{

    /* Portnummer */
    private int serverPort;

    /* Hostname */
    private String hostname;
    
    /* Chatname */
    private String chatName;

    private Socket clientSocket; // TCP-Standard-Socketklasse

    private DataOutputStream outToServer; // Ausgabestream zum Server
    private BufferedReader inFromServer; // Eingabestream vom Server
    private StringBuffer nachrichtenVerlauf;
    
    List<ClientObserver> observer;
    
    public String teilnehmer;
    
    public boolean serviceRequested = true; // Client beenden?

    public TCPClient2(String hostname, int serverPort, String chatName) throws UnknownHostException, IOException {
        this.serverPort = serverPort;
        init(hostname,chatName);
    }
    
    public TCPClient2(String hostname, String chatName) throws IOException{
        this.serverPort = 56789;
        init(hostname,chatName);
    }

    public void init(String hostname,String chatName) throws UnknownHostException, IOException{  	
        this.hostname = hostname;
        this.chatName = chatName;
        this.nachrichtenVerlauf = new StringBuffer();
        observer = new ArrayList<>();
        teilnehmer ="";
        
        /* Socket erzeugen --> Verbindungsaufbau mit dem Server */
        clientSocket = new Socket(hostname, serverPort);

        /* Socket-Basisstreams durch spezielle Streams filtern */
        outToServer = new DataOutputStream(clientSocket.getOutputStream());
        inFromServer = new BufferedReader(new InputStreamReader(
        		clientSocket.getInputStream()));
    }
    
    public void run(){
    	
    	while(serviceRequested){
    		
    		try {
				this.sleep(5000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    		informiereObserver();
    	}
    }

    public void writeToServer(String request) throws IOException {
        /* Sende eine Zeile (mit CRLF) zum Server */
        outToServer.writeBytes(request + '\r' + '\n');
        System.out.println("TCP Client has sent the message: " + request);
    }

    private String readFromServer() throws IOException {
        /* Lies die Antwort (reply) vom Server */
        String reply = inFromServer.readLine();
        System.out.println("TCP Client got from Server: " + reply);
        
        return reply;

    }
    
    public void nachrichtSenden(String nachricht) throws IOException{
    	writeToServer(nachricht);
    	
    	String modifiedSentence; // vom Server modifizierter String
    	
        /* Modifizierten String vom Server empfangen */
        modifiedSentence = readFromServer();
        
        nachrichtenVerlauf.append("\n"+modifiedSentence); 
        informiereObserver(); 
    }
    
    /**
     * Bevor der Client in den Chatraum darf, muss er sich anmelden.
     * Diese Methode regelt diesen Prozess.
     * 
     * @throws IllegalAccessException
     * @throws IOException
     */
	public void anmelden() throws IllegalAccessException, IOException{
        nachrichtenVerlauf = new StringBuffer();
		writeToServer("ChatName:"+chatName);
		
		String answer = readFromServer();
		
		if(!answer.equals("ChatName:OK")){
			throw new IllegalAccessException("Der Chatraum konnte nicht betreten werden.");
		}
		
		System.out.println("kommt nicht bis hier");
		answer = readFromServer(); // Willkommensgru�
		nachrichtenVerlauf.append("\n"+answer);
		
		answer = readFromServer(); // Weitere Nachrichten des Servers entgegennehmen
		
		if(answer.startsWith("CHAT_MEMBERS:UPDATED")){
			teilnehmer = readFromServer();
		}
		informiereObserver();
	}
	
	/**
	 * Erm�glicht es sich als Client beim Chat abzumelden.
	 * @throws IOException
	 * @throws AbmeldenNichtM�glichException 
	 */
	public void abmelden() throws IOException, AbmeldenNichtM�glichException{
        String modifiedSentence; // vom Server modifizierter String
		try {
			writeToServer("SERVER_QUIT:"+chatName);
			
			 /* Modifizierten String vom Server empfangen */
            modifiedSentence = readFromServer();

            /* Test, ob Client beendet werden soll */
            if (modifiedSentence.startsWith("CLIENT_QUIT:OK")){
            	modifiedSentence = readFromServer(); // Verabschiedung lesen 
            	nachrichtenVerlauf.append(modifiedSentence);
                informiereObserver();
    			clientSocket.close();
            } else{
            	throw new AbmeldenNichtM�glichException();
            }
            
//            nachrichtenVerlauf.append("\n<Tsch�ss, "+chatName +"! Bis zum n�chsten mal!>");

			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			throw new IOException("Der Server konnte die Anfrage nicht bearbeiten.");
		}
	}
	
	/**
	 * Liefert den aktuellen Nachrichtenverlauf.
	 * Der Client speichert seinen eigenen Verlauf.
	 * @return
	 */
	public String getNachrichtenverlauf(){
		return nachrichtenVerlauf.toString();
	}
	
    /**
     * Mit addObserver kann man diesem Client einen Beobachter hinzuf�gen.
     * @param o
     */
    public void addObserver(ClientObserver o){
    	observer.add(o);
    }
    
    /**
     * Mit removeObserver kann man einen Beobachter wieder entfernen.
     * @param o
     */
    public void removeObserver(ClientObserver o){
    	observer.remove(o);
    }
    
    /**
     * Mit informiereObserver kann man allen Beobachtern mitteilen, dass der Client eine Nachricht empfangen hat.
     * @param o
     */
    public void informiereObserver(){
    	for(ClientObserver o : observer){
    		o.update();
    	}
    }
}