package Receiver;

public enum ReceiverState {
	
	WAIT,
	CHECK_DATA,
	SEND_ACK_FALSE,
	SEND_ACK_true,
	GOT_LAST,
	END;

}
