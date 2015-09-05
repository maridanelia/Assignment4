
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import javax.swing.*;

public class WebWorker extends Thread {
	/*
	 * This is the core web/download i/o code...
	 */
	private String result;
	private static final String ERROR_MESSAGE = "err";
	private static final String INTERRUPTED_MESSAGE = "Interrupted";
	private WebFrame frame;
	private String urlString;

	private void download(String urlString) throws InterruptedException{
		long start = System.currentTimeMillis();

		InputStream input = null;
		StringBuilder contents = null;
		try {
			URL url = new URL(urlString);
			URLConnection connection = url.openConnection();

			// Set connect() to throw an IOException
			// if connection does not succeed in this many msecs.
			connection.setConnectTimeout(5000);

			connection.connect();
			input = connection.getInputStream();

			BufferedReader reader = new BufferedReader(new InputStreamReader(
					input));
			
			if(this.isInterrupted()) throw new InterruptedException();
			char[] array = new char[1000];
			int len;
			contents = new StringBuilder(1000);
			while ((len = reader.read(array, 0, array.length)) > 0) {
				if(this.isInterrupted()||frame.isInterruped()) {
					reader.close();
					throw new InterruptedException();
				}
				contents.append(array, 0, len);
				Thread.sleep(100);
			}

			long end = System.currentTimeMillis();
			long elapsed = end - start;
			Calendar cal = Calendar.getInstance();
			SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");
			StringBuilder build = new StringBuilder();
			build.append(format.format(cal.getTime()));
			build.append(" ");
			build.append(elapsed);
			build.append("ms ");
			build.append(contents.length());
			build.append(" bytes");
			result = build.toString();

		}
		// Otherwise control jumps to a catch...
		catch (MalformedURLException e) {
			result = ERROR_MESSAGE;
		}  catch (IOException ignored) {
			result = ERROR_MESSAGE;
		}
		// "finally" clause, to close the input stream
		// in any case
		finally {
			
			try {
				if (input != null)
					input.close();
				
			} catch (IOException ignored) {
			}
		}
	}

	WebWorker(WebFrame frame, String urlString) {
		super();
		this.frame = frame;
		this.urlString = urlString;
	}

	@Override
	public void run() {
		try {
			frame.threadStarted();
			download(urlString);
		} catch (InterruptedException e) {
			result = INTERRUPTED_MESSAGE;
		}
		
		frame.release();
		if(this.isInterrupted()) return;
		frame.threadFinished(urlString, result);
	}
}
