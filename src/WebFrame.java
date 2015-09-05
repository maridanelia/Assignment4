import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.ScrollPane;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.plaf.ProgressBarUI;
import javax.swing.table.DefaultTableModel;


public class WebFrame  extends JFrame{
	//downloads links using single thread
	private JButton singleFetchButton;
	//download using multiple threads
	private JButton concurentFetchButton;
	//stop download
	private JButton stopButton;
	
	
	//texxtfield to enter number of concurent worker threads
	private JTextField textField;
	private JProgressBar progressBar;
	
	private DefaultTableModel model;
	private JTable table;
	private JScrollPane scrollPane;
	
	private JLabel runningLabel;
	private JLabel completedLabel;
	private JLabel elapsedLabel;
	
	private static final String RUNNING_LABEL_TEXT = "Running: ";
	private static final String COMPLETED_LABEL_TEXT = "Completed: ";
	private static final String ELAPSED_TEXT = "TIME_ELAPSED: ";
	
	private int running = 0;
	private int completed = 0;
	private double elapsed = 0;
	
	private Semaphore semaphore;
	
	private AtomicBoolean isInterrupted;
	
	private BufferedReader in;
	private static final String INPUT_FILE = "links.txt";
	//checks if user has requested to interrupt work of current application.
	public boolean isInterruped(){
		return isInterrupted.get();
	}
	
	
	private class LauncherThread extends Thread{
		/*
		 * launcher thread launches worker threads.
		 */
		private int threadsRun;
		private BufferedReader in;
		
		private LauncherThread(int threadsRun, BufferedReader in){
			this.threadsRun = threadsRun;
			this.in = in;
		}
		@Override
		public void run(){
			semaphore = new Semaphore(threadsRun);
			while(true){
				if(WebFrame.this.isInterruped()) break;
				if(this.isInterrupted()) break;
				try{					
					
					String url = in.readLine();
					WebWorker worker = new WebWorker(WebFrame.this, url);
					semaphore.acquire();
					worker.start();
					if(url == null) break;
					
				}	catch (InterruptedException e){
					break;
				}	catch (IOException ignore){
					
				}
				
				
			}
		}
	}
	
	
	//acquire semaphore permit
	void acquire() throws InterruptedException{
		semaphore.acquire();
	}
	//release semaphore permit
	void release(){
		semaphore.release();
	}
	//constructor
	public WebFrame(String fileName){
		super("Web Loader");
		initialize();
		allign();
		
		
		
	}
	
	
	//adds and alligns elements of user interface
	private void allign(){
		this.setMinimumSize(new Dimension(200, 400));
		JPanel controlPanel = new JPanel();
		controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.Y_AXIS));
		controlPanel.add(singleFetchButton);
		controlPanel.add(concurentFetchButton);
		controlPanel.add(textField);
		textField.setMaximumSize(new Dimension(500,20));
		controlPanel.add(runningLabel);
		controlPanel.add(completedLabel);
		controlPanel.add(elapsedLabel);
		controlPanel.add(progressBar);
		progressBar.setMaximumSize(new Dimension(1000,30));
		controlPanel.add(stopButton);
		
		controlPanel.setMinimumSize(new Dimension(100, 200));
		
		
		JPanel proxy = new JPanel();
		proxy.setLayout(new BoxLayout(proxy,BoxLayout.Y_AXIS));
		proxy.add(scrollPane);
		proxy.add(controlPanel);
		this.add(proxy);
	}
	

	private void initialize(){
		//add buttons
		singleFetchButton = new JButton("Single Thread Fetch");
		concurentFetchButton = new JButton("Concurent Fetch");
		stopButton = new JButton("Stop");
		stopButton.setEnabled(false);
		
		//add labels
		runningLabel = new JLabel(RUNNING_LABEL_TEXT+running);
		completedLabel = new JLabel(COMPLETED_LABEL_TEXT+completed);
		elapsedLabel = new JLabel(ELAPSED_TEXT+elapsed);
		
		textField = new JTextField();
		progressBar = new JProgressBar();
		
		
		//create and add table to display results
		model = new DefaultTableModel(new String [] {"url", "status"}, 1);
		table = new JTable(model);
		scrollPane = new JScrollPane(table);
		addFetchListeners();
		addStopButtonListener();
		
		try {
			
			in = new BufferedReader(new FileReader(INPUT_FILE));
		} catch (Exception e) {
			e.printStackTrace();
			SwingUtilities.invokeLater(new Runnable() {
				
				@Override
				public void run() {
					
					stopButton.setEnabled(false);
					singleFetchButton.setEnabled(false);
					concurentFetchButton.setEnabled(false);
				}
			});
		}
	}
	public static void main(String args[]) {
		WebFrame fr = new WebFrame("file.txt");
		fr.pack();
		fr.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		fr.setVisible(true);
	}

	private void addStopButtonListener(){
		stopButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				stopButton.setEnabled(false);
				singleFetchButton.setEnabled(true);
				concurentFetchButton.setEnabled(true);
				
				isInterrupted.set(true);
			}
		});
	}
	
	private void addFetchListeners(){
		
		singleFetchButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				fetch(1);
				stopButton.setEnabled(true);
			}
		});
		
		concurentFetchButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				fetch(Integer.parseInt(textField.getText()));
				stopButton.setEnabled(true);
			}
		});
	}
	
	private void fetch(int numberOfThreads){
		isInterrupted = new AtomicBoolean(false);
		singleFetchButton.setEnabled(false);
		concurentFetchButton.setEnabled(false);
		(new LauncherThread(numberOfThreads, in)).start();
	}
	
	void threadFinished(String url, String result){		
		
		final String [] rowToAdd = new String [2];
		rowToAdd [0] = url;
		rowToAdd [1] = result;
		
		synchronized (this) {
			if(isInterruped()) return;
			running--;
			completed++;
			SwingUtilities.invokeLater(new Runnable() {
				
				@Override
				public void run() {
					runningLabel.setText(RUNNING_LABEL_TEXT + running);
					completedLabel.setText(COMPLETED_LABEL_TEXT+completed);
					progressBar.setValue(completed);
					model.addRow(rowToAdd);
				}
			});
		}
	}
	
	void threadStarted(){
		
		synchronized (this) {
			
			running++;
			
			SwingUtilities.invokeLater(new Runnable() {
				
				@Override
				public void run() {					
					completedLabel.setText(COMPLETED_LABEL_TEXT+completed);					
				}
			});
		}
	}
}
