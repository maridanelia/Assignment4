import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.omg.CORBA.INITIALIZE;

public class Bank {

	/**
	 * 
	 * contains basic information of single account: ID and balance.
	 * 
	 */
	private class Account {
		final int ID;
		long balance;

		Account(int ID, int balance) {
			this.ID = ID;
			this.balance = balance;
		}

	}

	/**
	 * 
	 * Class contains information of individual transactions.
	 * Immutable
	 */
	private class Transaction {
		final int from;
		final int to;
		final long amount;

		/**
		 * 
		 * @param from
		 *            : account ID of class from which amount is being
		 *            transferred
		 * @param to
		 *            : account ID of class to which amount i being transferred
		 * @param amount
		 *            being transferred
		 */
		Transaction(int from, int to, long amount) {
			this.from = from;
			this.to = to;
			this.amount = amount;
		}

	}

	// countdown latch used to wait until all worker threads finish processing.
	private CountDownLatch latch;
	private static final int WORK_QUEUE_CAP = 20;
	private BlockingQueue<Transaction> workQueue;
	//value indicating that producer thread has finished adding work to the workqueue.
	private static final int END_VALUE = -1;
	
	
	private static final int NUMBER_OF_ACCOUNTS = 20;
	private static final int INITIAL_BALANCE = 1000;
	
	// key - ID, value - account
	private Map<Integer, Account> accounts;
	
	

	/*
	 * worker thread. takes transactions from workQueue and performs
	 * transactions until it encounters endqueue value;
	 */
	private class Worker extends Thread {
		@Override
		public void run() {
			while (true) {
				try {
					Transaction workItem = workQueue.take();
					if (workItem.amount == END_VALUE) {
						workQueue.put(workItem);
						break;
					}
					transfer(workItem);
				} catch (InterruptedException e) {

				}
			}
			latch.countDown();

		}

	}

	private synchronized void transfer(Transaction t) {
		Account from = accounts.get(t.from);
		Account to = accounts.get(t.to);

		from.balance -= t.amount;
		to.balance += t.amount;
	}

	public static void main(String args[]) throws InterruptedException{
	
		String filename = args[0];
		int numberOfWorkers;
		try {
			numberOfWorkers = Integer.parseInt(args[1]);
		} catch (NumberFormatException e) {
			System.out.println("Incorect integer value");
			return;
		}
		
		
		try {
			BufferedReader in = new BufferedReader(new FileReader(filename));
			Bank bank = new Bank();
			
			bank.initializeBank();
			bank.launchWorkers(numberOfWorkers);
			bank.produceWork(in);
			in.close();
			
			bank.latch.await();
			bank.printAccounts();
			
		} catch (FileNotFoundException e) {
			System.out.println("no such file");
		} catch (IOException e) {
			
		}
		
		
		
		

	}

	private void launchWorkers(int numWorkers) {
		latch = new CountDownLatch(numWorkers);
		for (int i = 0; i < numWorkers; i++) {
			(new Worker()).start();
		}
	}

	private void initializeBank(){
		accounts = new HashMap<>();
		workQueue = new ArrayBlockingQueue<>(WORK_QUEUE_CAP);
		for(int i = 0; i<NUMBER_OF_ACCOUNTS; i++){
			Account acc = new Account(i, INITIAL_BALANCE);
			accounts.put(i, acc);
		}
	}
	
	private void produceWork(BufferedReader in) throws IOException{
		while(true){
			String line = in.readLine();
			
			if(line == null) {
				workQueue.add(new Transaction(END_VALUE, END_VALUE, END_VALUE));
				break;
			}
			StringTokenizer tok = new StringTokenizer(line);
			int from = Integer.parseInt(tok.nextToken());
			int to = Integer.parseInt(tok.nextToken());
			int amount = Integer.parseInt(tok.nextToken());
			try {
				workQueue.put(new Transaction(from,to,amount));
			} catch (Exception e) {
				// TODO: handle exception
			}
		}
	}
	
	private  void printAccounts(){
		for(int i = 0; i<NUMBER_OF_ACCOUNTS; i++){
			System.out.println("account: "+i+" balance: "+accounts.get(i).balance);
		}
	}
}
