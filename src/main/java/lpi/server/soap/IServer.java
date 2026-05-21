package lpi.server.soap;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.jws.soap.SOAPBinding.Style;
import javax.jws.soap.SOAPBinding.Use;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.ws.WebFault;

/**
 * Defines the operations provided by the server.
 * @author RST 
 */
@WebService(name = "IChatServer", serviceName="ChatServer", portName="ChatServerProxy")
@SOAPBinding(style = Style.DOCUMENT, use=Use.LITERAL)
public interface IServer {

	/**
	 * Simplest method that does not accept any parameters and does not return
	 * any result. The easiest way to ensure everything works as expected.
	 */
	@WebMethod
	public void ping();

	/**
	 * Next method to test client-server communication and parameter passing.
	 * 
	 * @param text
	 *            Any text you want to send to the server.
	 * @return The text you sent prepended with the "ECHO:".
	 */
	@WebMethod
	public String echo(@WebParam(name="text") String text);

	/**
	 * Allows logging in the user.
	 * 
	 * @param login
	 *            The unique name of the user.
	 * @param password
	 *            The password of the user.
	 * @return The session id that should be used in the consequent calls.
	 */
	@WebMethod
	public String login(@WebParam(name="login") String login, @WebParam(name="password") String password) throws LoginException, ArgumentException, ServerException;

	/**
	 * Provides a list of users currently active on the server.
	 * 
	 * @param sessionId
	 *            The session id assigned to you after <b>login</b> call.
	 * @return An array of user names.
	 */
	@WebMethod
	public String[] listUsers(@WebParam(name="sessionId") String sessionId) throws ArgumentException, ServerException;

	/**
	 * Sends the message to the user, registered on the server.
	 * 
	 * @param sessionId
	 *            The session id assigned to you after <b>login</b> call.
	 * @param msg
	 *            An message object that contains message information.
	 */
	@WebMethod
	public void sendMessage(@WebParam(name="sessionId") String sessionId, @WebParam(name="message") Message msg) throws ArgumentException, ServerException;

	/**
	 * Receives a message if there are any pending messages addressed to the
	 * logged in user associated with the specified sessionId.
	 * 
	 * @param sessionId
	 *            The session id assigned to you after <b>login</b> call.
	 * @return A message object or <b>null</b> if there are no pending messages.
	 */
	@WebMethod
	public Message receiveMessage(@WebParam(name="sessionId") String sessionId) throws ArgumentException, ServerException;

	/**
	 * Sends the file to the user, registered on the server.
	 * 
	 * @param sessionId
	 *            The session id assigned to you after <b>login</b> call.
	 * @param file
	 *            The file information that should be delivered to the receiver.
	 */
	@WebMethod
	public void sendFile(@WebParam(name="sessionId") String sessionId, @WebParam(name="file") FileInfo file) throws ArgumentException, ServerException;

	/**
	 * Receives a file if there are any pending files addressed to the logged in
	 * user associated with the specified sessionId.
	 * 
	 * @param sessionId
	 *            The session id assigned to you after <b>login</b> call.
	 * @return A file info object describing the file targeted to a user,
	 *         registered on the server.
	 */
	@WebMethod
	public FileInfo receiveFile(@WebParam(name="sessionId") String sessionId) throws ArgumentException, ServerException;

	/**
	 * Logs out the user, specified by the session id and disposes the session.
	 * 
	 * @param sessionId
	 *            The session id assigned to you after <b>login</b> call.
	 */
	@WebMethod
	public void exit(@WebParam(name="sessionId") String sessionId) throws ArgumentException, ServerException;

	/**
	 * @author RST The class that describes the textual message that should be
	 *         delivered to some user.
	 */
	@XmlRootElement
	@XmlAccessorType(XmlAccessType.FIELD)
	public static class Message implements Serializable {
		private static final long serialVersionUID = -2358472729391550082L;

		private String sender;
		private String receiver;
		private String message;

		/**
		 * Constructs an empty Message object. Should be used mainly for
		 * serialization purposes.
		 */
		public Message() {
		}

		/**
		 * Constructs a Message object with receiver and message content
		 * specified.
		 * 
		 * @param receiver
		 *            The targeted receiver of the message.
		 * @param message
		 *            The message itself (content).
		 */
		public Message(String receiver, String message) {
			this.receiver = receiver;
			this.message = message;
		}

		/**
		 * Constructs a Message object with receiver, sender and the message
		 * specified.
		 * 
		 * @param receiver
		 *            The targeted receiver of the message.
		 * @param sender
		 *            The sender of the message.
		 * @param message
		 *            The message itself (content).
		 */
		public Message(String receiver, String sender, String message) {
			this.sender = sender;
			this.receiver = receiver;
			this.message = message;
		}

		/**
		 * Gets the specified sender of the message.
		 * 
		 * @return A <b>String</b> that specifies the sender of the message.
		 */
		public String getSender() {
			return sender;
		}

		/**
		 * Sets the sender of the message.
		 * 
		 * @param sender
		 *            A <b>String</b> that specifies the sender of the message.
		 */
		public void setSender(String sender) {
			this.sender = sender;
		}

		/**
		 * Gets the receiver of the message.
		 * 
		 * @return A <b>String</b> that specifies the receiver of the message.
		 */
		public String getReceiver() {
			return receiver;
		}

		/**
		 * Sets the receiver of the message.
		 * 
		 * @param receiver
		 *            A <b>String</b> that specifies the receiver of the
		 *            message.
		 */
		public void setReceiver(String receiver) {
			this.receiver = receiver;
		}

		/**
		 * Gets the message content.
		 * 
		 * @return A <b>String</b> that specifies the message that should be
		 *         delivered to the receiver.
		 */
		public String getMessage() {
			return message;
		}

		/**
		 * Sets the message content.
		 * 
		 * @param message
		 *            A <b>String</b> that specifies the message that should be
		 *            delivered to the receiver.
		 */
		public void setMessage(String message) {
			this.message = message;
		}

		public String toString() {
			return String.format("Message from %s to %s: \"%s\"", this.sender, this.receiver, this.message);
		}
	}

	/**
	 * The class that describes a file that should be transferred to some user.
	 * @author RST 
	 */
	@XmlRootElement
	@XmlAccessorType(XmlAccessType.FIELD)
	public static class FileInfo implements Serializable {
		private static final long serialVersionUID = 8407920676195680991L;

		private String sender;
		private String receiver;
		private String filename;
		private byte[] fileContent;

		/**
		 * Constructs an empty File Info object. Should be used mainly for
		 * serialization purposes.
		 */
		public FileInfo() {
		}

		/**
		 * Constructs a File Info object with the receiver and the file
		 * specified.
		 * 
		 * @param receiver
		 *            The receiver that the file should be delivered to.
		 * @param file
		 *            The <b>File</b> object that points to a file that should
		 *            be transferred to receiver.
		 * @throws IOException
		 *             if the system failed to read or process the file.
		 */
		public FileInfo(String receiver, File file) throws IOException {
			this.receiver = receiver;
			this.filename = file.getName();
			this.fileContent = Files.readAllBytes(file.toPath());
		}

		/**
		 * Constructs a File Info object with the receiver, sender, filename and
		 * content specified.
		 * 
		 * @param receiver
		 *            The receiver that the file should be delivered to.
		 * @param sender
		 *            The sender that is sending this file.
		 * @param filename
		 *            The name of the file that should be transferred (usually
		 *            just a file name without a path).
		 * @param content
		 *            The content of the file to send.
		 */
		public FileInfo(String receiver, String sender, String filename, byte[] content) {
			this.sender = sender;
			this.receiver = receiver;
			this.filename = filename;
			this.fileContent = content;
		}

		/**
		 * Gets the sender of the file.
		 * 
		 * @return A <b>String</b> that defines the sender of the file.
		 */
		public String getSender() {
			return sender;
		}

		/**
		 * Sets the sender of the file.
		 * 
		 * @param sender
		 *            A <b>String</b> that defines the sender of the file.
		 */
		public void setSender(String sender) {
			this.sender = sender;
		}

		/**
		 * Gets the receiver of the file.
		 * 
		 * @return A <b>String</b> that defines the receiver of the file.
		 */
		public String getReceiver() {
			return receiver;
		}

		/**
		 * Sets the receiver of the file.
		 * 
		 * @param receiver
		 *            A <b>String</b> that defines the receiver of the file.
		 */
		public void setReceiver(String receiver) {
			this.receiver = receiver;
		}

		/**
		 * Gets the name of the file.
		 * 
		 * @return A <b>String</b> that defines a name of the file (only the
		 *         file name, without the file path).
		 */
		public String getFilename() {
			return filename;
		}

		/**
		 * Sets the name of the file.
		 * 
		 * @param filename
		 *            A <b>String</b> that defines the name of the file (only
		 *            the file name, without the file path).
		 */
		public void setFilename(String filename) {
			this.filename = filename;
		}

		/**
		 * Gets the content of the file.
		 * 
		 * @return An array of bytes that define the content of the file.
		 */
		public byte[] getFileContent() {
			return fileContent;
		}

		/**
		 * Sets the content of the file.
		 * 
		 * @param fileContent
		 *            An array of bytes that define the content of the file.
		 */
		public void setFileContent(byte[] fileContent) {
			this.fileContent = fileContent;
		}

		/**
		 * Saves the file to a specified location.
		 * 
		 * @param location
		 *            A <b>File</b> that specifies either the existing folder or
		 *            the full path where the file should be saved. In case the
		 *            complete filename is specified, the file will be
		 *            rewritten.
		 * @throws IOException
		 *             If the system failed to save the file to a specified
		 *             location.
		 */
		public void saveFileTo(File location) throws IOException {
			File fileLocation = location;

			// checking if we received a directory or a file.
			if (!location.isFile()) {
				// yep, that's a wood^w directory.
				if (!location.exists())
					throw new FileNotFoundException(
							String.format("The directory %s does not exist.", location.getCanonicalPath()));

				fileLocation = new File(location, this.filename);
				int i = 1;
				while (fileLocation.exists()) {
					fileLocation = new File(location, getIndexedFilename(this.filename, i++));
				}
			}

			// saving the file content to the calculated location.
			try (FileOutputStream fileStream = new FileOutputStream(fileLocation)) {
				fileStream.write(fileContent);
			}
		}

		/*
		 * Borrowed from http://stackoverflow.com/a/4546093 Does not support
		 * files like x.tar.gz (renames them to x.tar (index).gz
		 */
		private String getIndexedFilename(String filename, int index) {
			String[] parts = new String[] { filename };

			if (filename.contains("."))
				parts = filename.split("\\.(?=[^\\.]+$)");

			return String.format("%s_(%d)%s", parts[0], index, (parts.length > 1 ? "." + parts[1] : ""));
		}

		public String toString() {
			return String.format("File \"%s\"(%d kB) from %s to %s", this.filename,
					this.fileContent != null ? this.fileContent.length / 1024 : "null", this.sender, this.receiver);
		}
	}

	/**
	 * The class that describes a server-side error that occurred during request processing.
	 * @author RST 
	 */
	@WebFault(messageName="ServerFault", name = "ServerFault")
	public static class ServerException extends Exception {
		private static final long serialVersionUID = 2592458695363000913L;

		public ServerException() {
			super();
		}

		public ServerException(String message, Throwable cause) {
			super(message, cause);
		}

		public ServerException(String message) {
			super(message);
		}
	}

	/**
	 * The class that describes the login error that occurred.
	 * @author RST 
	 */
	@WebFault(messageName="LoginFault", name = "LoginFault")
	public static class LoginException extends Exception {
		private static final long serialVersionUID = -5682573656536628713L;

		public LoginException() {
			super();
		}

		public LoginException(String message, Throwable cause) {
			super(message, cause);
		}

		public LoginException(String message) {
			super(message);
		}
	}

	/**
	 * The class that describes an issue with the provided arguments.
	 * @author RST 
	 */
	@WebFault(messageName="ArgumentFault", name = "ArgumentFault")
	public static class ArgumentException extends Exception {
		private static final long serialVersionUID = 8404607085051949404L;

		private String argumentName;

		public ArgumentException() {
			super();
		}

		public ArgumentException(String argumentName, String message, Throwable cause) {
			super(message, cause);
			this.argumentName = argumentName;
		}

		public ArgumentException(String argumentName, String message) {
			super(message);
			this.argumentName = argumentName;
		}

		/**
		 * Gets the name of the argument that did not pass validation.
		 * 
		 * @return A name of the argument that was not valid.
		 * @see getMessage for validation error description
		 */
		public String getArgumentName() {
			return argumentName;
		}
	}
}
