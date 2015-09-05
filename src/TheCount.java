import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;


public class TheCount {
	
	private static class Panel extends JPanel{
		private JTextField textField;
		private JLabel countLabel;
		private JButton startButton;
		private JButton stopButton;
		
		private int currentCount;
		
		private static final  int TEXT_FIELD_SIZE = 10;
		private static final  int TEXT_FIELD_MAX_HEIGHT = 20;
		private static final  int TEXT_FIELD_MAX_WIDTH = 250;
		
		private static final int DEFAULT_COUNT = 0;
		
		private volatile WorkerThread currentWorker = null;
		
		Panel(){
			super();
			textField = new JTextField(TEXT_FIELD_SIZE);
			textField.setMaximumSize(new Dimension(TEXT_FIELD_MAX_WIDTH, TEXT_FIELD_MAX_HEIGHT));
			countLabel = new JLabel(Integer.toString(currentCount));
			startButton = new JButton("Start");
			stopButton = new JButton("Stop");
			
			this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
			this.add(textField);
			this.add(countLabel);
			this.add(startButton);
			this.add(stopButton);
			
			addActionListeners();
			
		}
		private void startNewThread(){
			currentWorker = new WorkerThread(this);
			currentWorker.start();
		}
		private void addActionListeners(){
			startButton.addActionListener(new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent arg0) {
					System.out.println();
					synchronized (this) {
						if(currentWorker==null){
							startNewThread();
						}	else {
							stop();
							startNewThread();
						}
					}
					
				}
			});
			
			
			stopButton.addActionListener(new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent e) {
					stop();
					
				}
			});
		}
		
		private synchronized void stop(){
			if(currentWorker != null){
				currentWorker.interrupt();
				currentWorker = null;							
			}
		}
	}
	
	private static class WorkerThread extends Thread{
		private final int count;
		private final Panel panel;
		
		public WorkerThread(Panel panel){
			super();
			this.count = Integer.parseInt(panel.textField.getText());
			this.panel = panel;
		}
		
		@Override
		public void run(){
			for(int i=0; i<=count; i++){
				
				try {
					this.sleep(50);
				} catch (Exception e) {
					this.interrupt();
				}
				
				if(isInterrupted()){
					return;
				}
				if(i%10==0){
					final String labelValue = Integer.toString(i);
					SwingUtilities.invokeLater(new Runnable() {
						
						@Override
						public void run() {
							panel.countLabel.setText(labelValue);
							
						}
					});
				}
			}
		}
	}
	
	private static final int NUMBER_OF_PANELS = 4;
	public static void main(String args[]){
		JFrame frame = new JFrame("The Count");
		
		for(int i=0; i<NUMBER_OF_PANELS; i++){
			frame.add(new Panel());
		}
		frame.setLayout(new GridLayout(NUMBER_OF_PANELS, 1));
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);
	}
}
