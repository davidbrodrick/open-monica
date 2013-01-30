// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.Border;

import atnf.atoms.mon.Alarm;
import atnf.atoms.mon.AlarmEvent;
import atnf.atoms.mon.AlarmEventListener;
import atnf.atoms.mon.PointDescription;
import atnf.atoms.mon.client.AlarmMaintainer;
import atnf.atoms.mon.util.MailSender;

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
			this.setMinimumSize(new Dimension(500, 800));
			this.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
			this.setTitle("ALARM NOTIFICATION FOR POINT " + itsName);
			JPanel content = new JPanel(new BorderLayout());
			//detailsScroller.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
			detailsScroller.setViewportView(details);
			notify.setToolTipText("Notify someone about these alarms through email.");
			notify.setFont(new Font("Sans Serif", Font.BOLD, 36));
			notify.addActionListener(this);
			notify.setActionCommand("notify");
			ack.setToolTipText("Acknowledge these alarms.");
			ack.setFont(new Font("Sans Serif", Font.BOLD, 36));
			ack.setActionCommand("ack");
			ack.addActionListener(this);
			if (itsAlarm.isShelved()) shelved = true;
			if (shelved){
				shelve.setText("UNSHELVE");
			} else {
				shelve.setText("SHELVE");
			}
			shelve.setToolTipText("Shelve the selected alarms.");
			shelve.setFont(new Font("Sans Serif", Font.BOLD, 36));
			shelve.setActionCommand("shelve");
			shelve.addActionListener(this);
			this.addWindowListener(new WindowAdapter(){
				public void windowClosing(WindowEvent we){
					AlarmMaintainer.removeListener(AlarmPopupFrame.this);
				}
			});
			JPanel optionButtons = new JPanel();
			optionButtons.setLayout(new GridLayout(1,3));
			optionButtons.add(notify);
			optionButtons.add(ack);
			optionButtons.add(shelve);
			JPanel confirmButtons = new JPanel();
			confirmButtons.setLayout(new GridLayout(1,2));

			content.add(detailsScroller, BorderLayout.CENTER);
			content.add(optionButtons, BorderLayout.SOUTH);
			this.getContentPane().add(content, BorderLayout.CENTER);
			this.getContentPane().add(confirmButtons, BorderLayout.SOUTH);
			this.pack();
			this.setLocationByPlatform(true);
			this.setVisible(false);
		} else {
			new AlarmPopupFrame();
			this.dispose();
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
		this.setMinimumSize(new Dimension(500, 800));
		this.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		this.setTitle("ALARM NOTIFICATION FOR POINT " + itsName);
		JPanel content = new JPanel(new BorderLayout());
		//detailsScroller.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		detailsScroller.setViewportView(details);
		notify.setToolTipText("Notify someone about these alarms through email.");
		notify.setFont(new Font("Sans Serif", Font.BOLD, 36));
		notify.addActionListener(this);
		notify.setActionCommand("notify");
		ack.setToolTipText("Acknowledge these alarms.");
		ack.setFont(new Font("Sans Serif", Font.BOLD, 36));
		ack.setActionCommand("ack");
		ack.addActionListener(this);
		shelve.setToolTipText("Shelve the selected alarms.");
		shelve.setFont(new Font("Sans Serif", Font.BOLD, 36));
		shelve.setActionCommand("shelve");
		shelve.addActionListener(this);

		JPanel optionButtons = new JPanel();
		optionButtons.setLayout(new GridLayout(1,3));
		optionButtons.add(notify);
		optionButtons.add(ack);
		optionButtons.add(shelve);
		JPanel confirmButtons = new JPanel();
		confirmButtons.setLayout(new GridLayout(1,2));

		content.add(detailsScroller, BorderLayout.CENTER);
		content.add(optionButtons, BorderLayout.SOUTH);
		this.getContentPane().add(content, BorderLayout.CENTER);
		this.getContentPane().add(confirmButtons, BorderLayout.SOUTH);
		this.pack();
		this.setLocationByPlatform(true);
		this.setVisible(false);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		String command = e.getActionCommand();

		if (command.equals("notify")){
			//notify via email
			String[] args = new String[3];
			JPanel inputs = new JPanel();
			JPanel tFieldsp = new JPanel();
			tFieldsp.setLayout(new GridLayout(3,2,0,5));
			JLabel lemail = new JLabel("CSIRO Recipient: ");
			JLabel lsubject = new JLabel("Subject: ");
			JLabel lbody = new JLabel("Body Text: ");
			lemail.setHorizontalAlignment(JLabel.LEFT);
			lsubject.setHorizontalAlignment(JLabel.LEFT);
			JTextField email = new JTextField(20);
			JTextField subject = new JTextField(20);
			JTextArea body = new JTextArea(5,20);
			Border border = BorderFactory.createLineBorder(Color.BLACK);
			body.setBorder(border);
			email.setBorder(border);
			subject.setBorder(border);

			tFieldsp.add(lemail);
			tFieldsp.add(email);
			tFieldsp.add(lsubject);
			tFieldsp.add(subject);
			tFieldsp.add(lbody);

			inputs.setLayout(new BoxLayout(inputs,BoxLayout.Y_AXIS));
			inputs.add(tFieldsp);
			inputs.add(body);

			int result = JOptionPane.showConfirmDialog(this, inputs, "Please fill out the following fields: ", JOptionPane.OK_CANCEL_OPTION);
			if (result == JOptionPane.OK_OPTION){
				args[0] = email.getText();
				args[1] = subject.getText();
				args[2] = body.getText();
				args[2] += "\n\n\n\t -- Sent via MoniCA Java Client";
				try {		

					//if (!args[0].endsWith("@csiro.au")) throw new IllegalArgumentException("Non-CSIRO Email");//Checks for correct email address
					MailSender.sendMail(args[0], args[1], args[2]);
					JOptionPane.showMessageDialog(this, "Email successfully sent!", "Email Notification", JOptionPane.INFORMATION_MESSAGE);
				} catch (IllegalArgumentException e0){
					JOptionPane.showMessageDialog(this, "Email sending failed!\n" +
							"You need to send this to a CSIRO email address!", "Email Notification", JOptionPane.ERROR_MESSAGE);
				} catch (Exception e1){
					JOptionPane.showMessageDialog(this, "Email sending failed!\n" +
							"You  may want to check your connection settings.", "Email Notification", JOptionPane.ERROR_MESSAGE);
				}
			}

		} else if (command.equals("ack")){
			if (username.equals("") || password.equals("")){
				JPanel inputs = new JPanel();
				inputs.setLayout(new GridBagLayout());
				GridBagConstraints gbc = new GridBagConstraints();
				gbc.fill = GridBagConstraints.HORIZONTAL;
				gbc.weightx = 0.5;
				gbc.gridx = 0;
				gbc.gridy = 0;
				JLabel usernameLabel = new JLabel("Username: ");
				JTextField usernameField = new JTextField(20);
				usernameField.setText(username);
				inputs.add(usernameLabel, gbc);
				gbc.gridx = 1;
				gbc.gridwidth = 3;
				inputs.add(usernameField, gbc);
				JLabel passwordLabel = new JLabel("Password: ");
				JPasswordField passwordField = new JPasswordField(20);
				gbc.gridx = 0;
				gbc.gridy = 1;
				gbc.gridwidth = 1;
				inputs.add(passwordLabel, gbc);
				gbc.gridwidth = 3;
				gbc.gridx = 1;
				inputs.add(passwordField, gbc);

				int result = JOptionPane.showConfirmDialog(this, inputs, "Authentication", JOptionPane.OK_CANCEL_OPTION);

				if (result == JOptionPane.OK_OPTION){
					username = usernameField.getText();
					password = new String(passwordField.getPassword());
				} else {
					return;
				}
			}
			try{
				AlarmMaintainer.setAcknowledged(this.itsName, true, username, password);
				AlarmMaintainer.removeListener(this);
				this.dispose();
			} catch (Exception ex){
				password = "";
				JOptionPane.showMessageDialog(this, "Something went wrong with the sending of data. " +
						"\nPlease ensure that you're properly connected to the network, you are attempting to write to a valid point" +
						"\n and your username and password are correct.", "Data Sending Error", JOptionPane.ERROR_MESSAGE);
				return;
			}
		} else if (command.equals("shelve")){
			if (username.equals("") || password.equals("")){
				JPanel inputs = new JPanel();
				inputs.setLayout(new GridBagLayout());
				GridBagConstraints gbc = new GridBagConstraints();
				gbc.fill = GridBagConstraints.HORIZONTAL;
				gbc.weightx = 0.5;
				gbc.gridx = 0;
				gbc.gridy = 0;
				JLabel usernameLabel = new JLabel("Username: ");
				JTextField usernameField = new JTextField(20);
				usernameField.setText(username);
				inputs.add(usernameLabel, gbc);
				gbc.gridx = 1;
				gbc.gridwidth = 3;
				inputs.add(usernameField, gbc);
				JLabel passwordLabel = new JLabel("Password: ");
				JPasswordField passwordField = new JPasswordField(20);
				gbc.gridx = 0;
				gbc.gridy = 1;
				gbc.gridwidth = 1;
				inputs.add(passwordLabel, gbc);
				gbc.gridwidth = 3;
				gbc.gridx = 1;
				inputs.add(passwordField, gbc);

				int result = JOptionPane.showConfirmDialog(this, inputs, "Authentication", JOptionPane.OK_CANCEL_OPTION);

				if (result == JOptionPane.OK_OPTION){
					username = usernameField.getText();
					password = new String(passwordField.getPassword());

				} else {
					return;
				}
			} 

			try {
				shelved = !shelved;
				AlarmMaintainer.setShelved(this.itsName, shelved, username, password);
				AlarmMaintainer.removeListener(this);
				this.dispose();
			} catch (Exception ex){
				password = "";
				JOptionPane.showMessageDialog(this, "Something went wrong with the sending of data. " +
						"\nPlease ensure that you're properly connected to the network, you are attempting to write to a valid point" +
						"\n and your username and password are correct.", "Data Sending Error", JOptionPane.ERROR_MESSAGE);
				return;
			}
		}
	}


	@Override
	public void onAlarmEvent(AlarmEvent event) {
		if (event.getAlarm().getPointDesc().getFullName().equals(this.itsAlarm.getPointDesc().getFullName())){
			itsAlarm = event.getAlarm();
			itsPointDesc = itsAlarm.getPointDesc();
			details = new AlarmPanel(itsName);
			if (itsAlarm.isShelved()){
				shelved = true;
				shelve.setText("UNSHELVE");
			} else {
				shelved = false;
				shelve.setText("SHELVE");
			}
			detailsScroller.setViewportView(details);

		}
	}

	// Test for a blank popup frame
	public static void main(String[] args){
		AlarmPopupFrame apf = new AlarmPopupFrame();
		apf.setVisible(true);
	}
}
