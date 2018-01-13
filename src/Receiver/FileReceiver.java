package Receiver;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

import Package.Package;

public class FileReceiver {
	
	public static void main(String[] args) throws SocketException {
		
		DatagramSocket sock = new DatagramSocket(5000);
		sock.setSoTimeout(5000);
		
		while(true) {
			byte[] buffer = new byte[1400];
			DatagramPacket incomingPacket = new DatagramPacket(buffer, buffer.length);
			
			try {
				sock.receive(incomingPacket);
				Package pak = new Package(incomingPacket);
				System.out.println("Package erhalten");
			}
			catch (SocketTimeoutException s) {
				//nochmal senden lassen
				System.out.println("Timeout");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}		
	}
}
