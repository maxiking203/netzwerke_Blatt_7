package Sender;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import Package.Package;

public class FilerSender {
	
	private File file;
	private InetAddress ip;
	private FileInputStream fis;
	private DatagramSocket sock;
	private final int port = 5000;
	private DatagramPacket backupPacket;
	
	public FilerSender(String filename, InetAddress ip) throws FileNotFoundException, SocketException {
		this.file = new File(filename);
		this.ip = ip;
		
		fis = new FileInputStream(file);
		sock = new DatagramSocket();
		sock.setSoTimeout(500);
		
	}
	
	private void sendPacket(Package pak) throws IOException {
		DatagramPacket dpak = pak.PackageToDatagramPacket();
		sendPacket(dpak);
	}
	
	private void sendPacket(DatagramPacket dpak) throws IOException {
		dpak.setAddress(ip);
		dpak.setPort(port);
		sock.send(dpak);
		backupPacket = dpak;
	}
	
	private void waitForIncomingPacket() {
		byte[] buffer = new byte[1400];
		DatagramPacket incomingPacket = new DatagramPacket(buffer, buffer.length);
		
		try {
			sock.receive(incomingPacket);
			Package pak = new Package(incomingPacket);
			long check = pak.getCheckSum();
			pak.setChecksum();
			if (check != pak.getCheckSum()) {
				//resend backupPacket
			}
			else {
				//proceed
			}
		}
		catch (SocketTimeoutException s) {
			//resend backupPacket
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		try {
			new FilerSender(args[0], InetAddress.getByName(args[1]));
		} catch (ArrayIndexOutOfBoundsException e) {
			try {
				new FilerSender("default", InetAddress.getByName("127.0.0.1"));
			} catch (FileNotFoundException | SocketException | UnknownHostException e1) {
				e1.printStackTrace();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
}

}
