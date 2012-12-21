package atnf.atoms.mon.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.security.InvalidParameterException;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.border.Border;

import atnf.atoms.mon.AlarmManager;
import atnf.atoms.mon.PointDescription;
import atnf.atoms.mon.AlarmManager.Alarm;
import atnf.atoms.mon.AlarmManager.AlarmEvent;
import atnf.atoms.mon.util.MailSender;
import atnf.atoms.time.AbsTime;
import atnf.atoms.util.AlarmEventListener;

public class AlarmPopupFrame extends JFrame implements ActionListener, ItemListener, AlarmEventListener{

	String itsUser = "";
	String itsName;
	PointDescription itsPointDesc;
	Alarm itsAlarm;
	boolean shlvChanged = false;
	boolean ackChanged = false;
	boolean tempShlv = false;
	boolean tempAck = false;
	/**
	 * 
	 */
	private static final long serialVersionUID = -2222706905265546584L;
	boolean active = false;
	AlarmPanel details = new AlarmPanel();
	JScrollPane detailsScroller = new JScrollPane();
	JButton notify = new JButton("NOTIFY");
	JToggleButton ack = new JToggleButton("ACK");
	JToggleButton shelve = new JToggleButton("SHELVE");
	JButton reset = new JButton("Reset");
	JButton confirm = new JButton("Confirm");
	public AlarmPopupFrame(String s) throws HeadlessException {

		itsName = s;
		itsPointDesc = PointDescription.getPoint(itsName);
		itsAlarm = AlarmManager.getAlarm(itsPointDesc);
		this.setLayout(new BorderLayout());
		this.setMinimumSize(new Dimension(500, 800));
		this.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		this.setTitle("ALARM NOTIFICATION FOR POINT " + s);
		JPanel content = new JPanel(new BorderLayout());
		//detailsScroller.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		detailsScroller.setViewportView(details);
		notify.setToolTipText("Notify someone about these alarms through email.");
		notify.setFont(new Font("Sans Serif", Font.BOLD, 36));
		notify.addActionListener(this);
		ack.setToolTipText("Acknowledge these alarms.");
		ack.setFont(new Font("Sans Serif", Font.BOLD, 36));
		ack.addItemListener(this);
		shelve.setToolTipText("Shelve the selected alarms.");
		shelve.setFont(new Font("Sans Serif", Font.BOLD, 36));
		shelve.addItemListener(this);
		reset.setToolTipText("Reset your selections");
		reset.setFont(new Font("Sans Serif", Font.ITALIC, 28));
		reset.addActionListener(this);
		confirm.setToolTipText("Execute the selected actions on these alarms.");
		confirm.setFont(new Font("Sans Serif", Font.ITALIC, 28));
		confirm.addActionListener(this);
		JPanel optionButtons = new JPanel();
		optionButtons.setLayout(new GridLayout(1,3));
		optionButtons.add(notify);
		optionButtons.add(ack);
		optionButtons.add(shelve);
		JPanel confirmButtons = new JPanel();
		confirmButtons.setLayout(new GridLayout(1,2));
		confirmButtons.add(reset);
		confirmButtons.add(confirm);

		content.add(detailsScroller, BorderLayout.CENTER);
		content.add(optionButtons, BorderLayout.SOUTH);
		this.getContentPane().add(content, BorderLayout.CENTER);
		this.getContentPane().add(confirmButtons, BorderLayout.SOUTH);
		this.pack();
		this.setLocationByPlatform(true);
		this.setVisible(false);
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		Object source = arg0.getSource();
		if (source.equals(notify)){
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
					if (!args[0].contains("@")) throw (new InvalidParameterException());
					//if (!args[0].endsWith("@csiro.au")) throw new InvalidParameterException("Non-CSIRO Email");//Checks for correct email address
					MailSender.sendMail(args[0], args[1], args[2]);
					JOptionPane.showMessageDialog(this, "Email successfully sent!", "Email Notification", JOptionPane.INFORMATION_MESSAGE);
					this.setVisible(false);
				} catch (InvalidParameterException e0){
					JOptionPane.showMessageDialog(this, "Email sending failed!\n" +
							"You need to send this to a valid email address!", "Email Notification", JOptionPane.ERROR_MESSAGE);
				} catch (Exception e1){
					JOptionPane.showMessageDialog(this, "Email sending failed!\n" +
							"You  may want to check your connection settings.", "Email Notification", JOptionPane.ERROR_MESSAGE);
				}
			}

		} else if (source.equals(reset)){
			tempShlv = false;
			tempAck = false;
			ack.setSelected(false);
			shelve.setSelected(false);
		} else if (source.equals(confirm)){
			int res = 0;
			if (ackChanged && ack.isSelected() || shlvChanged && ack.isSelected()){
				res = JOptionPane.showConfirmDialog(this, "This will set this point to Acknowledged. Is this OK?", "Confirmation" , JOptionPane.YES_NO_OPTION);
			} else if (shlvChanged && shelve.isSelected() || ackChanged && shelve.isSelected()){
				res = JOptionPane.showConfirmDialog(this, "This will set this point to Shelved. Is this OK?", "Confirmation", JOptionPane.YES_NO_OPTION);
			} else if (ackChanged && !ack.isSelected() && !shelve.isSelected()){
				res = JOptionPane.showConfirmDialog(this, "This will de-Acknowledge this point. Is this OK?", "Confirmation" , JOptionPane.YES_NO_OPTION);
			} else if (shlvChanged && !shelve.isSelected() && !ack.isSelected()){
				res = JOptionPane.showConfirmDialog(this, "This will un-Shelve this point. Is this OK?", "Confirmation" , JOptionPane.YES_NO_OPTION);
			}
			if (res == JOptionPane.YES_OPTION){
				ackChanged = false;
				shlvChanged = false;
				try {
					AlarmManager.setAcknowledged(itsPointDesc, tempAck, itsUser, AbsTime.factory());
					AlarmManager.setShelved(itsPointDesc, tempShlv, itsUser, AbsTime.factory());
					this.setVisible(false);
				} catch (NullPointerException e){
					// No point available if this is the case
				}
			}
		}

	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		ackChanged = false;
		shlvChanged = false;
		if (e.getSource().equals(ack)){
			ackChanged = true;
			if (e.getStateChange() == ItemEvent.SELECTED){
				tempAck = true;
				tempShlv = false;
				shelve.setSelected(false);
			} else if (e.getStateChange() == ItemEvent.DESELECTED){
				tempAck = false;
			}

		} else if (e.getSource().equals(shelve)){
			shlvChanged = true;
			if (e.getStateChange() == ItemEvent.SELECTED){
				tempShlv = true;
				tempAck = false;
				ack.setSelected(false);
			} else if (e.getStateChange() == ItemEvent.DESELECTED){
				tempShlv = false;
			}
		}
	}

	@Override
	public void onAlarmEvent(AlarmEvent event) {
		// Rather than this, might be better to just create new AlarmPopupFrames whenever
		// and alarm goes off, wherever this is implemented
		if (event.getAlarm().getAlarmStatus() == Alarm.ALARMING){
			details = new AlarmPanel(event.getAlarm().getPointDesc().getFullName());
			detailsScroller.setViewportView(details);
			this.setVisible(true);
			
		}
	}
	
	public static void main(String[] args){
		AlarmPopupFrame apf = new AlarmPopupFrame("null");
		apf.setVisible(true);
	}
}
