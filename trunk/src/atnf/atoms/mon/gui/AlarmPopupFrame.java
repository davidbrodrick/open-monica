// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.InputStream;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import atnf.atoms.mon.Alarm;
import atnf.atoms.mon.AlarmEvent;
import atnf.atoms.mon.AlarmEventListener;
import atnf.atoms.mon.PointDescription;
import atnf.atoms.mon.client.AlarmMaintainer;
import atnf.atoms.mon.client.MonClientUtil;
import atnf.atoms.mon.gui.monpanel.AlarmManagerPanel;
import atnf.atoms.time.RelTime;

/**
 * Class that encapsulates the creation of a JFrame that holds the information about a 
 * given Alarm. Should generally be used only for high priority alarms, but could theoretically
 * be used for any alarm, up to the programmer's discretion.
 * <br/>This class extends the javax.swing.JFrame class, and utilises the atnf.atoms.mon.gui.AlarmPanel class
 * to draw the alarm details pane. 
 * @author Kalinga Hulugalle
 * @see JFrame
 * @see AlarmPanel
 */
public class AlarmPopupFrame extends JFrame implements ActionListener, AlarmEventListener{

	String username = "";
	String password = "";
	String itsName;
	PointDescription itsPointDesc;
	Alarm itsAlarm;
	boolean alive = true;
	AudioWarning klaxon = new AudioWarning();

	boolean shelved = false;

	/**
	 * 
	 */
	private static final long serialVersionUID = -2222706905265546584L;
	boolean active = false;
	AlarmPanel details;
	JScrollPane detailsScroller = new JScrollPane();
	JButton notify = new JButton("NOTIFY");
	JButton ack = new JButton("ACK");
	JButton shelve = new JButton("SHELVE");
	JButton dismiss = new JButton("DISMISS");
	JButton ignoreAlms = new JButton("Turn off Notifications");

	/**
	 * Constructor for a new AlarmPopupFrame
	 * @param a The Alarm object this AlarmPopupFrame is referencing
	 * @throws HeadlessException
	 */
	public AlarmPopupFrame(Alarm a) throws HeadlessException {
		if (a != null){
			AlarmMaintainer.addListener(this);
			itsAlarm = a;
			itsPointDesc = a.getPointDesc();
			itsName = itsPointDesc.getFullName();

			details = new AlarmPanel(itsName);
			this.setLayout(new BorderLayout());
			this.setMinimumSize(new Dimension(300,500));
			this.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
			this.setTitle("ALARM NOTIFICATION FOR POINT " + itsName);
			JPanel content = new JPanel(new BorderLayout());
			detailsScroller.setViewportView(details);
			notify.setToolTipText("Notify someone about this alarm through email.");
			notify.setFont(new Font("Sans Serif", Font.BOLD, 18));
			notify.addActionListener(this);
			notify.setActionCommand("notify");
			ack.setToolTipText("Acknowledge this alarm.");
			ack.setFont(new Font("Sans Serif", Font.BOLD, 18));
			ack.setActionCommand("ack");
			ack.addActionListener(this);
			if (itsAlarm.isShelved()) shelved = true;
			if (shelved){
				shelve.setText("UNSHELVE");
			} else {
				shelve.setText("SHELVE");
			}
			shelve.setToolTipText("Shelve this alarm.");
			shelve.setFont(new Font("Sans Serif", Font.BOLD, 18));
			shelve.setActionCommand("shelve");
			shelve.addActionListener(this);
			dismiss.setToolTipText("Dismiss this alarm.");
			dismiss.setFont(new Font("Sans Serif", Font.BOLD, 18));
			dismiss.setActionCommand("dismiss");
			dismiss.addActionListener(this);
			ignoreAlms.setToolTipText("Turn off automatic notifications.");
			ignoreAlms.setFont(new Font("Sans Serif", Font.BOLD, 18));
			ignoreAlms.setActionCommand("ignore");
			ignoreAlms.addActionListener(this);
			this.addWindowListener(new WindowAdapter(){
				public void windowClosing(WindowEvent we){
					AlarmMaintainer.removeListener(AlarmPopupFrame.this);
				}
			});
			JPanel optionButtons = new JPanel();
			optionButtons.setLayout(new GridLayout(2,2));
			optionButtons.add(notify);
			optionButtons.add(ack);
			optionButtons.add(shelve);
			optionButtons.add(dismiss);

			content.add(detailsScroller, BorderLayout.CENTER);
			content.add(optionButtons, BorderLayout.SOUTH);
			this.getContentPane().add(content, BorderLayout.CENTER);
			this.getContentPane().add(ignoreAlms, BorderLayout.SOUTH);
			this.pack();
			this.setLocationByPlatform(true);
			this.addWindowListener(new WindowListener(){
				@Override
				public void windowActivated(WindowEvent arg0) {}
				@Override
				public void windowClosed(WindowEvent arg0) {}
				@Override
				public void windowClosing(WindowEvent arg0) {
					AlarmPopupFrame.this.vaporise();
				}
				@Override
				public void windowDeactivated(WindowEvent arg0) {}
				@Override
				public void windowDeiconified(WindowEvent arg0) {}
				@Override
				public void windowIconified(WindowEvent arg0) {}
				@Override
				public void windowOpened(WindowEvent arg0) {}
			});
		}
	}

	/**
	 * Constructor for a blank AlarmPopupFrame
	 * @throws HeadlessException
	 */
	public AlarmPopupFrame() throws HeadlessException {
		itsName = "";
		details = new AlarmPanel();
		this.setLayout(new BorderLayout());
		this.setMinimumSize(new Dimension(300, 500));
		this.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		this.setTitle("ALARM NOTIFICATION FOR POINT " + itsName);
		JPanel content = new JPanel(new BorderLayout());
		//detailsScroller.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		detailsScroller.setViewportView(details);
		notify.setToolTipText("Notify someone about these alarms through email.");
		notify.setFont(new Font("Sans Serif", Font.BOLD, 18));
		notify.addActionListener(this);
		notify.setActionCommand("notify");
		ack.setToolTipText("Acknowledge these alarms.");
		ack.setFont(new Font("Sans Serif", Font.BOLD, 18));
		ack.setActionCommand("ack");
		ack.addActionListener(this);
		shelve.setToolTipText("Shelve this alarm.");
		shelve.setFont(new Font("Sans Serif", Font.BOLD, 18));
		shelve.setActionCommand("shelve");
		shelve.addActionListener(this);
		dismiss.setToolTipText("Dismiss this alarm.");
		dismiss.setFont(new Font("Sans Serif", Font.BOLD, 18));
		dismiss.setActionCommand("dismiss");
		dismiss.addActionListener(this);
		ignoreAlms.setToolTipText("Turn off automatic notifications.");
		ignoreAlms.setFont(new Font("Sans Serif", Font.BOLD, 18));
		ignoreAlms.setActionCommand("ignore");
		ignoreAlms.addActionListener(this);
		this.addWindowListener(new WindowAdapter(){
			public void windowClosing(WindowEvent we){
				AlarmMaintainer.removeListener(AlarmPopupFrame.this);
			}
		});
		JPanel optionButtons = new JPanel();
		optionButtons.setLayout(new GridLayout(1,4));
		optionButtons.add(notify);
		optionButtons.add(ack);
		optionButtons.add(shelve);
		optionButtons.add(dismiss);

		content.add(detailsScroller, BorderLayout.CENTER);
		content.add(optionButtons, BorderLayout.SOUTH);
		this.getContentPane().add(content, BorderLayout.CENTER);
		this.getContentPane().add(ignoreAlms, BorderLayout.SOUTH);
		this.pack();
		this.setLocationByPlatform(true);
	}
	
	public String getPointName(){
		return itsName;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		String command = e.getActionCommand();

		if (command.equals("notify")){
			MonClientUtil.showEmailPrompt(this);

		} else if (command.equals("ack")){
			String[] res = MonClientUtil.showLogin(this, username, password);
			username = res[0];
			password = res[1];
			if (username.isEmpty() || password.isEmpty()){
				password = ""; 
				return;
			}
			try{
				AlarmMaintainer.setAcknowledged(this.itsName, true, username, password);
				this.vaporise();
			} catch (Exception ex){
				password = "";
				JOptionPane.showMessageDialog(this, "Something went wrong with the sending of data. " +
						"\nPlease ensure that you're properly connected to the network, you are attempting to write to a valid point" +
						"\n and your username and password are correct.", "Data Sending Error", JOptionPane.ERROR_MESSAGE);
				return;
			}
		} else if (command.equals("shelve")){
			String[] res = MonClientUtil.showLogin(this, username, password);
			username = res[0];
			password = res[1];
			if (username.isEmpty() || password.isEmpty()){
				password = ""; 
				return;
			}
			try {
				shelved = !shelved;
				AlarmMaintainer.setShelved(this.itsName, shelved, username, password);
				this.vaporise();
			} catch (Exception ex){
				password = "";
				JOptionPane.showMessageDialog(this, "Something went wrong with the sending of data. " +
						"\nPlease ensure that you're properly connected to the network, you are attempting to write to a valid point" +
						"\n and your username and password are correct.", "Data Sending Error", JOptionPane.ERROR_MESSAGE);
				return;
			}
		}
	}
	
	private void vaporise(){
		AlarmMaintainer.removeListener(this);
		alive = false;
		AlarmMaintainer.popupMap.remove(itsPointDesc.getFullName());
		this.dispose();
	}


	@Override
	public void onAlarmEvent(AlarmEvent event) {
		if (event.getAlarm().getPointDesc().getFullName().equals(this.itsAlarm.getPointDesc().getFullName())){
			itsAlarm = event.getAlarm();
			itsPointDesc = itsAlarm.getPointDesc();
			AlarmPanel newPanel = new AlarmPanel(itsPointDesc.getFullName());
			this.detailsScroller.setViewportView(newPanel);
			if (itsAlarm.getPriority() >= 2 && !klaxon.isAlive()){
				klaxon.start();
			}
			if (itsAlarm.getAlarmStatus() != Alarm.ALARMING){
				this.vaporise();
			}
		}
	}

	/** Play an audio sample on the sound card. */
	private boolean playAudio(String resname) {
		RelTime sleep = RelTime.factory(1000000);
		try {
			InputStream in = AlarmManagerPanel.class.getClassLoader().getResourceAsStream(resname);
			AudioInputStream soundIn = AudioSystem.getAudioInputStream(in);
			DataLine.Info info = new DataLine.Info(Clip.class, soundIn.getFormat());
			Clip clip = (Clip) AudioSystem.getLine(info);
			clip.open(soundIn);
			sleep.sleep(); // Clips start of clip without this
			clip.start();
			// Wait until clip is finished then release the sound card
			while (clip.isActive()) {
				Thread.yield();
			}
			clip.drain();
			sleep.sleep(); // Clips end of clip without this
			clip.close();
		} catch (Exception e) {
			System.err.println("AlarmManagerPanel.playAudio: " + e.getClass());
			return false;
		}
		return true;
	}

	// Test for a blank popup frame
	public static void main(String[] args){
		AlarmPopupFrame apf = new AlarmPopupFrame();
		apf.setVisible(true);
	}
	// /////////////////////// NESTED CLASS ///////////////////////////////

	/**
	 * Class that extends Thread, to create an audio sound effect without tying up any other
	 * threads, such as the UI or logic threads.
	 */
	public class AudioWarning extends Thread {
		/**
		 * AudioWarning implementation of the Thread's run() method. Periodically activates
		 * and sounds an audio warning if there are high priority (priority >=2 ) alarms currently
		 * alarming.
		 */
		@Override
		public void run() {
			try {
				RelTime sleep = RelTime.factory(10000000);
				while (alive){
					boolean success = playAudio("atnf/atoms/mon/gui/monpanel/watchdog.wav");
					if (success == false) throw (new Exception());
					sleep.sleep();
				}
			} catch (Exception e) {
				System.err.println("Audio Playing failed");
			}
		}
	}
	// ///////////////////// END NESTED CLASS /////////////////////////////
}
