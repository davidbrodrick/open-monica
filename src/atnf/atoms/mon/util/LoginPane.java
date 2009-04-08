/**
 * Class: LoginPane
 * Description: A simple class to get user's loginname and password.
 * NOTE: This class is not very secure!
 * @author Le Cuong Nguyen
 **/
package atnf.atoms.mon.util;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class LoginPane extends JDialog implements ActionListener
{
   String itsUsername = "";
   String itsPassword = "";
   boolean itsFirst = true;
   boolean itsKeep = false;
   JTextField itsUserField = new JTextField(15);
   JPasswordField itsPassField = new JPasswordField(15);
   JCheckBox itsKeepBox = new JCheckBox("Save details:", false);
   boolean itsInit = false;
      
   public LoginPane()
   {
      super();
      setTitle("Login");
      setModal(true);
      getContentPane().setLayout(new GridLayout(3, 2));
      getContentPane().add(new JLabel("Username:"));
      getContentPane().add(itsUserField);
      getContentPane().add(new JLabel("Password"));
      getContentPane().add(itsPassField);
      getContentPane().add(itsKeepBox);
      JButton submit = new JButton("done");
      getContentPane().add(submit);
      submit.addActionListener(this);
      pack();
   }
   
   public String[] getLogin()
   {
      if (!itsKeep && !itsFirst) {
        return null;
      }
      if (!itsInit) {
        return null;
      }
      itsFirst = false;
      String[] res = new String[2];
      res[0] = itsUsername;
      res[1] = itsPassword;
      if (!itsKeep) {
         itsUsername = "";
	 itsPassword = "";
      }
      return res;
   }

   public void actionPerformed(ActionEvent e)
   {
      itsUsername = itsUserField.getText();
      itsPassword = new String(itsPassField.getPassword());
      itsKeep = itsKeepBox.isSelected();
      itsInit = true;
      setVisible(false);
   }
}
