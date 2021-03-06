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
	private final int sport = 5001;
	private DatagramPacket backupDataPacket;
	private byte[] toSendFile;
	private int seq = 0;
	private boolean ack = false;
	private int positionArray;
	private SenderState currentState;
	private boolean status = true;
	private Package backupPacket;
	private static boolean fin = false;
	private Transition[][] trans = new Transition[SenderState.values().length][SenderMsg.values().length];
	private static int failTime = 0;

	
	public FilerSender(String filename, InetAddress ip) {
		currentState = SenderState.START;
		trans = new Transition[SenderState.values().length] [SenderMsg.values().length];
		trans[SenderState.START.ordinal()][SenderMsg.set_up_first.ordinal()] = p -> {
			prepare();
			return SenderState.SEND;
		};
		trans[SenderState.SEND.ordinal()][SenderMsg.wait_ack.ordinal()] = p -> {
			System.out.println("Waiting for ACK.");
			waitForIncomingPacket();
			return SenderState.WAIT_FOR_ACK;
		};
		
		trans[SenderState.WAIT_FOR_ACK.ordinal()][SenderMsg.ack_true.ordinal()] = p -> {
			System.out.println("Ack true");
			prepare();
			return SenderState.SEND;
		};
		
		trans[SenderState.WAIT_FOR_ACK.ordinal()][SenderMsg.ack_false.ordinal()] = p -> {
			sendBackupPack();
			System.out.println("Got false ack. Setting up new Data");
			return SenderState.SEND;
		};
		
		trans[SenderState.WAIT_FOR_ACK.ordinal()][SenderMsg.received_fin.ordinal()] = p -> {
			System.out.println("Got FIN end now.");
			return SenderState.END;
		};
		
		System.out.println("INFO FSM constructed, current state: "+currentState);
		
		this.file = new File(filename);
		this.ip = ip;
	
		try {
			this.fis = new FileInputStream(file);
			this.toSendFile = fileToByteArray();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		try {
			sock = new DatagramSocket(port);
			sock.setSoTimeout(1000);
		} catch (SocketException e) {
			e.printStackTrace();
		}
	}
	
	private void sendPacket(Package pak) throws IOException {
		DatagramPacket dpak = pak.PackageToDatagramPacket();
		backupPacket = pak;
		sendPacket(dpak);
	}
	
	private void sendPacket(DatagramPacket dpak) throws IOException {
		dpak.setAddress(ip);
		dpak.setPort(sport);
		backupDataPacket = dpak;
		sock.send(dpak);
	}
	
	private void waitForIncomingPacket() {
		boolean gotpackage = false;
			byte[] buffer = new byte[1400];
			DatagramPacket incomingPacket = new DatagramPacket(buffer, buffer.length);
			if (failTime < 3) {
				while(!gotpackage) {
					try {
						sock.receive(incomingPacket);
						failTime = 0;
						Package pak = new Package(incomingPacket);
						fin = pak.getFin();
						Package test = new Package(pak.getFilename(), pak.getSeqNum(), pak.getAck(), pak.getFin(), pak.getContent());
						long check = test.getCheckSum();
						if (pak.getSeqNum() == seq) {
							System.out.println("Seq in Ordnung");
							if (pak.getAck()) {
								System.out.println("Ack in Ordnung");
								ack = true;
								if (check == pak.getCheckSum()) {
									System.out.println("Checksum passt");
									System.out.println("Package erhalten");
									System.out.println(pak.getAck() + "," + pak.getFilename() + "," + pak.getFin() + "," + pak.getSeqNum());
									gotpackage = true;
									if (!fin) {
										prepare();
									}
								}
								else {
									System.out.println("Checksum falsch");
									sendBackupPack();
								}
							}
							else {
								System.out.println("Ack falsch");
								sendBackupPack();
							}
						}
						else {
							System.out.println("Seq falsch");
							sendBackupPack();
						}
					}
					catch (SocketTimeoutException s) {
						System.out.println("Timeout");
						failTime++;
						if (failTime >= 3) {
							break;
						}
						System.out.println(failTime);
						sendBackupPack();
						System.out.println("Resend Backup-Packet");
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			else {
				fin = true;
			}
	}
	
	private void sendBackupPack() {
		try {
			sendPacket(backupDataPacket);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void prepare(){
		if (ack) {
			if (seq == 0) {
				seq = 1;
			}
			else {
				seq = 0;
			}
		}
		try {
			Package pack = setupPackage();
			sendPacket(pack);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private byte[] fileToByteArray() {
		byte[] data = null;
        try {
            fis = new FileInputStream(file);
            data = new byte[(int) file.length()];
            fis.read(data);
            
        } 
        catch (FileNotFoundException e) {
            e.printStackTrace();
        } 
        catch (IOException e) {
            e.printStackTrace();
        } 
        finally {
            try {
                if (fis != null) 
                	fis.close();
            } 
            catch (IOException e) {}
        }
        return data; 
	}
	
	private byte[] splitSendArray() {
		byte[] splittedArray;
		if (toSendFile.length - positionArray >= 1000) {
			splittedArray = new byte[1000];
			System.arraycopy(toSendFile, positionArray, splittedArray, 0, 1000);
			positionArray += 1000;	
		}
		else {
			splittedArray = new byte[toSendFile.length - positionArray];
			System.arraycopy(toSendFile, positionArray, splittedArray, 0, toSendFile.length - positionArray);
			positionArray += (toSendFile.length - positionArray);	
		}
		return splittedArray;
	}
	
	private Package setupPackage() throws IOException {
		byte[] send = splitSendArray();
		if (positionArray >= toSendFile.length) {
			Package pack = new Package(file.getName(), seq, true, true, send);
			return pack;
		}
		else {
			Package pack = new Package(file.getName(), seq, true, false, send);
			System.out.println(file.getName());
			return pack;
		}
	}
	
	public static void main(String[] args) {
		try {
			new FilerSender(args[0], InetAddress.getByName(args[1]));
		} catch (ArrayIndexOutOfBoundsException e) {
			try {
				FilerSender fs = new FilerSender("default.txt", InetAddress.getByName("10.179.5.174"));
				while(!fin) {
					fs.prepare();
					fs.waitForIncomingPacket();
				}
			} catch (UnknownHostException e1) {
				e1.printStackTrace();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@FunctionalInterface
	private interface Transition {
		SenderState execute(SenderMsg msg);
	}
	
	public void processMsg(SenderMsg input){
		System.out.println("INFO Received "+input+" in state "+currentState);
		Transition t = trans[currentState.ordinal()][input.ordinal()];
		if(trans != null){
			currentState = t.execute(input);
		}
		System.out.println("INFO State: "+currentState);
	}

}
